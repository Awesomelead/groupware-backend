package kr.co.awesomelead.groupware_backend.domain.safetytraining;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.request.SafetyTrainingSessionUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.dto.response.SafetyTrainingSessionDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSession;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttachment;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttendee;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationMethod;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionAttendeeRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.service.SafetyTrainingSessionService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class SafetyTrainingSessionServiceTest {

    @Mock private SafetyTrainingSessionRepository sessionRepository;
    @Mock private SafetyTrainingSessionAttendeeRepository attendeeRepository;
    @Mock private SafetyTrainingSessionAttachmentRepository attachmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private kr.co.awesomelead.groupware_backend.domain.safetytraining.service
                    .SafetyTrainingExcelService
            safetyTrainingExcelService;

    @Mock private kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service s3Service;

    @InjectMocks private SafetyTrainingSessionService safetyTrainingSessionService;

    private static final Long SESSION_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long OTHER_USER_ID = 99L;

    private User actor;
    private SafetyTrainingSession session;

    @BeforeEach
    void setUp() {
        actor =
                User.builder()
                        .id(USER_ID)
                        .nameKor("작성자")
                        .workLocation(Company.AWESOME)
                        .position(Position.STAFF)
                        .build();
        actor.addAuthority(Authority.MANAGE_SAFETY);

        session =
                SafetyTrainingSession.builder()
                        .id(SESSION_ID)
                        .title("2024년 안전보건교육")
                        .educationType(SafetyEducationType.REGULAR)
                        .educationMethodsJson("[\"LECTURE\"]")
                        .startAt(LocalDateTime.of(2026, 7, 2, 9, 0))
                        .endAt(LocalDateTime.of(2026, 7, 2, 10, 0))
                        .place("교육장")
                        .companyScope(Company.AWESOME)
                        .instructorUser(actor)
                        .instructorNameSnapshot("작성자")
                        .createdBy(actor)
                        .build();
    }

    @Nested
    @DisplayName("create - 안전보건교육 세션 등록")
    class Create {

        @Test
        @DisplayName("첨부파일이 있으면 S3 업로드 후 첨부파일 메타데이터를 저장한다")
        void create_savesAttachments() throws Exception {
            // given
            SafetyTrainingSessionCreateRequestDto requestDto = createRequestDto();
            MockMultipartFile attachment =
                    new MockMultipartFile(
                            "attachments",
                            "교육자료.pdf",
                            "application/pdf",
                            "file-content".getBytes());

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(objectMapper.writeValueAsString(List.of(SafetyEducationMethod.LECTURE)))
                    .thenReturn("[\"LECTURE\"]");
            when(sessionRepository.save(any(SafetyTrainingSession.class)))
                    .thenAnswer(
                            invocation -> {
                                SafetyTrainingSession saved = invocation.getArgument(0);
                                saved.setId(SESSION_ID);
                                return saved;
                            });
            when(userRepository.findAllByCompanyAndStatusExcludingPosition(
                            Company.AWESOME, Status.AVAILABLE, Position.CEO))
                    .thenReturn(List.of(actor));
            when(s3Service.uploadFile(attachment)).thenReturn("safety-training/file-key.pdf");

            // when
            Long sessionId =
                    safetyTrainingSessionService.create(USER_ID, requestDto, List.of(attachment));

            // then
            assertThat(sessionId).isEqualTo(SESSION_ID);
            verify(attachmentRepository)
                    .saveAll(
                            argThat(
                                    attachments -> {
                                        SafetyTrainingSessionAttachment savedAttachment =
                                                attachments.iterator().next();
                                        assertThat(savedAttachment.getSession().getId())
                                                .isEqualTo(SESSION_ID);
                                        assertThat(savedAttachment.getOriginalFileName())
                                                .isEqualTo("교육자료.pdf");
                                        assertThat(savedAttachment.getFileKey())
                                                .isEqualTo("safety-training/file-key.pdf");
                                        assertThat(savedAttachment.getContentType())
                                                .isEqualTo("application/pdf");
                                        assertThat(savedAttachment.getFileSize()).isEqualTo(12);
                                        return true;
                                    }));
        }

        @Test
        @DisplayName("Swagger가 파일 미첨부 시 보내는 blob placeholder는 첨부파일로 저장하지 않는다")
        void create_ignoresSwaggerPlaceholderAttachment() throws Exception {
            // given
            SafetyTrainingSessionCreateRequestDto requestDto = createRequestDto();
            MockMultipartFile placeholder =
                    new MockMultipartFile("attachments", "blob", "*/*", "string".getBytes());

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(objectMapper.writeValueAsString(List.of(SafetyEducationMethod.LECTURE)))
                    .thenReturn("[\"LECTURE\"]");
            when(sessionRepository.save(any(SafetyTrainingSession.class)))
                    .thenAnswer(
                            invocation -> {
                                SafetyTrainingSession saved = invocation.getArgument(0);
                                saved.setId(SESSION_ID);
                                return saved;
                            });
            when(userRepository.findAllByCompanyAndStatusExcludingPosition(
                            Company.AWESOME, Status.AVAILABLE, Position.CEO))
                    .thenReturn(List.of(actor));

            // when
            Long sessionId =
                    safetyTrainingSessionService.create(USER_ID, requestDto, List.of(placeholder));

            // then
            assertThat(sessionId).isEqualTo(SESSION_ID);
            verify(s3Service, never()).uploadFile(placeholder);
            verify(attachmentRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("update - 안전보건교육 세션 수정")
    class Update {

        @Test
        @DisplayName("deleteAttachmentIds로 기존 첨부파일을 삭제하고 새 첨부파일을 추가한다")
        void update_deletesSelectedAttachmentsAndAddsNewAttachments() throws Exception {
            // given
            SafetyTrainingSessionUpdateRequestDto requestDto = updateRequestDto();
            ReflectionTestUtils.setField(requestDto, "deleteAttachmentIds", List.of(1L));
            SafetyTrainingSessionAttachment oldAttachment =
                    SafetyTrainingSessionAttachment.builder()
                            .id(1L)
                            .session(session)
                            .originalFileName("기존자료.pdf")
                            .fileKey("safety-training/old-file.pdf")
                            .contentType("application/pdf")
                            .fileSize(100L)
                            .build();
            MockMultipartFile newAttachment =
                    new MockMultipartFile(
                            "attachments", "새자료.pdf", "application/pdf", "new-file".getBytes());

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(attendeeRepository.countBySessionIdAndStatus(
                            SESSION_ID, SafetyTrainingAttendeeStatus.SIGNED))
                    .thenReturn(0L);
            when(objectMapper.writeValueAsString(List.of(SafetyEducationMethod.LECTURE)))
                    .thenReturn("[\"LECTURE\"]");
            when(userRepository.findAllByCompanyAndStatusExcludingPosition(
                            Company.AWESOME, Status.AVAILABLE, Position.CEO))
                    .thenReturn(List.of(actor));
            when(attachmentRepository.findById(1L)).thenReturn(Optional.of(oldAttachment));
            when(s3Service.uploadFile(newAttachment)).thenReturn("safety-training/new-file.pdf");

            // when
            Long updatedId =
                    safetyTrainingSessionService.update(
                            SESSION_ID, USER_ID, requestDto, List.of(newAttachment));

            // then
            assertThat(updatedId).isEqualTo(SESSION_ID);
            verify(attachmentRepository).delete(oldAttachment);
            verify(s3Service).deleteFile("safety-training/old-file.pdf");
            verify(attachmentRepository)
                    .saveAll(
                            argThat(
                                    attachments -> {
                                        SafetyTrainingSessionAttachment savedAttachment =
                                                attachments.iterator().next();
                                        assertThat(savedAttachment.getOriginalFileName())
                                                .isEqualTo("새자료.pdf");
                                        assertThat(savedAttachment.getFileKey())
                                                .isEqualTo("safety-training/new-file.pdf");
                                        return true;
                                    }));
        }
    }

    @Nested
    @DisplayName("getSessionDetail - 안전보건교육 상세 조회")
    class GetSessionDetail {

        @Test
        @DisplayName("교육 첨부파일 목록과 조회 URL을 응답에 포함한다")
        void getSessionDetail_returnsAttachments() {
            // given
            SafetyTrainingSessionAttachment attachment =
                    SafetyTrainingSessionAttachment.builder()
                            .id(3L)
                            .session(session)
                            .originalFileName("교육자료.pdf")
                            .fileKey("safety-training/file-key.pdf")
                            .contentType("application/pdf")
                            .fileSize(1024L)
                            .build();
            SafetyTrainingSessionAttendee attendee =
                    SafetyTrainingSessionAttendee.builder()
                            .session(session)
                            .user(actor)
                            .status(SafetyTrainingAttendeeStatus.PENDING)
                            .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(attendeeRepository.findBySessionIdAndUserId(SESSION_ID, USER_ID))
                    .thenReturn(Optional.of(attendee));
            when(attachmentRepository.findAllBySessionIdOrderByIdAsc(SESSION_ID))
                    .thenReturn(List.of(attachment));
            when(s3Service.getPresignedViewUrl("safety-training/file-key.pdf"))
                    .thenReturn("https://example.com/file-key.pdf");
            when(sessionRepository.findFirstByIdGreaterThanOrderByIdAsc(SESSION_ID))
                    .thenReturn(Optional.empty());
            when(sessionRepository.findFirstByIdLessThanOrderByIdDesc(SESSION_ID))
                    .thenReturn(Optional.empty());

            // when
            SafetyTrainingSessionDetailResponseDto response =
                    safetyTrainingSessionService.getSessionDetail(SESSION_ID, USER_ID);

            // then
            assertThat(response.getAttachments()).hasSize(1);
            SafetyTrainingSessionDetailResponseDto.AttachmentItem attachmentItem =
                    response.getAttachments().get(0);
            assertThat(attachmentItem.getAttachmentId()).isEqualTo(3L);
            assertThat(attachmentItem.getFileName()).isEqualTo("교육자료.pdf");
            assertThat(attachmentItem.getFileUrl()).isEqualTo("https://example.com/file-key.pdf");
            assertThat(attachmentItem.getContentType()).isEqualTo("application/pdf");
            assertThat(attachmentItem.getFileSize()).isEqualTo(1024L);
        }
    }

    @Nested
    @DisplayName("remindSession - 안전보건교육 세션 리마인드 알림 전송")
    class RemindSession {

        @Test
        @DisplayName("userId에 해당하는 유저가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void remindSession_throwsWhenUserNotFound() {
            // given
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(
                            () -> safetyTrainingSessionService.remindSession(SESSION_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(sessionRepository, never()).findById(anyLong());
            verify(notificationService, never())
                    .sendSafetyTrainingSessionRemindAlertToAttendees(
                            anyLong(), anyString(), anyList());
        }

        @Test
        @DisplayName("sessionId에 해당하는 세션이 없으면 SAFETY_TRAINING_SESSION_NOT_FOUND 예외가 발생한다")
        void remindSession_throwsWhenSessionNotFound() {
            // given
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(
                            () -> safetyTrainingSessionService.remindSession(SESSION_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", ErrorCode.SAFETY_TRAINING_SESSION_NOT_FOUND);

            verify(notificationService, never())
                    .sendSafetyTrainingSessionRemindAlertToAttendees(
                            anyLong(), anyString(), anyList());
        }

        @Test
        @DisplayName("세션의 createdBy가 null이면 NO_AUTHORITY_FOR_SAFETY_WRITE 예외가 발생한다")
        void remindSession_throwsWhenCreatedByIsNull() {
            // given
            SafetyTrainingSession sessionWithNullCreator =
                    SafetyTrainingSession.builder()
                            .id(SESSION_ID)
                            .title("2024년 안전보건교육")
                            .companyScope(Company.AWESOME)
                            .createdBy(null)
                            .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(sessionRepository.findById(SESSION_ID))
                    .thenReturn(Optional.of(sessionWithNullCreator));

            // when & then
            assertThatThrownBy(
                            () -> safetyTrainingSessionService.remindSession(SESSION_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", ErrorCode.NO_AUTHORITY_FOR_SAFETY_WRITE);

            verify(notificationService, never())
                    .sendSafetyTrainingSessionRemindAlertToAttendees(
                            anyLong(), anyString(), anyList());
        }

        @Test
        @DisplayName("세션의 createdBy가 요청 userId와 다르면 NO_AUTHORITY_FOR_SAFETY_WRITE 예외가 발생한다")
        void remindSession_throwsWhenCreatedByMismatch() {
            // given
            User anotherUser = User.builder().id(OTHER_USER_ID).nameKor("다른사람").build();
            SafetyTrainingSession sessionOwnedByOther =
                    SafetyTrainingSession.builder()
                            .id(SESSION_ID)
                            .title("2024년 안전보건교육")
                            .companyScope(Company.AWESOME)
                            .createdBy(anotherUser)
                            .build();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(sessionRepository.findById(SESSION_ID))
                    .thenReturn(Optional.of(sessionOwnedByOther));

            // when & then
            assertThatThrownBy(
                            () -> safetyTrainingSessionService.remindSession(SESSION_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", ErrorCode.NO_AUTHORITY_FOR_SAFETY_WRITE);

            verify(notificationService, never())
                    .sendSafetyTrainingSessionRemindAlertToAttendees(
                            anyLong(), anyString(), anyList());
        }

        @Test
        @DisplayName("정상 흐름: 대상자 조회 후 리마인드 알림을 전송한다")
        void remindSession_success() {
            // given
            User attendee1 = User.builder().id(101L).nameKor("직원1").build();
            User attendee2 = User.builder().id(102L).nameKor("직원2").build();
            List<User> targetUsers = List.of(attendee1, attendee2);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(actor));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(userRepository.findAllByCompanyAndStatusExcludingPosition(
                            Company.AWESOME, Status.AVAILABLE, Position.CEO))
                    .thenReturn(targetUsers);

            // when
            safetyTrainingSessionService.remindSession(SESSION_ID, USER_ID);

            // then
            verify(userRepository)
                    .findAllByCompanyAndStatusExcludingPosition(
                            Company.AWESOME, Status.AVAILABLE, Position.CEO);
            verify(notificationService)
                    .sendSafetyTrainingSessionRemindAlertToAttendees(
                            eq(SESSION_ID), eq("2024년 안전보건교육"), eq(List.of(101L, 102L)));
        }
    }

    private SafetyTrainingSessionCreateRequestDto createRequestDto() {
        SafetyTrainingSessionCreateRequestDto requestDto =
                new SafetyTrainingSessionCreateRequestDto();
        ReflectionTestUtils.setField(requestDto, "title", "2026년 안전보건교육");
        ReflectionTestUtils.setField(requestDto, "educationType", SafetyEducationType.REGULAR);
        ReflectionTestUtils.setField(
                requestDto, "educationMethods", List.of(SafetyEducationMethod.LECTURE));
        ReflectionTestUtils.setField(requestDto, "startAt", LocalDateTime.of(2026, 7, 2, 9, 0));
        ReflectionTestUtils.setField(requestDto, "endAt", LocalDateTime.of(2026, 7, 2, 10, 0));
        ReflectionTestUtils.setField(requestDto, "educationContent", "교육 내용");
        ReflectionTestUtils.setField(requestDto, "place", "교육장");
        ReflectionTestUtils.setField(requestDto, "instructorUserId", USER_ID);
        ReflectionTestUtils.setField(requestDto, "companyScope", Company.AWESOME);
        return requestDto;
    }

    private SafetyTrainingSessionUpdateRequestDto updateRequestDto() {
        SafetyTrainingSessionUpdateRequestDto requestDto =
                new SafetyTrainingSessionUpdateRequestDto();
        ReflectionTestUtils.setField(requestDto, "title", "2026년 안전보건교육 수정");
        ReflectionTestUtils.setField(requestDto, "educationType", SafetyEducationType.REGULAR);
        ReflectionTestUtils.setField(
                requestDto, "educationMethods", List.of(SafetyEducationMethod.LECTURE));
        ReflectionTestUtils.setField(requestDto, "startAt", LocalDateTime.of(2026, 7, 2, 9, 0));
        ReflectionTestUtils.setField(requestDto, "endAt", LocalDateTime.of(2026, 7, 2, 10, 0));
        ReflectionTestUtils.setField(requestDto, "educationContent", "교육 내용 수정");
        ReflectionTestUtils.setField(requestDto, "place", "교육장");
        ReflectionTestUtils.setField(requestDto, "instructorUserId", USER_ID);
        ReflectionTestUtils.setField(requestDto, "companyScope", Company.AWESOME);
        return requestDto;
    }
}
