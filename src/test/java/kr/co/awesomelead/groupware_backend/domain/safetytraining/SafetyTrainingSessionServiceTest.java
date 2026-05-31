package kr.co.awesomelead.groupware_backend.domain.safetytraining;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSession;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionAttendeeRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionRepository;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.service.SafetyTrainingSessionService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class SafetyTrainingSessionServiceTest {

    @Mock private SafetyTrainingSessionRepository sessionRepository;
    @Mock private SafetyTrainingSessionAttendeeRepository attendeeRepository;
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
        actor = User.builder().id(USER_ID).nameKor("작성자").build();

        session =
                SafetyTrainingSession.builder()
                        .id(SESSION_ID)
                        .title("2024년 안전보건교육")
                        .companyScope(Company.AWESOME)
                        .createdBy(actor)
                        .build();
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
}
