package kr.co.awesomelead.groupware_backend.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.fcm.event.FcmSendEvent;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserRepository userRepository;

    @InjectMocks private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("createNotification - metadata 없이 호출 시 metadata가 null로 저장된다")
    void createNotification_withoutMetadata_savesNotification() {
        notificationService.createNotification(
                1L, "제목", "내용", NotificationDomainType.VISIT, 10L);

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

    @Test
    @DisplayName("sendAlertToAdmins - metadata 포함 시 모든 관리자에게 metadata가 저장된다")
    void sendAlertToAdmins_withMetadata_savesMetadataForEachAdmin() {
        User admin1 = mock(User.class);
        when(admin1.getId()).thenReturn(10L);
        User masterAdmin = mock(User.class);
        when(masterAdmin.getId()).thenReturn(20L);

        when(userRepository.findAllByRole(Role.ADMIN)).thenReturn(new java.util.ArrayList<>(List.of(admin1)));
        when(userRepository.findAllByRole(Role.MASTER_ADMIN)).thenReturn(new java.util.ArrayList<>(List.of(masterAdmin)));

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
