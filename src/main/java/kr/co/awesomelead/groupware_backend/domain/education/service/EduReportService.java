package kr.co.awesomelead.groupware_backend.domain.education.service;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.DepartmentEduReportCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportStatusUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.PsmEduReportCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.SafetyEduReportCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttachment;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EducationCategory;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduReportStatus;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;
import kr.co.awesomelead.groupware_backend.domain.education.mapper.EduMapper;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttendanceRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduReportQueryRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduReportRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EducationCategoryRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EduReportService {

    private final EduReportRepository eduReportRepository;
    private final EduReportQueryRepository eduReportQueryRepository;
    private final EduAttendanceRepository eduAttendanceRepository;
    private final EduAttachmentRepository eduAttachmentRepository;
    private final EducationCategoryRepository educationCategoryRepository;
    private final EduMapper eduMapper;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;

    @Transactional
    public Long createEduReport(EduReportRequestDto requestDto, List<MultipartFile> files, Long id)
            throws IOException {
        return createEduReportInternal(requestDto, files, id, false, null);
    }

    @Transactional
    public Long createPsmEduReport(
            PsmEduReportCreateRequestDto requestDto, List<MultipartFile> files, Long id)
            throws IOException {
        EduReportRequestDto baseRequest =
                EduReportRequestDto.builder()
                        .eduType(EduType.PSM)
                        .title(requestDto.getTitle())
                        .content(requestDto.getContent())
                        .pinned(requestDto.isPinned())
                        .signatureRequired(false)
                        .categoryId(requestDto.getCategoryId())
                        .build();
        return createEduReportInternal(baseRequest, files, id, true, requestDto.getCompanyScope());
    }

    @Transactional
    public Long createSafetyEduReport(
            SafetyEduReportCreateRequestDto requestDto, List<MultipartFile> files, Long id)
            throws IOException {
        EduReportRequestDto baseRequest =
                EduReportRequestDto.builder()
                        .eduType(EduType.SAFETY)
                        .title(requestDto.getTitle())
                        .content(requestDto.getContent())
                        .pinned(requestDto.isPinned())
                        .signatureRequired(false)
                        .categoryId(requestDto.getCategoryId())
                        .build();
        return createEduReportInternal(baseRequest, files, id, true, requestDto.getCompanyScope());
    }

    private Long createEduReportInternal(
            EduReportRequestDto requestDto,
            List<MultipartFile> files,
            Long id,
            boolean useCompanyOverride,
            Company companyOverride)
            throws IOException {

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validateCreateAuthority(user, requestDto.getEduType());

        Department department = null;
        if (requestDto.getEduType() == EduType.DEPARTMENT) {
            if (requestDto.getDepartmentId() == null) {
                throw new CustomException(ErrorCode.DEPARTMENT_ID_REQUIRED);
            }
            department =
                    departmentRepository
                            .findById(requestDto.getDepartmentId())
                            .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

        EducationCategory category = null;
        if (requestDto.getEduType() == EduType.PSM || requestDto.getEduType() == EduType.SAFETY) {
            if (requestDto.getCategoryId() == null) {
                throw new CustomException(ErrorCode.EDUCATION_CATEGORY_REQUIRED);
            }
            category = getValidatedCategory(requestDto.getEduType(), requestDto.getCategoryId());
        }

        EduReport report = eduMapper.toEduReportEntity(requestDto, department, category);
        if (requestDto.getEduType() == EduType.PSM || requestDto.getEduType() == EduType.SAFETY) {
            report.setCompany(useCompanyOverride ? companyOverride : user.getWorkLocation());
        } else {
            report.setCompany(null);
        }

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!isRealAttachmentFile(file)) {
                    continue;
                }

                // S3 업로드 후 고유 키 반환
                String s3Key = s3Service.uploadFile(file);

                // 첨부파일 엔티티 생성
                EduAttachment attachment = new EduAttachment();
                attachment.setOriginalFileName(file.getOriginalFilename());
                attachment.setS3Key(s3Key);
                attachment.setFileSize(file.getSize());

                // 연관관계 편의 메서드 활용
                report.addAttachment(attachment);
            }
        }

        EduReport savedReport = eduReportRepository.save(report);

        // 알림 발송 대상 조회 및 전송
        List<Long> targetUserIds;
        if (requestDto.getEduType() == EduType.PSM || requestDto.getEduType() == EduType.SAFETY) {
            if (report.getCompany() == null) {
                targetUserIds = userRepository.findAllActiveUserIds();
            } else {
                targetUserIds = userRepository.findAllIdsByCompany(report.getCompany());
            }
        } else {
            targetUserIds = userRepository.findAllIdsByDepartmentId(requestDto.getDepartmentId());
        }
        Map<String, Object> metadata =
                requestDto.getEduType() == EduType.SAFETY
                        ? Map.of(
                                "educationType",
                                requestDto.getEduType().name(),
                                "detailType",
                                "GENERAL")
                        : Map.of("educationType", requestDto.getEduType().name());
        notificationService.sendEduReportAlertToTargets(
                requestDto.getEduType().getDescription(),
                requestDto.getTitle(),
                savedReport.getId(),
                targetUserIds,
                metadata);

        return savedReport.getId();
    }

    @Transactional
    public Long createDepartmentEduReport(
            DepartmentEduReportCreateRequestDto requestDto, List<MultipartFile> files, Long id)
            throws IOException {
        EduReportRequestDto baseRequest =
                EduReportRequestDto.builder()
                        .eduType(EduType.DEPARTMENT)
                        .title(requestDto.getTitle())
                        .content(requestDto.getContent())
                        .pinned(requestDto.isPinned())
                        .signatureRequired(requestDto.isSignatureRequired())
                        .departmentId(requestDto.getDepartmentId())
                        .build();
        return createEduReport(baseRequest, files, id);
    }

    private void validateCreateAuthority(User user, EduType eduType) {
        if (eduType == EduType.DEPARTMENT) {
            if (!user.hasAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION)) {
                throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_EDU_REPORT);
            }
            return;
        }

        if (eduType == EduType.PSM) {
            if (!user.hasAuthority(Authority.MANAGE_PSM)) {
                throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_PSM_MANAGE);
            }
            return;
        }

        if (eduType == EduType.SAFETY && !user.hasAuthority(Authority.MANAGE_SAFETY)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_SAFETY_WRITE);
        }
    }

    @Transactional(readOnly = true)
    public List<EduReportSummaryDto> getEduReports(
            EduType type, DepartmentName departmentName, Long categoryId, Long id) {

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean hasAccess = user.hasAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);
        boolean canReadAllPsmCompanies = user.hasAuthority(Authority.MANAGE_PSM);
        Company psmCompanyFilter = canReadAllPsmCompanies ? null : user.getWorkLocation();

        Department dept;
        if (hasAccess) {
            // 권한 있음: departmentName이 지정되면 해당 부서, 없으면 null(전체 조회)
            dept =
                    (departmentName != null)
                            ? departmentRepository
                                    .findByName(departmentName)
                                    .orElseThrow(
                                            () ->
                                                    new CustomException(
                                                            ErrorCode.DEPARTMENT_NOT_FOUND))
                            : null;
        } else {
            // 권한 없음: 자신의 부서로 제한
            dept = user.getDepartment();
        }

        return eduReportQueryRepository.findEduReports(
                type, dept, categoryId, id, hasAccess, psmCompanyFilter, canReadAllPsmCompanies);
    }

    @Transactional(readOnly = true)
    public List<EduReportSummaryDto> getDepartmentEduReports(
            DepartmentName departmentName, Long id) {
        return getEduReports(EduType.DEPARTMENT, departmentName, null, id);
    }

    @Transactional(readOnly = true)
    public List<EduReportSummaryDto> getPsmEduReports(Long categoryId, Long id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean canReadAllCompanies = user.hasAuthority(Authority.MANAGE_PSM);
        Company companyFilter = canReadAllCompanies ? null : user.getWorkLocation();
        return eduReportQueryRepository.findPsmEduReports(
                categoryId, id, companyFilter, canReadAllCompanies);
    }

    @Transactional(readOnly = true)
    public List<EduReportSummaryDto> getSafetyEduReports(Long categoryId, Long id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean canReadAllCompanies = user.hasAuthority(Authority.MANAGE_SAFETY);
        Company companyFilter = canReadAllCompanies ? null : user.getWorkLocation();
        return eduReportQueryRepository.findSafetyEduReports(
                categoryId, id, companyFilter, canReadAllCompanies);
    }

    @Transactional(readOnly = true)
    public EduReportDetailDto getEduReport(Long eduReportId, Long id) {

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        EduReport report =
                eduReportRepository
                        .findById(eduReportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));

        boolean hasAccess = user.hasAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);
        return buildEduReportDetailDto(user, report, hasAccess);
    }

    @Transactional(readOnly = true)
    public EduReportDetailDto getDepartmentEduReport(Long eduReportId, Long id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        EduReport report =
                eduReportRepository
                        .findById(eduReportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));

        if (report.getEduType() != EduType.DEPARTMENT) {
            throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
        }

        boolean hasAccess = user.hasAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);
        if (!hasAccess) {
            if (user.getDepartment() == null
                    || report.getDepartment() == null
                    || !report.getDepartment().getId().equals(user.getDepartment().getId())) {
                throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
            }
        }

        return buildEduReportDetailDto(user, report, hasAccess);
    }

    @Transactional(readOnly = true)
    public EduReportDetailDto getPsmEduReport(Long eduReportId, Long id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        EduReport report =
                eduReportRepository
                        .findById(eduReportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));

        if (report.getEduType() != EduType.PSM) {
            throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
        }

        boolean canReadAllCompanies = user.hasAuthority(Authority.MANAGE_PSM);
        if (!canReadAllCompanies) {
            Company userCompany = user.getWorkLocation();
            if (userCompany == null) {
                throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
            }
            if (report.getCompany() != null && report.getCompany() != userCompany) {
                throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
            }
        }

        boolean hasAccess = user.hasAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);
        return buildEduReportDetailDto(user, report, hasAccess);
    }

    @Transactional(readOnly = true)
    public EduReportDetailDto getSafetyEduReport(Long eduReportId, Long id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        EduReport report =
                eduReportRepository
                        .findById(eduReportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));

        if (report.getEduType() != EduType.SAFETY) {
            throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
        }

        boolean canReadAllCompanies = user.hasAuthority(Authority.MANAGE_SAFETY);
        if (!canReadAllCompanies) {
            Company userCompany = user.getWorkLocation();
            if (userCompany == null) {
                throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
            }
            if (report.getCompany() != null && report.getCompany() != userCompany) {
                throw new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND);
            }
        }

        boolean hasAccess = user.hasAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);
        return buildEduReportDetailDto(user, report, hasAccess);
    }

    private EduReportDetailDto buildEduReportDetailDto(
            User user, EduReport report, boolean hasAccess) {
        List<EduAttendance> attendances = null;
        long numberOfPeople = -1L;

        if (hasAccess) {
            // MANAGE_DEPARTMENT_EDUCATION 권한 있음: 출석자 목록과 통계 포함
            attendances = eduAttendanceRepository.findAllByEduReportIdWithUser(report.getId());
            numberOfPeople = calculateTargetPeopleCount(report);
        }

        EduReportDetailDto dto =
                eduMapper.toDetailDto(report, attendances, numberOfPeople, s3Service);

        boolean isAttended = eduAttendanceRepository.existsByEduReportAndUser(report, user);
        dto.setAttendance(isAttended);
        return dto;
    }

    @Transactional
    public void deleteEduReport(Long eduReportId, Long id) {

        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.hasAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_EDU_REPORT);
        }

        EduReport report =
                eduReportRepository
                        .findById(eduReportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));

        report.getAttachments()
                .forEach(
                        attachment -> {
                            s3Service.deleteFile(attachment.getS3Key());
                        });
        eduReportRepository.delete(report);
    }

    public record FileDownloadDto(byte[] fileData, String originalFileName, long fileSize) {}

    @Transactional(readOnly = true)
    public FileDownloadDto getFileForDownload(Long attachmentId) {
        // DB 조회
        EduAttachment attachment =
                eduAttachmentRepository
                        .findById(attachmentId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_ATTACHMENT_NOT_FOUND));

        // S3 데이터 다운로드
        byte[] fileData = s3Service.downloadFile(attachment.getS3Key());

        return new FileDownloadDto(
                fileData, attachment.getOriginalFileName(), attachment.getFileSize());
    }

    // @Transactional(readOnly = true)
    // public String getDownloadUrl(Long attachmentId) {
    // EduAttachment attachment =
    // eduAttachmentRepository.findById(attachmentId).orElseThrow(
    // () -> new CustomException(ErrorCode.EDU_ATTACHMENT_NOT_FOUND)
    // );
    // return s3Service.getPresignedViewUrl(attachment.getS3Key());
    // }

    @Transactional
    public void markAttendance(Long reportId, MultipartFile signatureFile, Long userId)
            throws IOException {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        EduReport report =
                eduReportRepository
                        .findById(reportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));

        if (report.getStatus() != EduReportStatus.OPEN) {
            throw new CustomException(ErrorCode.EDU_REPORT_CLOSED);
        }

        if (eduAttendanceRepository.existsByEduReportAndUser(report, user)) {
            throw new CustomException(ErrorCode.ALREADY_MARKED_ATTENDANCE);
        }

        String signatureKey = null;
        try {
            // 서명 업로드 (S3)
            if (report.isSignatureRequired()) {
                if (signatureFile == null || signatureFile.isEmpty()) {
                    throw new CustomException(ErrorCode.NO_SIGNATURE_PROVIDED);
                }
                validatePngFormat(signatureFile);
                signatureKey = s3Service.uploadFile(signatureFile);
            }

            // 엔티티 생성 및 저장
            EduAttendance attendance = eduMapper.toEduAttendanceEntity(user, report, signatureKey);
            eduAttendanceRepository.save(attendance);

        } catch (DataIntegrityViolationException e) {
            deleteS3FileIfExist(signatureKey);
            throw new CustomException(ErrorCode.ALREADY_MARKED_ATTENDANCE);

        } catch (Exception e) {
            // 고아 객체 방지: 기타 DB 에러나 로직 에러 발생 시 S3 파일 삭제
            deleteS3FileIfExist(signatureKey);
            throw e; // 발생한 예외를 그대로 던져 트랜잭션 롤백 유도
        }
    }

    @Transactional
    public Long updateEduReport(
            Long eduReportId, EduReportUpdateRequestDto requestDto, Long userId) {
        return updateEduReport(eduReportId, requestDto, null, userId);
    }

    @Transactional
    public Long updateEduReport(
            Long eduReportId,
            EduReportUpdateRequestDto requestDto,
            List<MultipartFile> files,
            Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        EduReport report =
                eduReportRepository
                        .findById(eduReportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));
        validateCreateAuthority(user, report.getEduType());
        validateReportEditable(report);

        long signedCount = eduAttendanceRepository.countByEduReportId(eduReportId);
        if (signedCount > 0) {
            throw new CustomException(ErrorCode.EDU_REPORT_HAS_SIGNED_ATTENDEE);
        }

        List<String> uploadedAttachmentKeys = new ArrayList<>();
        try {
            report.setTitle(requestDto.getTitle().trim());
            report.setContent(requestDto.getContent().trim());
            report.setPinned(requestDto.isPinned());
            report.setSignatureRequired(requestDto.isSignatureRequired());

            if (report.getEduType() == EduType.DEPARTMENT) {
                if (requestDto.getDepartmentId() == null) {
                    throw new CustomException(ErrorCode.DEPARTMENT_ID_REQUIRED);
                }
                Department department =
                        departmentRepository
                                .findById(requestDto.getDepartmentId())
                                .orElseThrow(
                                        () -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
                report.setDepartment(department);
                report.setCategory(null);
                report.setCompany(null);
            } else {
                if (requestDto.getCategoryId() != null) {
                    EducationCategory category =
                            getValidatedCategory(report.getEduType(), requestDto.getCategoryId());
                    report.setCategory(category);
                } else if (report.getCategory() == null) {
                    throw new CustomException(ErrorCode.EDUCATION_CATEGORY_REQUIRED);
                }
                report.setDepartment(null);
            }

            deleteAttachments(report, requestDto.getDeleteAttachmentIds());
            addAttachments(report, files, uploadedAttachmentKeys);
        } catch (Exception e) {
            uploadedAttachmentKeys.forEach(this::deleteS3FileIfExist);
            throw e;
        }

        return report.getId();
    }

    @Transactional
    public Long updateEduReportStatus(
            Long eduReportId, EduReportStatusUpdateRequestDto requestDto, Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        EduReport report =
                eduReportRepository
                        .findById(eduReportId)
                        .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));
        validateCreateAuthority(user, report.getEduType());

        report.setStatus(requestDto.getStatus());
        return report.getId();
    }

    // S3 파일 삭제를 위한 헬퍼 메서드
    private void deleteS3FileIfExist(String signatureKey) {
        if (signatureKey != null) {
            try {
                s3Service.deleteFile(signatureKey);
            } catch (Exception s3Ex) {
                log.error("롤백 중 S3 파일 삭제 실패 (고아 객체 발생 가능): {}", signatureKey);
            }
        }
    }

    private void validatePngFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("image/png")) {
            throw new CustomException(ErrorCode.INVALID_SIGNATURE_FORMAT);
        }
    }

    private boolean isRealAttachmentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            return false;
        }

        // Swagger UI generated curl can send placeholder part: files=string -> filename "blob"
        if ("blob".equalsIgnoreCase(originalFileName) && file.getSize() <= 32) {
            try {
                String payload = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
                if (payload.isEmpty() || "string".equalsIgnoreCase(payload)) {
                    return false;
                }
            } catch (IOException ignored) {
                // Ignore and treat as real file when content cannot be read.
            }
        }

        return true;
    }

    private void validateReportEditable(EduReport report) {
        if (report.getStatus() != EduReportStatus.OPEN) {
            throw new CustomException(ErrorCode.EDU_REPORT_CLOSED);
        }
    }

    private void deleteAttachments(EduReport report, List<Long> deleteAttachmentIds) {
        if (deleteAttachmentIds == null || deleteAttachmentIds.isEmpty()) {
            return;
        }

        for (Long attachmentId : new LinkedHashSet<>(deleteAttachmentIds)) {
            EduAttachment attachment =
                    eduAttachmentRepository
                            .findById(attachmentId)
                            .orElseThrow(
                                    () -> new CustomException(ErrorCode.EDU_ATTACHMENT_NOT_FOUND));

            if (attachment.getEduReport() == null
                    || !attachment.getEduReport().getId().equals(report.getId())) {
                throw new CustomException(ErrorCode.EDU_ATTACHMENT_NOT_FOUND);
            }

            s3Service.deleteFile(attachment.getS3Key());
            report.getAttachments().remove(attachment);
            eduAttachmentRepository.delete(attachment);
        }
    }

    private void addAttachments(
            EduReport report, List<MultipartFile> files, List<String> uploadedAttachmentKeys) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {
            if (!isRealAttachmentFile(file)) {
                continue;
            }

            String s3Key;
            try {
                s3Key = s3Service.uploadFile(file);
            } catch (IOException e) {
                throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
            }

            uploadedAttachmentKeys.add(s3Key);

            EduAttachment attachment = new EduAttachment();
            attachment.setOriginalFileName(file.getOriginalFilename());
            attachment.setS3Key(s3Key);
            attachment.setFileSize(file.getSize());
            report.addAttachment(attachment);
        }
    }

    private EducationCategory getValidatedCategory(EduType eduType, Long categoryId) {
        if (eduType != EduType.PSM && eduType != EduType.SAFETY) {
            return null;
        }

        EducationCategory category =
                educationCategoryRepository
                        .findById(categoryId)
                        .orElseThrow(
                                () -> new CustomException(ErrorCode.EDUCATION_CATEGORY_NOT_FOUND));

        EducationCategoryType expectedType =
                eduType == EduType.PSM ? EducationCategoryType.PSM : EducationCategoryType.SAFETY;
        if (category.getCategoryType() != expectedType) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
        return category;
    }

    private long calculateTargetPeopleCount(EduReport report) {
        if (report.getEduType() == EduType.DEPARTMENT && report.getDepartment() != null) {
            return userRepository.countByDepartment(report.getDepartment());
        }
        return userRepository.count();
    }
}
