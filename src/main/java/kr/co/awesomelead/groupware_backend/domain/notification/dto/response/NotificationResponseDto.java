package kr.co.awesomelead.groupware_backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.notification.entity.Notification;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;

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

    @Schema(description = "알림 내용", example = "홍길동님의 방문 신청이 승인되었습니다.")
    private final String content;

    @Schema(description = "도메인 유형", example = "VISIT")
    private final NotificationDomainType domainType;

    @Schema(description = "도메인 PK", example = "42")
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
    }

    public static NotificationResponseDto from(Notification notification) {
        return new NotificationResponseDto(notification);
    }
}
