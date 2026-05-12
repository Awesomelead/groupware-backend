package kr.co.awesomelead.groupware_backend.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.awesomelead.groupware_backend.domain.notification.entity.Notification;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

@DisplayName("Notification 엔티티 구조 변경 테스트")
class NotificationEntityTest {

    @Nested
    @DisplayName("messageType 필드 null 허용 (기존 of() 호환성)")
    class MessageTypeNullability {

        @Test
        @DisplayName("of_5param_messageTypeIsNull: 5-param of() 호출 시 messageType이 null이다")
        void of_5param_messageTypeIsNull() {
            // given
            Long userId = 1L;
            String title = "제목";
            String content = "내용";
            NotificationDomainType domainType = NotificationDomainType.GENERAL;
            Long domainId = 10L;

            // when
            Notification notification =
                    Notification.of(userId, title, content, domainType, domainId);

            // then
            assertThat(notification.getMessageType()).isNull();
        }

        @Test
        @DisplayName("of_6param_messageTypeIsNull: 6-param of() 호출 시 messageType이 null이다")
        void of_6param_messageTypeIsNull() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");

            // when
            Notification notification =
                    Notification.of(1L, "제목", "내용", NotificationDomainType.GENERAL, 10L, metadata);

            // then
            assertThat(notification.getMessageType()).isNull();
        }

        @Test
        @DisplayName("of_7param_messageTypeIsNull: 7-param of() 호출 시 messageType이 null이다")
        void of_7param_messageTypeIsNull() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");

            // when
            Notification notification =
                    Notification.of(
                            1L, "제목", "내용", NotificationDomainType.GENERAL, 10L, metadata, true);

            // then
            assertThat(notification.getMessageType()).isNull();
        }
    }

    @Nested
    @DisplayName("messageType 포함 새 of() 팩토리 메서드")
    class MessageTypeFactory {

        @Test
        @DisplayName(
                "of_withMessageType_5param_storesMessageType: messageType 포함 5-param of() 호출 시 값이"
                    + " 저장된다")
        void of_withMessageType_5param_storesMessageType() {
            // given
            NotificationMessage messageType = NotificationMessage.NOTICE_CREATED;

            // when
            Notification notification =
                    Notification.of(
                            1L, "제목", "내용", NotificationDomainType.NOTICE, 10L, messageType);
            assertThat(notification.getMessageType()).isEqualTo(NotificationMessage.NOTICE_CREATED);
        }

        @Test
        @DisplayName(
                "of_withMessageType_6param_storesMessageType: messageType 포함 6-param of() 호출 시 값이"
                    + " 저장된다")
        void of_withMessageType_6param_storesMessageType() {
            // given
            NotificationMessage messageType = NotificationMessage.APPROVAL_CREATED_APPROVER;
            Map<String, Object> metadata = Map.of("approvalId", 42);

            // when
            Notification notification =
                    Notification.of(
                            1L,
                            "제목",
                            "내용",
                            NotificationDomainType.APPROVAL,
                            10L,
                            metadata,
                            messageType);
            assertThat(notification.getMessageType())
                    .isEqualTo(NotificationMessage.APPROVAL_CREATED_APPROVER);
        }

        @Test
        @DisplayName(
                "of_withMessageType_7param_storesMessageType: messageType 포함 7-param of() 호출 시 값이"
                    + " 저장된다")
        void of_withMessageType_7param_storesMessageType() {
            // given
            NotificationMessage messageType = NotificationMessage.VISIT_LONG_TERM_PRE;
            Map<String, Object> metadata = Map.of("visitId", 99);

            // when
            Notification notification =
                    Notification.of(
                            1L,
                            "제목",
                            "내용",
                            NotificationDomainType.VISIT,
                            10L,
                            metadata,
                            true,
                            messageType);
            assertThat(notification.getMessageType())
                    .isEqualTo(NotificationMessage.VISIT_LONG_TERM_PRE);
        }

        @Test
        @DisplayName("of_withMessageType_null_storesNull: messageType에 null 전달 시 null로 저장된다")
        void of_withMessageType_null_storesNull() {
            // given / when
            Notification notification =
                    Notification.of(
                            1L,
                            "제목",
                            "내용",
                            NotificationDomainType.GENERAL,
                            10L,
                            null,
                            (NotificationMessage) null);
            assertThat(notification.getMessageType()).isNull();
        }
    }
}
