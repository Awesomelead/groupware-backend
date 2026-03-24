package kr.co.awesomelead.groupware_backend.domain.safetytraining.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingPreviewResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSession;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttendee;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionAttendeeRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        String educationDateText = toEducationDateText(requestDto.getStartAt(), requestDto.getEndAt());

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
        String educationDateText = toEducationDateText(requestDto.getStartAt(), requestDto.getEndAt());

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
}
