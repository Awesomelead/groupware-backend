package kr.co.awesomelead.groupware_backend.domain.annualleave.service;

import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AnnualLeaveResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto.FailureDetail;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.annualleave.mapper.AnnualLeaveMapper;
import kr.co.awesomelead.groupware_backend.domain.annualleave.repository.AnnualLeaveRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

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

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnualLeaveService {

    private final AnnualLeaveRepository annualLeaveRepository;
    private final AnnualLeaveMapper annualLeaveMapper;
    private final UserRepository userRepository;

    @Transactional
    public ExcelUploadResponseDto uploadAnnualLeaveFile(
            MultipartFile file, String sheetName, Long userId) {
        // 유저의 연차발송 권한 확인
        User currentUser =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!currentUser.hasAuthority(Authority.UPLOAD_ANNUAL_LEAVE)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_ANNUAL_LEAVE);
        }

        List<FailureDetail> failures = new ArrayList<>();
        int successCount = 0;
        int totalProcessed = 0;

        try (InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheet(sheetName); // 하나의 엑셀파일에서 월별로 시트를 구분하는 것으로 확인

            // 기준일 파싱 (5행 J열(Index 9)에서 기준일 추출)
            LocalDate baseUpdateDate = parseBaseDate(sheet);

            // 데이터 파싱 (8행(Index 7)부터 시작)
            for (int i = 7; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isRowEmpty(row)) { // 성명이 비었는지 확인 후
                    break; // 파싱 종료
                }

                totalProcessed++; // 파싱 작업 수 증가
                try {
                    processAnnualLeaveRow(row, baseUpdateDate);
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

        return ExcelUploadResponseDto.builder()
                .totalCount(totalProcessed)
                .successCount(successCount)
                .failureCount(failures.size())
                .failures(failures)
                .build();
    }

    private void processAnnualLeaveRow(Row row, LocalDate updateDate) {
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

        annualLeave.setUser(targetUser);
        annualLeave.setTotal(total);
        annualLeave.setMonthlyLeave(monthly);
        annualLeave.setCarriedOver(carried);
        annualLeave.setUsed(used);
        annualLeave.setRemain(remain);
        annualLeave.setUpdateDate(updateDate); // Date 타입 호환

        annualLeaveRepository.save(annualLeave);
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

    @Transactional(readOnly = true)
    public AnnualLeaveResponseDto getAnnualLeave(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return annualLeaveMapper.toAnnualLeaveResponseDto(user.getAnnualLeave());
    }
}
