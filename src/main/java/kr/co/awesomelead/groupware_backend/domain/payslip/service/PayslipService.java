package kr.co.awesomelead.groupware_backend.domain.payslip.service;

import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipGroupDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import kr.co.awesomelead.groupware_backend.domain.payslip.mapper.PayslipMapper;
import kr.co.awesomelead.groupware_backend.domain.payslip.repository.PayslipRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayslipService {

    private static final Pattern PAYSLIP_FILE_NAME_PATTERN =
            Pattern.compile(
                    "^급여명세서\\(근로기준1\\)_([^_]+)_([^_]+)_([0-9]{6})\\.pdf$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern PAYSLIP_MONTH_PATTERN =
            Pattern.compile(
                    "^급여명세서\\(근로기준1\\)_[^_]+_[^_]+_([0-9]{6})\\.pdf$",
                    Pattern.CASE_INSENSITIVE);
    private static final String UNKNOWN_YEAR_MONTH = "UNKNOWN";

    private final PayslipRepository payslipRepository;
    private final PayslipMapper payslipMapper;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;
    private final PayslipPdfInfoExtractor payslipPdfInfoExtractor;
    private final PayslipOcrInfoExtractor payslipOcrInfoExtractor;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public void sendPayslip(List<MultipartFile> payslipFiles, Long userId) throws IOException {
        validatePayslipEditAuthority(userId);

        for (MultipartFile file : payslipFiles) {

            String originalFileName = file.getOriginalFilename();

            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                throw new CustomException(ErrorCode.ONLY_PDF_ALLOWED);
            }

            String fileNameEmployeeName = parseEmployeeNameFromFileName(originalFileName);
            PayslipPdfInfoExtractor.PayslipPersonalInfo pdfPersonalInfo =
                    extractPersonalInfoWithFallback(file, originalFileName);

            if (!isSamePersonName(fileNameEmployeeName, pdfPersonalInfo.name())) {
                throw new CustomException(ErrorCode.PAYSLIP_USER_INFO_MISMATCH);
            }

            User target =
                    findTargetUserByNameAndBirthDate(
                            pdfPersonalInfo.name(),
                            fileNameEmployeeName,
                            pdfPersonalInfo.birthDate());

            String s3Key = s3Service.uploadFile(file);

            Payslip savedPayslip = savePayslipInfo(s3Key, originalFileName, target);
            notificationService.sendPayslipAlertToUser(target.getId(), savedPayslip.getId());
        }
    }

    @Transactional
    public void updatePayslip(Long payslipId, MultipartFile payslipFile, Long userId)
            throws IOException {
        validatePayslipEditAuthority(userId);

        Payslip payslip =
                payslipRepository
                        .findById(payslipId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        String originalFileName = payslipFile.getOriginalFilename();
        if (!Objects.equals(payslipFile.getContentType(), "application/pdf")) {
            throw new CustomException(ErrorCode.ONLY_PDF_ALLOWED);
        }

        String fileNameEmployeeName = parseEmployeeNameFromFileName(originalFileName);
        PayslipPdfInfoExtractor.PayslipPersonalInfo pdfPersonalInfo =
                extractPersonalInfoWithFallback(payslipFile, originalFileName);

        if (!isSamePersonName(fileNameEmployeeName, pdfPersonalInfo.name())) {
            throw new CustomException(ErrorCode.PAYSLIP_USER_INFO_MISMATCH);
        }

        User target =
                findTargetUserByNameAndBirthDate(
                        pdfPersonalInfo.name(), fileNameEmployeeName, pdfPersonalInfo.birthDate());

        String oldFileKey = payslip.getFileKey();
        String newFileKey = s3Service.uploadFile(payslipFile);

        payslip.setFileKey(newFileKey);
        payslip.setOriginalFileName(originalFileName);
        payslip.setUser(target);
        payslip.setStatus(PayslipStatus.SENT);
        payslip.setReadAt(null);

        notificationService.sendPayslipAlertToUser(target.getId(), payslip.getId());

        if (oldFileKey != null && !oldFileKey.isBlank() && !oldFileKey.equals(newFileKey)) {
            try {
                s3Service.deleteFile(oldFileKey);
            } catch (RuntimeException e) {
                log.warn("급여명세서 기존 파일 삭제 실패 - payslipId: {}, fileKey: {}", payslipId, oldFileKey, e);
            }
        }
    }

    @Transactional
    public void deletePayslip(Long payslipId, Long userId) {
        validatePayslipEditAuthority(userId);

        Payslip payslip =
                payslipRepository
                        .findById(payslipId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        String fileKey = payslip.getFileKey();
        payslipRepository.delete(payslip);

        if (fileKey != null && !fileKey.isBlank()) {
            try {
                s3Service.deleteFile(fileKey);
            } catch (RuntimeException e) {
                log.warn("급여명세서 파일 삭제 실패 - payslipId: {}, fileKey: {}", payslipId, fileKey, e);
            }
        }
    }

    private PayslipPdfInfoExtractor.PayslipPersonalInfo extractPersonalInfoWithFallback(
            MultipartFile file, String originalFileName) {
        PayslipPdfInfoExtractor.PayslipPersonalInfo primaryInfo = null;
        boolean needsOcrFallback = false;

        try {
            primaryInfo = payslipPdfInfoExtractor.extract(file);
            needsOcrFallback = payslipPdfInfoExtractor.isNameLikelyCorrupted(primaryInfo.name());
        } catch (CustomException e) {
            if (e.getErrorCode() != ErrorCode.INVALID_PAYSLIP_PDF_CONTENT) {
                throw e;
            }
            needsOcrFallback = true;
        }

        if (!needsOcrFallback || !payslipOcrInfoExtractor.isOcrEnabled()) {
            if (primaryInfo == null) {
                throw new CustomException(ErrorCode.INVALID_PAYSLIP_PDF_CONTENT);
            }
            return primaryInfo;
        }

        try {
            PayslipPdfInfoExtractor.PayslipPersonalInfo ocrInfo = payslipOcrInfoExtractor.extract(file);
            if (!payslipPdfInfoExtractor.isNameLikelyCorrupted(ocrInfo.name())) {
                log.info("급여명세서 OCR fallback 적용 성공 - fileName: {}", originalFileName);
                return ocrInfo;
            }
            return primaryInfo != null ? primaryInfo : ocrInfo;
        } catch (CustomException e) {
            if (primaryInfo != null) {
                return primaryInfo;
            }
            throw e;
        }
    }

    private String parseEmployeeNameFromFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
        }

        String baseName = extractBaseFileName(originalFileName);
        Matcher matcher = PAYSLIP_FILE_NAME_PATTERN.matcher(baseName);
        if (!matcher.matches()) {
            throw new CustomException(ErrorCode.INVALID_PAYSLIP_FILE_NAME_FORMAT);
        }

        String employeeName = matcher.group(2);
        return normalizeNameForLookup(employeeName);
    }

    private User findTargetUserByNameAndBirthDate(
            String pdfName, String fileNameName, LocalDate birthDate) {
        List<String> lookupCandidates = new ArrayList<>();
        String normalizedPdfName = normalizeNameForLookup(pdfName);
        String normalizedFileName = normalizeNameForLookup(fileNameName);
        lookupCandidates.add(normalizedPdfName);
        if (!normalizedPdfName.equals(normalizedFileName)) {
            lookupCandidates.add(normalizedFileName);
        }

        User matchedUser = null;
        for (String nameCandidate : lookupCandidates) {
            List<User> foundUsers =
                    userRepository.findAllByNameAndBirthDate(nameCandidate, birthDate);

            if (foundUsers.size() > 1) {
                throw new CustomException(ErrorCode.AMBIGUOUS_PAYSLIP_RECIPIENT);
            }
            if (foundUsers.size() == 1) {
                User foundUser = foundUsers.get(0);
                if (matchedUser == null) {
                    matchedUser = foundUser;
                } else if (!matchedUser.getId().equals(foundUser.getId())) {
                    throw new CustomException(ErrorCode.AMBIGUOUS_PAYSLIP_RECIPIENT);
                }
            }
        }

        if (matchedUser != null) {
            return matchedUser;
        }
        throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }

    private boolean isSamePersonName(String fileNameName, String pdfName) {
        String normalizedFileNameName = normalizeNameForComparison(fileNameName);
        String normalizedPdfName = normalizeNameForComparison(pdfName);
        return normalizedFileNameName.equals(normalizedPdfName);
    }

    private String normalizeNameForComparison(String name) {
        return normalizeNameForLookup(name).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeNameForLookup(String name) {
        if (name == null) {
            return "";
        }
        return Normalizer.normalize(name, Normalizer.Form.NFC).replaceAll("\\s+", " ").trim();
    }

    private String extractBaseFileName(String fileName) {
        String normalizedPath = fileName.replace("\\", "/");
        String fileOnly = Paths.get(normalizedPath).getFileName().toString();
        return Normalizer.normalize(fileOnly, Normalizer.Form.NFC);
    }

    @Transactional
    protected Payslip savePayslipInfo(String s3Key, String originalFileName, User targetUser) {
        Payslip payslip =
                Payslip.builder()
                        .fileKey(s3Key)
                        .originalFileName(originalFileName)
                        .status(PayslipStatus.SENT)
                        .user(targetUser)
                        .build();
        return payslipRepository.save(payslip);
    }

    // 관리자용 보낸 급여명세서 목록 조회 (Status에 따라)
    @Transactional(readOnly = true)
    public List<AdminPayslipSummaryDto> getPayslipsForAdmin(Long adminId, PayslipStatus status) {
        validateAdminAuthority(adminId);

        List<Payslip> payslipList = payslipRepository.findAllByStatusOptionalWithUser(status);

        return payslipMapper.toAdminPayslipSummaryDtoList(payslipList);
    }

    @Transactional(readOnly = true)
    public List<AdminPayslipGroupDto> getPayslipsForAdminGrouped(Long adminId, PayslipStatus status) {
        List<AdminPayslipSummaryDto> summaries = getPayslipsForAdmin(adminId, status);

        Map<String, List<AdminPayslipSummaryDto>> groupedByYearMonth =
                summaries.stream()
                        .collect(
                                Collectors.groupingBy(
                                        summary -> extractYearMonth(summary.getOriginalFileName())));

        return groupedByYearMonth.entrySet().stream()
                .sorted((left, right) -> compareYearMonthDesc(left.getKey(), right.getKey()))
                .map(
                        entry -> {
                            String yearMonth = entry.getKey();
                            List<AdminPayslipSummaryDto> items =
                                    entry.getValue().stream()
                                            .sorted(
                                                    Comparator.comparing(
                                                                    AdminPayslipSummaryDto::getCreatedAt,
                                                                    Comparator.nullsLast(
                                                                            Comparator.naturalOrder()))
                                                            .reversed())
                                            .toList();

                            return AdminPayslipGroupDto.builder()
                                    .yearMonth(UNKNOWN_YEAR_MONTH.equals(yearMonth) ? null : yearMonth)
                                    .title(buildGroupTitle(yearMonth))
                                    .totalCount(items.size())
                                    .items(items)
                                    .build();
                        })
                .toList();
    }

    // 관리자용 보낸 급여명세서 상세 조회
    @Transactional(readOnly = true)
    public AdminPayslipDetailDto getPayslipForAdmin(Long adminId, Long payslipId) {
        validateAdminAuthority(adminId);

        Payslip payslip =
                payslipRepository
                        .findById(payslipId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        return payslipMapper.toAdminPayslipDetailDto(payslip, s3Service);
    }

    // 직원의 급여명세서 목록 조회 (Status에 따라)
    @Transactional(readOnly = true)
    public List<EmployeePayslipSummaryDto> getPayslips(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        List<Payslip> payslipList = payslipRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        return payslipMapper.toEmployeePayslipSummaryDtoList(payslipList);
    }

    // 직원의 급여명세서 상세 조회
    @Transactional
    public EmployeePayslipDetailDto getPayslip(Long userId, Long payslipId, String password) {

        Payslip payslip =
                payslipRepository
                        .findById(payslipId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PAYSLIP_NOT_FOUND));

        if (!payslip.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_VIEW_PAYSLIP);
        }

        if (!bCryptPasswordEncoder.matches(password, payslip.getUser().getPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 통보형 프로세스: 직원이 상세 조회하면 열람 완료 처리
        if (payslip.getStatus() == PayslipStatus.SENT) {
            payslip.setStatus(PayslipStatus.READ);
            payslip.setReadAt(LocalDateTime.now());
        }
        return payslipMapper.toEmployeePayslipDetailDto(payslip, s3Service);
    }

    private void validateAdminAuthority(Long adminId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }
    }

    private void validatePayslipEditAuthority(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!user.hasAuthority(Authority.EDIT_EMPLOYEE_INFO)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_PAYSLIP);
        }
    }

    private String extractYearMonth(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return UNKNOWN_YEAR_MONTH;
        }

        String normalizedPath = originalFileName.replace("\\", "/");
        String baseFileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
        baseFileName = Normalizer.normalize(baseFileName, Normalizer.Form.NFC);

        Matcher matcher = PAYSLIP_MONTH_PATTERN.matcher(baseFileName);
        if (!matcher.matches()) {
            return UNKNOWN_YEAR_MONTH;
        }
        return matcher.group(1);
    }

    private String buildGroupTitle(String yearMonth) {
        if (UNKNOWN_YEAR_MONTH.equals(yearMonth) || yearMonth == null || yearMonth.length() != 6) {
            return "미분류 급여명세서";
        }

        int year = Integer.parseInt(yearMonth.substring(0, 4));
        int month = Integer.parseInt(yearMonth.substring(4, 6));
        if (month < 1 || month > 12) {
            return "미분류 급여명세서";
        }

        return year + "년 " + month + "월 급여명세서";
    }

    private int compareYearMonthDesc(String first, String second) {
        if (UNKNOWN_YEAR_MONTH.equals(first) && UNKNOWN_YEAR_MONTH.equals(second)) {
            return 0;
        }
        if (UNKNOWN_YEAR_MONTH.equals(first)) {
            return 1;
        }
        if (UNKNOWN_YEAR_MONTH.equals(second)) {
            return -1;
        }
        return second.compareTo(first);
    }
}
