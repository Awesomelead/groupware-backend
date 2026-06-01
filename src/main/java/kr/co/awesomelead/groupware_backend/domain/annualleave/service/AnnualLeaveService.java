package kr.co.awesomelead.groupware_backend.domain.annualleave.service;

import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchGroupResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchItemResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AnnualLeaveResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto.FailureDetail;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeaveDispatchHistory;
import kr.co.awesomelead.groupware_backend.domain.annualleave.mapper.AnnualLeaveMapper;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveDispatchHistoryRepository;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnualLeaveService {

    private final AnnualLeaveRepository annualLeaveRepository;
    private final AnnualLeaveDispatchHistoryRepository annualLeaveDispatchHistoryRepository;
    private final AnnualLeaveMapper annualLeaveMapper;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final S3Service s3Service;

    @Transactional
    public ExcelUploadResponseDto uploadAnnualLeaveFile(
            MultipartFile file, String sheetName, Long userId) {
        User currentUser = validateAnnualLeaveEditAuthority(userId);
        String normalizedSheetName = normalizeSheetName(sheetName);

        YearMonth sheetYearMonth = parseYearMonthFromSheetName(normalizedSheetName);
        if (sheetYearMonth != null) {
            LocalDate start = sheetYearMonth.atDay(1);
            LocalDate end = sheetYearMonth.atEndOfMonth();
            if (annualLeaveDispatchHistoryRepository.existsByBaseDateBetween(start, end)) {
                throw new CustomException(ErrorCode.DUPLICATE_ANNUAL_LEAVE_DISPATCH);
            }
        }

        // S3 업로드를 트랜잭션 전에 수행 (네트워크 I/O를 DB 커넥션 점유 시간에서 분리)
        String fileKey = uploadAnnualLeaveSourceFile(file);

        ProcessResult processResult = processAnnualLeaveFile(file, normalizedSheetName);
        validateSheetMonthMatchesBaseDate(normalizedSheetName, processResult.baseDate());

        saveDispatchHistory(
                currentUser,
                file.getOriginalFilename(),
                normalizedSheetName,
                fileKey,
                processResult.baseDate());
        return processResult.response();
    }

    @Transactional
    public ExcelUploadResponseDto updateAnnualLeaveDispatchForAdmin(
            Long userId, Long dispatchId, MultipartFile file, String sheetName) {
        User currentUser = validateAnnualLeaveEditAuthority(userId);
        AnnualLeaveDispatchHistory history =
                annualLeaveDispatchHistoryRepository
                        .findById(dispatchId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.ANNUAL_LEAVE_DISPATCH_HISTORY_NOT_FOUND));
        String normalizedSheetName = normalizeSheetName(sheetName);

        YearMonth sheetYearMonth = parseYearMonthFromSheetName(normalizedSheetName);
        if (sheetYearMonth != null) {
            LocalDate start = sheetYearMonth.atDay(1);
            LocalDate end = sheetYearMonth.atEndOfMonth();
            if (annualLeaveDispatchHistoryRepository.existsByIdNotAndBaseDateBetween(
                    dispatchId, start, end)) {
                throw new CustomException(ErrorCode.DUPLICATE_ANNUAL_LEAVE_DISPATCH);
            }
        }

        // S3 업로드를 DB 작업 전에 수행 (트랜잭션 내 네트워크 I/O 제거)
        String oldFileKey = history.getFileKey();
        String newFileKey = uploadAnnualLeaveSourceFile(file);

        ProcessResult processResult = processAnnualLeaveFile(file, normalizedSheetName);
        validateSheetMonthMatchesBaseDate(normalizedSheetName, processResult.baseDate());

        history.setUploadedBy(currentUser);
        history.setOriginalFileName(normalizeOriginalFileName(file.getOriginalFilename()));
        history.setSheetName(normalizeSheetName(sheetName));
        history.setFileKey(newFileKey);
        history.setBaseDate(processResult.baseDate());

        if (oldFileKey != null && !oldFileKey.isBlank() && !oldFileKey.equals(newFileKey)) {
            try {
                s3Service.deleteFile(oldFileKey);
            } catch (RuntimeException e) {
                log.warn(
                        "연차 발송 기존 파일 삭제 실패 - dispatchId: {}, fileKey: {}",
                        dispatchId,
                        oldFileKey,
                        e);
            }
        }

        return processResult.response();
    }

    @Transactional
    public void deleteAnnualLeaveDispatchForAdmin(Long userId, Long dispatchId) {
        validateAnnualLeaveEditAuthority(userId);

        AnnualLeaveDispatchHistory history =
                annualLeaveDispatchHistoryRepository
                        .findById(dispatchId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.ANNUAL_LEAVE_DISPATCH_HISTORY_NOT_FOUND));

        String fileKey = history.getFileKey();
        annualLeaveDispatchHistoryRepository.delete(history);
        rebuildAnnualLeavesFromDispatchHistories(dispatchId);

        if (fileKey != null && !fileKey.isBlank()) {
            try {
                s3Service.deleteFile(fileKey);
            } catch (RuntimeException e) {
                log.warn("연차 발송 파일 삭제 실패 - dispatchId: {}, fileKey: {}", dispatchId, fileKey, e);
            }
        }
    }

    @Transactional
    public void rebuildAnnualLeavesForAdmin(Long userId) {
        validateAnnualLeaveEditAuthority(userId);
        rebuildAnnualLeavesFromDispatchHistories(null);
    }

    private record ProcessResult(ExcelUploadResponseDto response, LocalDate baseDate) {}

    private ProcessResult processAnnualLeaveFile(MultipartFile file, String normalizedSheetName) {
        try (InputStream is = file.getInputStream()) {
            return processAnnualLeaveFile(is, normalizedSheetName, true);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    private ProcessResult processAnnualLeaveFile(
            InputStream is, String normalizedSheetName, boolean sendNotification) {
        List<FailureDetail> failures = new ArrayList<>();
        int successCount = 0;
        int totalProcessed = 0;
        LocalDate baseUpdateDate = null;

        try (Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheet(normalizedSheetName);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            // 기준일 파싱 (5행 J열(Index 9)에서 기준일 추출)
            baseUpdateDate = parseBaseDate(sheet);

            // 데이터 파싱 (8행(Index 7)부터 시작)
            for (int i = 7; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row)) { // 성명이 비었는지 확인 후
                    break; // 파싱 종료
                }

                totalProcessed++; // 파싱 작업 수 증가
                try {
                    processAnnualLeaveRow(row, baseUpdateDate, sendNotification);
                    successCount++; // 성공 작업 수 증가
                } catch (Exception e) {
                    log.warn("엑셀 업로드 실패 - 행 {}: {}", i + 1, e.getMessage());
                    failures.add(
                            new ExcelUploadResponseDto.FailureDetail(
                                    i + 1, // 엑셀 상의 열
                                    getCellValueAsString(row.getCell(3)), // 성명
                                    e.getMessage() // 실패 원인 (에러 메세지)
                                    ));
                }
            }

        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        ExcelUploadResponseDto response =
                ExcelUploadResponseDto.builder()
                        .totalCount(totalProcessed)
                        .successCount(successCount)
                        .failureCount(failures.size())
                        .failures(failures)
                        .build();
        return new ProcessResult(response, baseUpdateDate);
    }

    private void processAnnualLeaveRow(Row row, LocalDate updateDate, boolean sendNotification) {
        // 엑셀 컬럼 정의 기반 데이터 추출
        String name = getCellValueAsString(row.getCell(3)).trim();
        LocalDate joinDate = getCellValueAsLocalDate(row.getCell(4));

        if (name.isEmpty() || joinDate == null) {
            throw new RuntimeException("성명 또는 입사일 정보가 누락되었습니다.");
        }

        // 유저 식별 (성명 + 입사일)
        User targetUser =
                userRepository
                        .findByNameAndJoinDate(name, joinDate)
                        .orElseThrow(() -> new RuntimeException("일치하는 직원을 찾을 수 없습니다."));

        // 연차 정보 추출 (Double 타입 대응)
        Double total = getCellValueAsDouble(row.getCell(5));
        Double monthly = getCellValueAsDouble(row.getCell(6));
        Double carried = getCellValueAsDouble(row.getCell(7));
        Double used = getCellValueAsDouble(row.getCell(8));
        Double remain = getCellValueAsDouble(row.getCell(9));

        // 기존 정보가 있으면 업데이트, 없으면 신규 생성
        AnnualLeave annualLeave =
                annualLeaveRepository.findByUser(targetUser).orElse(new AnnualLeave());

        // 기존 연차의 updateDate YearMonth가 새 updateDate YearMonth보다 최신이면 스킵
        if (annualLeave.getUpdateDate() != null) {
            YearMonth existingYM = YearMonth.from(annualLeave.getUpdateDate());
            YearMonth newYM = YearMonth.from(updateDate);
            if (newYM.isBefore(existingYM)) {
                return;
            }
        }

        annualLeave.setUser(targetUser);
        annualLeave.setTotal(total);
        annualLeave.setMonthlyLeave(monthly);
        annualLeave.setCarriedOver(carried);
        annualLeave.setUsed(used);
        annualLeave.setRemain(remain);
        annualLeave.setUpdateDate(updateDate); // Date 타입 호환

        annualLeaveRepository.save(annualLeave);

        if (sendNotification) {
            // 연차 알림 전송 (LocalDate를 "yyyy년 MM월 dd일" 형식으로 변환하여 템플릿과 매칭)
            String formattedDate =
                    updateDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
            notificationService.sendAnnualLeaveAlertToUser(targetUser.getId(), formattedDate);
        }
    }

    // 연차 데이터 공지 기준일 파싱
    private LocalDate parseBaseDate(Sheet sheet) {
        try {
            // 5행(Index 4)의 J열(Index 9) 또는 K열 확인
            String cellValue = getCellValueAsString(sheet.getRow(4).getCell(9));
            // 예시 "기준일 : 2025-08-01" 에서 날짜 부분만 추출
            String dateStr = cellValue.replaceAll("[^0-9-]", "");
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            log.error("기준일 파싱 실패, 오늘 날짜로 대체합니다.");
            throw new CustomException(ErrorCode.INVALID_BASE_DATE_FORMAT);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> "";
        };
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return 0.0;
        }
        return cell.getNumericCellValue();
    }

    private LocalDate getCellValueAsLocalDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        // 문자열로 적혀있을 경우 대응
        return LocalDate.parse(
                cell.getStringCellValue(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    // 성명이 비어있는 데이터를 기준으로 파싱을 멈추기 위함
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        Cell cell = row.getCell(3); // 성명이 비었는지 확인
        return cell == null || cell.getCellType() == CellType.BLANK;
    }

    public AnnualLeaveResponseDto getAnnualLeave(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return annualLeaveMapper.toAnnualLeaveResponseDto(user.getAnnualLeave());
    }

    public List<AdminAnnualLeaveDispatchGroupResponseDto> getAnnualLeavesForAdmin(Long userId) {
        validateAnnualLeaveEditAuthority(userId);

        List<AnnualLeaveDispatchHistory> histories =
                annualLeaveDispatchHistoryRepository.findAllOrderByCreatedAtDesc();
        Map<String, List<AnnualLeaveDispatchHistory>> groupedByYear =
                histories.stream()
                        .collect(
                                Collectors.groupingBy(
                                        this::resolveDispatchYearTitle,
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        return groupedByYear.entrySet().stream()
                .map(
                        entry -> {
                            String yearTitle = entry.getKey();
                            List<AdminAnnualLeaveDispatchItemResponseDto> items =
                                    entry.getValue().stream().map(this::toDispatchItemDto).toList();

                            return AdminAnnualLeaveDispatchGroupResponseDto.builder()
                                    .sheetName(yearTitle)
                                    .title(yearTitle)
                                    .totalCount(items.size())
                                    .items(items)
                                    .build();
                        })
                .toList();
    }

    private AdminAnnualLeaveDispatchItemResponseDto toDispatchItemDto(
            AnnualLeaveDispatchHistory history) {
        return AdminAnnualLeaveDispatchItemResponseDto.builder()
                .dispatchId(history.getId())
                .title(resolveDispatchMonthTitle(history))
                .originalFileName(history.getOriginalFileName())
                .sheetName(normalizeSheetName(history.getSheetName()))
                .fileUrl(s3Service.getPresignedViewUrl(history.getFileKey()))
                .build();
    }

    private String resolveDispatchYearTitle(AnnualLeaveDispatchHistory history) {
        DispatchPeriod period = resolveDispatchPeriod(history);
        if (period.year() == null) {
            return "연도 미상";
        }
        return period.year() + "년";
    }

    private String resolveDispatchMonthTitle(AnnualLeaveDispatchHistory history) {
        DispatchPeriod period = resolveDispatchPeriod(history);
        if (period.month() == null) {
            return normalizeSheetName(history.getSheetName());
        }
        return period.month() + "월";
    }

    private DispatchPeriod resolveDispatchPeriod(AnnualLeaveDispatchHistory history) {
        LocalDate baseDate = history.getBaseDate();
        if (baseDate != null) {
            return new DispatchPeriod(baseDate.getYear(), baseDate.getMonthValue());
        }

        YearMonth sheetYearMonth =
                parseStrictYearMonthFromSheetName(normalizeSheetName(history.getSheetName()));
        if (sheetYearMonth != null) {
            return new DispatchPeriod(sheetYearMonth.getYear(), sheetYearMonth.getMonthValue());
        }

        Integer month = parseMonthFromSheetName(normalizeSheetName(history.getSheetName()));
        return new DispatchPeriod(null, month);
    }

    private record DispatchPeriod(Integer year, Integer month) {}

    private void rebuildAnnualLeavesFromDispatchHistories(Long excludedDispatchId) {
        annualLeaveRepository.deleteAllInBatch();

        List<AnnualLeaveDispatchHistory> remainingHistories =
                annualLeaveDispatchHistoryRepository.findAll().stream()
                        .filter(
                                history ->
                                        excludedDispatchId == null
                                                || !Objects.equals(
                                                        history.getId(), excludedDispatchId))
                        .sorted(dispatchHistoryReplayOrder())
                        .toList();

        for (AnnualLeaveDispatchHistory history : remainingHistories) {
            if (history.getFileKey() == null || history.getFileKey().isBlank()) {
                log.warn("연차 재계산 스킵 - dispatchId: {}, fileKey 없음", history.getId());
                continue;
            }

            byte[] fileBytes = s3Service.downloadFile(history.getFileKey());
            processAnnualLeaveFile(
                    new ByteArrayInputStream(fileBytes),
                    normalizeSheetName(history.getSheetName()),
                    false);
        }
    }

    private Comparator<AnnualLeaveDispatchHistory> dispatchHistoryReplayOrder() {
        return Comparator.comparing(
                        AnnualLeaveDispatchHistory::getBaseDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        AnnualLeaveDispatchHistory::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        AnnualLeaveDispatchHistory::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    public AdminAnnualLeaveDispatchDetailResponseDto getAnnualLeaveDispatchDetailForAdmin(
            Long userId, Long dispatchId) {
        validateAnnualLeaveEditAuthority(userId);

        AnnualLeaveDispatchHistory history =
                annualLeaveDispatchHistoryRepository
                        .findById(dispatchId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.ANNUAL_LEAVE_DISPATCH_HISTORY_NOT_FOUND));

        return AdminAnnualLeaveDispatchDetailResponseDto.builder()
                .dispatchId(history.getId())
                .originalFileName(history.getOriginalFileName())
                .sheetName(normalizeSheetName(history.getSheetName()))
                .createdAt(history.getCreatedAt())
                .fileUrl(s3Service.getPresignedViewUrl(history.getFileKey()))
                .build();
    }

    private User validateAnnualLeaveEditAuthority(Long userId) {
        User currentUser =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!currentUser.hasAuthority(Authority.EDIT_EMPLOYEE_INFO)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_ANNUAL_LEAVE);
        }
        return currentUser;
    }

    private void saveDispatchHistory(
            User uploader,
            String originalFileName,
            String sheetName,
            String fileKey,
            LocalDate baseDate) {
        AnnualLeaveDispatchHistory history =
                AnnualLeaveDispatchHistory.builder()
                        .uploadedBy(uploader)
                        .originalFileName(normalizeOriginalFileName(originalFileName))
                        .sheetName(normalizeSheetName(sheetName))
                        .fileKey(fileKey)
                        .baseDate(baseDate)
                        .build();
        annualLeaveDispatchHistoryRepository.save(history);
    }

    private YearMonth parseYearMonthFromSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            return null;
        }
        // "2026-06" 형식
        YearMonth strictYearMonth = parseStrictYearMonthFromSheetName(sheetName);
        if (strictYearMonth != null) {
            return strictYearMonth;
        }
        // "8월", "08월", "8" 형식 — 연도는 현재 연도로 추정
        try {
            Integer month = parseMonthFromSheetName(sheetName);
            if (month != null) {
                return YearMonth.of(java.time.LocalDate.now().getYear(), month);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private YearMonth parseStrictYearMonthFromSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(sheetName, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseMonthFromSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            return null;
        }
        String numericStr = sheetName.replaceAll("[^0-9]", "");
        if (numericStr.isEmpty()) {
            return null;
        }
        try {
            int month = Integer.parseInt(numericStr);
            if (month >= 1 && month <= 12) {
                return month;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private void validateSheetMonthMatchesBaseDate(String sheetName, LocalDate baseDate) {
        try {
            String numericStr = sheetName.replaceAll("[^0-9]", "");
            if (!numericStr.isEmpty()) {
                int sheetMonth = Integer.parseInt(numericStr);
                if (sheetMonth >= 1 && sheetMonth <= 12 && sheetMonth != baseDate.getMonthValue()) {
                    throw new CustomException(ErrorCode.ANNUAL_LEAVE_MONTH_MISMATCH);
                }
            }
        } catch (CustomException e) {
            throw e;
        } catch (NumberFormatException e) {
            // 숫자 파싱 불가 시트명은 검증 통과
        }
    }

    private String uploadAnnualLeaveSourceFile(MultipartFile file) {
        try {
            return s3Service.uploadFile(file);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    private String normalizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "unknown.xlsx";
        }
        return originalFileName.trim();
    }

    private String normalizeSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            return "미분류 시트";
        }
        return Objects.requireNonNull(sheetName).trim();
    }
}
