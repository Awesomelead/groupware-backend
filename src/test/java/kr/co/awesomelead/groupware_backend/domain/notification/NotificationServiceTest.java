package kr.co.awesomelead.groupware_backend.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.fcm.event.FcmSendEvent;
import kr.co.awesomelead.groupware_backend.domain.notification.dto.response.NotificationResponseDto;
import kr.co.awesomelead.groupware_backend.domain.notification.entity.Notification;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;
import kr.co.awesomelead.groupware_backend.domain.notification.repository.NotificationRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserRepository userRepository;

    @InjectMocks private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("createNotification - metadata 없이 호출 시 metadata가 null로 저장된다")
    void createNotification_withoutMetadata_savesNotification() {
        notificationService.createNotification(1L, "제목", "내용", NotificationDomainType.VISIT, 10L);

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMetadata()).isNull();
    }

    @Test
    @DisplayName("createNotification - metadata 포함하여 호출 시 엔티티에 metadata가 저장된다")
    void createNotification_withMetadata_savesMetadata() {
        Map<String, Object> metadata = Map.of("requestId", 99, "status", "PENDING");

        notificationService.createNotification(
                1L, "제목", "내용", NotificationDomainType.VISIT, 10L, metadata);

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMetadata()).isNotNull();
        assertThat(captor.getValue().getMetadata().get("status")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("sendAlertToUser - metadata 포함 시 FCM 이벤트 data에 metadata JSON이 포함된다")
    void sendAlertToUser_withMetadata_fcmEventContainsMetadata() {
        Map<String, Object> metadata = Map.of("approvalTargetId", 55);

        notificationService.sendAlertToUser(
                2L,
                NotificationMessage.APPROVAL_CREATED_APPROVER,
                NotificationDomainType.APPROVAL,
                20L,
                metadata,
                "결재문서");

        ArgumentCaptor<FcmSendEvent> fcmCaptor = forClass(FcmSendEvent.class);
        verify(eventPublisher).publishEvent(fcmCaptor.capture());

        Map<String, String> fcmData = fcmCaptor.getValue().data();
        assertThat(fcmData).containsKey("metadata");
        assertThat(fcmData.get("metadata")).contains("approvalTargetId");
    }

    // -------------------------------------------------------------------------
    // messageType 검증 테스트
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("sendAlertToAdmins - 저장된 Notification의 messageType이 전달된 template과 동일하다")
    void sendAlertToAdmins_messageType_matchesTemplate() {
        User admin1 = mock(User.class);
        when(admin1.getId()).thenReturn(10L);
        when(userRepository.findAllByRole(Role.ADMIN))
                .thenReturn(new java.util.ArrayList<>(List.of(admin1)));
        when(userRepository.findAllByRole(Role.MASTER_ADMIN))
                .thenReturn(new java.util.ArrayList<>());

        notificationService.sendAlertToAdmins(
                NotificationMessage.VISIT_CHECK_IN,
                NotificationDomainType.VISIT,
                77L,
                "홍길동",
                "2026-04-09 09:00");

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.VISIT_CHECK_IN);
    }

    @Test
    @DisplayName("sendAlertToUser - 저장된 Notification의 messageType이 전달된 template과 동일하다")
    void sendAlertToUser_messageType_matchesTemplate() {
        notificationService.sendAlertToUser(
                2L,
                NotificationMessage.APPROVAL_CREATED_APPROVER,
                NotificationDomainType.APPROVAL,
                20L,
                "결재문서");

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.APPROVAL_CREATED_APPROVER);
    }

    @Test
    @DisplayName("sendNoticeAlertToTargets - 저장된 Notification의 messageType이 NOTICE_CREATED이다")
    void sendNoticeAlertToTargets_messageType_isNoticeCreated() {
        notificationService.sendNoticeAlertToTargets("공지 제목", 5L, Set.of(1L));

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.NOTICE_CREATED);
    }

    @Test
    @DisplayName(
            "sendEduReportAlertToTargets - 저장된 Notification의 messageType이 EDU_REPORT_CREATED이다")
    void sendEduReportAlertToTargets_messageType_isEduReportCreated() {
        notificationService.sendEduReportAlertToTargets("SAFETY", "교육 제목", 10L, List.of(1L));

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.EDU_REPORT_CREATED);
    }

    @Test
    @DisplayName("sendVisitAlertToDepartment - 저장된 Notification의 messageType이 전달된 template과 동일하다")
    void sendVisitAlertToDepartment_messageType_matchesTemplate() {
        when(userRepository.findAllIdsByDepartmentId(3L)).thenReturn(List.of(1L));

        notificationService.sendVisitAlertToDepartment(
                NotificationMessage.VISIT_CHECK_IN, 99L, 3L, "홍길동", "2026-04-09 09:00");

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.VISIT_CHECK_IN);
    }

    @Test
    @DisplayName(
            "sendAnnualLeaveAlertToUser - 저장된 Notification의 messageType이 ANNUAL_LEAVE_UPDATED이다")
    void sendAnnualLeaveAlertToUser_messageType_isAnnualLeaveUpdated() {
        notificationService.sendAnnualLeaveAlertToUser(1L, "2026년 01월 01일");

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.ANNUAL_LEAVE_UPDATED);
    }

    @Test
    @DisplayName("sendPayslipAlertToUser - 저장된 Notification의 messageType이 PAYSLIP_SENT이다")
    void sendPayslipAlertToUser_messageType_isPayslipSent() {
        notificationService.sendPayslipAlertToUser(1L, 50L);

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType()).isEqualTo(NotificationMessage.PAYSLIP_SENT);
    }

    @Test
    @DisplayName(
            "sendApprovalCreatedAlert - 결재자에게 저장된 Notification의 messageType이"
                    + " APPROVAL_CREATED_APPROVER이다")
    void sendApprovalCreatedAlert_approver_messageType() {
        notificationService.sendApprovalCreatedAlert(100L, "결재문서", 11L, List.of());

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.APPROVAL_CREATED_APPROVER);
    }

    @Test
    @DisplayName(
            "sendApprovalCreatedAlert - 참조자에게 저장된 Notification의 messageType이"
                    + " APPROVAL_CREATED_REFERRER이다")
    void sendApprovalCreatedAlert_referrer_messageType() {
        notificationService.sendApprovalCreatedAlert(100L, "결재문서", 11L, List.of(22L));

        // 결재자(1번) + 참조자(1번) = 총 2회 save
        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        // 두 번째 save가 참조자
        Notification referrerNotification = captor.getAllValues().get(1);
        assertThat(referrerNotification.getMessageType())
                .isEqualTo(NotificationMessage.APPROVAL_CREATED_REFERRER);
    }

    @Test
    @DisplayName(
            "sendApprovalNextStepAlert - 저장된 Notification의 messageType이"
                    + " APPROVAL_CREATED_APPROVER이다")
    void sendApprovalNextStepAlert_messageType() {
        notificationService.sendApprovalNextStepAlert(33L, 100L, "결재문서");

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.APPROVAL_CREATED_APPROVER);
    }

    @Test
    @DisplayName("sendApprovalRejectedAlert - 저장된 Notification의 messageType이 APPROVAL_REJECTED이다")
    void sendApprovalRejectedAlert_messageType() {
        notificationService.sendApprovalRejectedAlert(44L, 100L, "결재문서", "반려 사유");

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.APPROVAL_REJECTED);
    }

    @Test
    @DisplayName(
            "sendApprovalFinallyApprovedAlert - 기안자와 열람권자 모두 messageType이"
                    + " APPROVAL_FINALLY_APPROVED이다")
    void sendApprovalFinallyApprovedAlert_messageType() {
        notificationService.sendApprovalFinallyApprovedAlert(100L, "결재문서", 55L, List.of(66L, 77L));

        // 기안자(1) + 열람권자(2) = 3회 save
        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository, times(3)).save(captor.capture());

        captor.getAllValues()
                .forEach(
                        n ->
                                assertThat(n.getMessageType())
                                        .isEqualTo(NotificationMessage.APPROVAL_FINALLY_APPROVED));
    }

    @Test
    @DisplayName(
            "sendSafetyTrainingSessionAlertToAttendees - 저장된 Notification의 messageType이"
                    + " SAFETY_TRAINING_SESSION_CREATED이다")
    void sendSafetyTrainingSessionAlertToAttendees_messageType() {
        notificationService.sendSafetyTrainingSessionAlertToAttendees(
                200L, "안전보건교육 세션", List.of(1L));

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getMessageType())
                .isEqualTo(NotificationMessage.SAFETY_TRAINING_SESSION_CREATED);
    }

    // -------------------------------------------------------------------------
    // getNotifications - responseDto messageType 검증 테스트
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("반환된 NotificationResponseDto에 엔티티의 messageType이 포함된다")
        void getNotifications_responseDto_containsMessageType() {
            // given
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 10);
            Notification notification =
                    Notification.of(
                            userId,
                            NotificationMessage.VISIT_CHECK_IN.getTitle(),
                            NotificationMessage.VISIT_CHECK_IN.formatContent(
                                    "홍길동", "2026-05-13 09:00"),
                            NotificationDomainType.VISIT,
                            99L,
                            NotificationMessage.VISIT_CHECK_IN);
            Page<Notification> page = new PageImpl<>(List.of(notification));
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(page);

            // when
            Page<NotificationResponseDto> result =
                    notificationService.getNotifications(userId, false, pageable);

            // then
            NotificationResponseDto dto = result.getContent().get(0);
            assertThat(dto.getMessageType()).isEqualTo(NotificationMessage.VISIT_CHECK_IN);
        }
    }

    @Test
    @DisplayName("sendAlertToAdmins - metadata 포함 시 모든 관리자에게 metadata가 저장된다")
    void sendAlertToAdmins_withMetadata_savesMetadataForEachAdmin() {
        User admin1 = mock(User.class);
        when(admin1.getId()).thenReturn(10L);
        User masterAdmin = mock(User.class);
        when(masterAdmin.getId()).thenReturn(20L);

        when(userRepository.findAllByRole(Role.ADMIN))
                .thenReturn(new java.util.ArrayList<>(List.of(admin1)));
        when(userRepository.findAllByRole(Role.MASTER_ADMIN))
                .thenReturn(new java.util.ArrayList<>(List.of(masterAdmin)));

        Map<String, Object> metadata = Map.of("visitId", 77);

        notificationService.sendAlertToAdmins(
                NotificationMessage.VISIT_CHECK_IN,
                NotificationDomainType.VISIT,
                77L,
                metadata,
                "홍길동",
                "2026-04-09 09:00");

        ArgumentCaptor<Notification> captor = forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        captor.getAllValues().forEach(n -> assertThat(n.getMetadata()).containsKey("visitId"));
    }
}
