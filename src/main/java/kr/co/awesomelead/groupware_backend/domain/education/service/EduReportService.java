package kr.co.awesomelead.groupware_backend.domain.education.service;

import java.io.IOException;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportAdminDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttachment;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.education.mapper.EduMapper;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttendanceRepository;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduReportRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class EduReportService {

    private final EduReportRepository eduReportRepository;
    private final EduAttendanceRepository eduAttendanceRepository;
    private final EduAttachmentRepository eduAttachmentRepository;
    private final EduMapper eduMapper;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public void createEduReport(EduReportRequestDto requestDto, List<MultipartFile> files, Long id)
        throws IOException {

        User user =
            userRepository
                .findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.hasAuthority(Authority.WRITE_EDUCATION)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_EDU_REPORT);
        }

        Department department = null;
        if (requestDto.getEduType() == EduType.DEPARTMENT && requestDto.getDepartmentId() != null) {
            department =
                departmentRepository
                    .findById(requestDto.getDepartmentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

        EduReport report = eduMapper.toEduReportEntity(requestDto, department);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
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
        eduReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<EduReportSummaryDto> getEduReports(EduType type, Long id) {

        User user =
            userRepository
                .findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Department department = user.getDepartment();

        return eduReportRepository.findEduReportsWithFilters(type, department, user);
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

        EduReportDetailDto dto = eduMapper.toDetailDto(report, s3Service);

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

        if (!user.hasAuthority(Authority.WRITE_EDUCATION)) {
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

    public record FileDownloadDto(byte[] fileData, String originalFileName, long fileSize) {

    }

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

//    @Transactional(readOnly = true)
//    public String getDownloadUrl(Long attachmentId) {
//        EduAttachment attachment = eduAttachmentRepository.findById(attachmentId).orElseThrow(
//            () -> new CustomException(ErrorCode.EDU_ATTACHMENT_NOT_FOUND)
//        );
//        return s3Service.getPresignedViewUrl(attachment.getS3Key());
//    }

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

    @Transactional(readOnly = true)
    public EduReportAdminDetailDto getEduReportForAdmin(Long id) {

        EduReport report =
            eduReportRepository
                .findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.EDU_REPORT_NOT_FOUND));

        // 출석 명단 조회
        List<EduAttendance> attendances = eduAttendanceRepository.findAllByEduReportIdWithUser(id);

        // 통계 데이터 계산
        long numberOfPeople = calculateTargetPeopleCount(report); // 교육 대상 인원

        return eduMapper.toAdminDetailDto(report, attendances, numberOfPeople, s3Service);
    }

    private long calculateTargetPeopleCount(EduReport report) {
        if (report.getEduType() == EduType.DEPARTMENT && report.getDepartment() != null) {
            // 부서 교육인 경우 해당 부서의 인원수만 카운트
            return userRepository.countByDepartment(report.getDepartment());
        }
        // 공통 교육(COMMON) 등은 전체 인원수 카운트
        return userRepository.count();
    }
}
