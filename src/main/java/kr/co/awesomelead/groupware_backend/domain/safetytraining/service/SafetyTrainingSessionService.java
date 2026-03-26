package kr.co.awesomelead.groupware_backend.domain.safetytraining.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionSearchConditionDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingPreviewResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingSessionDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingSessionSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSession;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttendee;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationMethod;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingCompletionStatus;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionAttendeeRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SafetyTrainingSessionService {

    private static final DateTimeFormatter DATE_TIME_TEXT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");

    private final SafetyTrainingSessionRepository sessionRepository;
    private final SafetyTrainingSessionAttendeeRepository attendeeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SafetyTrainingExcelService safetyTrainingExcelService;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public SafetyTrainingPreviewResponseDto preview(
            Long userId, SafetyTrainingSessionCreateRequestDto requestDto) {
        User actor =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateSafetyWriteAuthority(actor);
        validateTimeRange(requestDto.getStartAt(), requestDto.getEndAt());

        User instructor = findAndValidateInstructor(requestDto);
        List<User> attendees = findTargetAttendees(requestDto);
        String educationDateText =
                toEducationDateText(requestDto.getStartAt(), requestDto.getEndAt());

        byte[] excelBytes =
                safetyTrainingExcelService.buildPreviewExcel(
                        requestDto, educationDateText, attendees, instructor.getNameKor());

        String previewFileKey =
                s3Service.uploadBytes(
                        excelBytes,
                        "safety-training-preview-" + System.currentTimeMillis() + ".xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String previewUrl = s3Service.getPresignedViewUrl(previewFileKey);

        return SafetyTrainingPreviewResponseDto.builder()
                .previewFileUrl(previewUrl)
                .targetCount(attendees.size())
                .attendedCount(0)
                .absentCount(0)
                .build();
    }

    @Transactional
    public Long create(Long userId, SafetyTrainingSessionCreateRequestDto requestDto) {
        User actor =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateSafetyWriteAuthority(actor);
        validateTimeRange(requestDto.getStartAt(), requestDto.getEndAt());

        User instructor = findAndValidateInstructor(requestDto);

        String methodsJson = toMethodsJson(requestDto);
        String educationDateText =
                toEducationDateText(requestDto.getStartAt(), requestDto.getEndAt());

        SafetyTrainingSession session =
                SafetyTrainingSession.builder()
                        .title(requestDto.getTitle().trim())
                        .educationType(requestDto.getEducationType())
                        .educationMethodsJson(methodsJson)
                        .startAt(requestDto.getStartAt())
                        .endAt(requestDto.getEndAt())
                        .educationDateText(educationDateText)
                        .educationContent(requestDto.getEducationContent())
                        .place(requestDto.getPlace().trim())
                        .companyScope(requestDto.getCompanyScope())
                        .instructorUser(instructor)
                        .instructorNameSnapshot(instructor.getNameKor())
                        .createdBy(actor)
                        .build();

        SafetyTrainingSession saved = sessionRepository.save(session);

        List<User> attendees = findTargetAttendees(requestDto);

        List<SafetyTrainingSessionAttendee> rows =
                attendees.stream()
                        .map(
                                user ->
                                        SafetyTrainingSessionAttendee.builder()
                                                .session(saved)
                                                .user(user)
                                                .status(SafetyTrainingAttendeeStatus.PENDING)
                                                .build())
                        .toList();

        attendeeRepository.saveAll(rows);

        saved.setTargetCount(rows.size());
        saved.setAttendedCount(0);
        saved.setAbsentCount(0);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public Page<SafetyTrainingSessionSummaryResponseDto> getSessions(
            Long userId, SafetyTrainingSessionSearchConditionDto condition, Pageable pageable) {
        User actor =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        SafetyTrainingSessionSearchConditionDto filter =
                condition == null ? new SafetyTrainingSessionSearchConditionDto() : condition;

        boolean canReadAllCompanies = actor.hasAuthority(Authority.WRITE_SAFETY);
        Company companyScope =
                canReadAllCompanies ? filter.getCompanyScope() : actor.getWorkLocation();

        if (!canReadAllCompanies && companyScope == null) {
            return Page.empty(pageable);
        }

        return sessionRepository
                .findAllByFilters(
                        companyScope,
                        filter.getEducationType(),
                        filter.getStatus(),
                        filter.getStartAtFrom(),
                        filter.getStartAtTo(),
                        pageable)
                .map(this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public SafetyTrainingSessionDetailResponseDto getSessionDetail(Long sessionId, Long userId) {
        User actor =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        SafetyTrainingSession session =
                sessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.SAFETY_TRAINING_SESSION_NOT_FOUND));

        validateSessionReadAccess(actor, session);

        SafetyTrainingSessionAttendee attendee =
                attendeeRepository
                        .findBySessionIdAndUserId(sessionId, userId)
                        .orElse(
                                SafetyTrainingSessionAttendee.builder()
                                        .status(SafetyTrainingAttendeeStatus.PENDING)
                                        .build());

        SafetyTrainingAttendeeStatus myStatus = attendee.getStatus();
        SafetyTrainingCompletionStatus completionStatus =
                myStatus == SafetyTrainingAttendeeStatus.SIGNED
                        ? SafetyTrainingCompletionStatus.COMPLETED
                        : SafetyTrainingCompletionStatus.INCOMPLETE;

        String reportFileUrl =
                session.getReportFileKey() == null
                        ? null
                        : s3Service.getPresignedViewUrl(session.getReportFileKey());

        return SafetyTrainingSessionDetailResponseDto.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .educationType(session.getEducationType())
                .educationMethods(toMethods(session.getEducationMethodsJson()))
                .startAt(session.getStartAt())
                .endAt(session.getEndAt())
                .place(session.getPlace())
                .companyScope(session.getCompanyScope())
                .status(session.getStatus())
                .instructorUserId(
                        session.getInstructorUser() == null
                                ? null
                                : session.getInstructorUser().getId())
                .instructorName(session.getInstructorNameSnapshot())
                .targetCount(session.getTargetCount())
                .attendedCount(session.getAttendedCount())
                .absentCount(session.getAbsentCount())
                .reportFileUrl(reportFileUrl)
                .myAttendanceStatus(myStatus)
                .myCompletionStatus(completionStatus)
                .mySignedAt(attendee.getSignedAt())
                .mySignatureUrl(
                        attendee.getSignatureKey() == null
                                ? null
                                : s3Service.getPresignedViewUrl(attendee.getSignatureKey()))
                .canSign(myStatus != SafetyTrainingAttendeeStatus.SIGNED)
                .build();
    }

    @Transactional
    public void signAttendance(Long sessionId, Long userId, MultipartFile signatureFile)
            throws IOException {
        User actor =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        SafetyTrainingSession session =
                sessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.SAFETY_TRAINING_SESSION_NOT_FOUND));

        validateSessionReadAccess(actor, session);

        SafetyTrainingSessionAttendee attendee =
                attendeeRepository
                        .findBySessionIdAndUserId(sessionId, userId)
                        .orElseThrow(
                                () ->
                                        new CustomException(
                                                ErrorCode.SAFETY_TRAINING_ATTENDEE_NOT_FOUND));

        if (attendee.getStatus() == SafetyTrainingAttendeeStatus.SIGNED) {
            throw new CustomException(ErrorCode.ALREADY_MARKED_ATTENDANCE);
        }

        if (signatureFile == null || signatureFile.isEmpty()) {
            throw new CustomException(ErrorCode.NO_SIGNATURE_PROVIDED);
        }
        validatePngFormat(signatureFile);

        String signatureKey = null;
        try {
            signatureKey = s3Service.uploadFile(signatureFile);

            attendee.setStatus(SafetyTrainingAttendeeStatus.SIGNED);
            attendee.setSignedAt(LocalDateTime.now());
            attendee.setSignatureKey(signatureKey);
            attendee.setAbsentReason(null);

            syncSessionCounts(session);
        } catch (Exception e) {
            deleteS3FileIfExist(signatureKey);
            throw e;
        }
    }

    private void validateSafetyWriteAuthority(User user) {
        if (!user.hasAuthority(Authority.WRITE_SAFETY)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_SAFETY_WRITE);
        }
    }

    private User findAndValidateInstructor(SafetyTrainingSessionCreateRequestDto requestDto) {
        return userRepository
                .findById(requestDto.getInstructorUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private List<User> findTargetAttendees(SafetyTrainingSessionCreateRequestDto requestDto) {
        return userRepository.findAllByCompanyAndStatusExcludingPosition(
                requestDto.getCompanyScope(), Status.AVAILABLE, Position.CEO);
    }

    private void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
        }
    }

    private String toMethodsJson(SafetyTrainingSessionCreateRequestDto requestDto) {
        try {
            return objectMapper.writeValueAsString(requestDto.getEducationMethods());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
    }

    private String toEducationDateText(LocalDateTime startAt, LocalDateTime endAt) {
        long minutes = Duration.between(startAt, endAt).toMinutes();
        long hours = minutes / 60;
        long remainMinutes = minutes % 60;

        String durationText =
                remainMinutes == 0
                        ? String.format("%d시간", hours)
                        : String.format("%d시간 %d분", hours, remainMinutes);

        return String.format(
                "%s ~ %s (%s)",
                startAt.format(DATE_TIME_TEXT_FORMATTER),
                endAt.format(DATE_TIME_TEXT_FORMATTER),
                durationText);
    }

    private void deleteS3FileIfExist(String signatureKey) {
        if (signatureKey == null) {
            return;
        }
        try {
            s3Service.deleteFile(signatureKey);
        } catch (Exception ignored) {
            // 트랜잭션 롤백 시 S3 고아 파일 정리 실패 가능
        }
    }

    private void validatePngFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("image/png")) {
            throw new CustomException(ErrorCode.INVALID_SIGNATURE_FORMAT);
        }
    }

    private void validateSessionReadAccess(User actor, SafetyTrainingSession session) {
        if (actor.hasAuthority(Authority.WRITE_SAFETY)) {
            return;
        }
        if (actor.getWorkLocation() == null
                || actor.getWorkLocation() != session.getCompanyScope()) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_SAFETY_READ);
        }
    }

    private List<SafetyEducationMethod> toMethods(String educationMethodsJson) {
        if (educationMethodsJson == null || educationMethodsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(
                    educationMethodsJson,
                    objectMapper
                            .getTypeFactory()
                            .constructCollectionType(List.class, SafetyEducationMethod.class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void syncSessionCounts(SafetyTrainingSession session) {
        long attended =
                attendeeRepository.countBySessionIdAndStatus(
                        session.getId(), SafetyTrainingAttendeeStatus.SIGNED);
        long absent =
                attendeeRepository.countBySessionIdAndStatus(
                        session.getId(), SafetyTrainingAttendeeStatus.ABSENT);
        session.setAttendedCount((int) attended);
        session.setAbsentCount((int) absent);
    }

    private SafetyTrainingSessionSummaryResponseDto toSummaryDto(SafetyTrainingSession session) {
        return SafetyTrainingSessionSummaryResponseDto.builder()
                .sessionId(session.getId())
                .title(session.getTitle())
                .educationType(session.getEducationType())
                .startAt(session.getStartAt())
                .endAt(session.getEndAt())
                .place(session.getPlace())
                .companyScope(session.getCompanyScope())
                .instructorUserId(
                        session.getInstructorUser() == null
                                ? null
                                : session.getInstructorUser().getId())
                .instructorName(session.getInstructorNameSnapshot())
                .status(session.getStatus())
                .targetCount(session.getTargetCount())
                .attendedCount(session.getAttendedCount())
                .absentCount(session.getAbsentCount())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
