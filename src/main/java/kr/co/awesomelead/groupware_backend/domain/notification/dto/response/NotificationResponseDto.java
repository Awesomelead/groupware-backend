package kr.co.awesomelead.groupware_backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.notification.entity.Notification;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Schema(description = "알림 응답")
public class NotificationResponseDto {

    @Schema(description = "알림 ID", example = "1")
    private final Long id;

    @Schema(description = "알림 제목", example = "방문 신청이 승인되었습니다.")
    private final String title;

    @Schema(description = "알림 내용", example = "[이승민] 고객이 03:43:05에 입실했습니다.")
    private final String content;

    @Schema(description = "도메인 유형", example = "VISIT")
    private final NotificationDomainType domainType;

    @Schema(description = "도메인 PK (내 정보 수정 승인 요청 알림은 요청 사용자 userId)", example = "42")
    private final Long domainId;

    @Schema(description = "읽음 여부", example = "false")
    private final Boolean isRead;

    @Schema(description = "생성일시", example = "2026-02-25T01:00:00")
    private final LocalDateTime createdAt;

    @Schema(
            description = "도메인별 메타데이터 (requestId, approvalTargetId 등)",
            example = "{\"requestId\": 10}")
    private final Map<String, Object> metadata;

    @Schema(description = "승인 대기 여부 (true인 알림만 필터링 가능)", example = "false")
    private final boolean requiresApproval;

    @Schema(description = "알림 메시지 유형", example = "VISIT_CHECK_IN")
    private final NotificationMessage messageType;

    @Schema(
            description = "승인/반려 완료 알림 여부 (예: 내정보수정 승인·반려 완료, 장기방문 승인요청 처리 완료)",
            example = "false")
    private final boolean approvalOrRejectionCompleted;

    private NotificationResponseDto(Notification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.content = notification.getContent();
        this.domainType = notification.getDomainType();
        this.domainId = notification.getDomainId();
        this.isRead = notification.getIsRead();
        this.createdAt = notification.getCreatedAt();
        this.metadata = notification.getMetadata();
        this.requiresApproval = notification.isRequiresApproval();
        this.messageType = notification.getMessageType();
        this.approvalOrRejectionCompleted = isApprovalOrRejectionCompleted(notification);
    }

    public static NotificationResponseDto from(Notification notification) {
        return new NotificationResponseDto(notification);
    }

    private static boolean isApprovalOrRejectionCompleted(Notification notification) {
        NotificationMessage messageType = notification.getMessageType();
        if (messageType == null) {
            return false;
        }

        if (messageType == NotificationMessage.MY_INFO_UPDATE_APPROVED
                || messageType == NotificationMessage.MY_INFO_UPDATE_REJECTED) {
            return true;
        }

        // 장기 방문 승인요청 알림은 requiresApproval 해제 시점(승인/반려 처리 완료)을 완료로 간주
        if (messageType == NotificationMessage.VISIT_LONG_TERM_PRE) {
            return !notification.isRequiresApproval();
        }

        return false;
    }
}
