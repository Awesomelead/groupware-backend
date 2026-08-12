package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalActionType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLineStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalReadRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전자결재 문서 상세 응답")
public class ApprovalDetailResponseDto {

    @Schema(description = "문서 ID", example = "101")
    private Long documentId;

    @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
    private String documentNo;

    @Schema(description = "양식 ID", example = "1")
    private Long templateId;

    @Schema(description = "양식 코드", example = "BASIC")
    private String templateCode;

    @Schema(description = "양식명", example = "기본양식")
    private String templateName;

    @Schema(description = "문서 제목", example = "1분기 출장비 정산")
    private String title;

    @Schema(description = "문서 본문 문자열")
    private String content;

    @Schema(description = "결재유형", example = "INTERNAL")
    private ApprovalType approvalType;

    @Schema(description = "결재유형 한글 라벨", example = "내부결재")
    private String approvalTypeLabel;

    @Schema(description = "문서 상태", example = "IN_PROGRESS")
    private ApprovalStatus status;

    @Schema(description = "문서 상태 한글 라벨", example = "결재진행")
    private String statusLabel;

    @Schema(description = "기안자 사용자 ID", example = "14")
    private Long drafterUserId;

    @Schema(description = "기안자 이름", example = "고영민")
    private String drafterUserName;

    @Schema(description = "기안자 부서 ID", example = "3")
    private Long drafterDepartmentId;

    @Schema(description = "기안자 부서명", example = "경영지원부")
    private String drafterDepartmentName;

    @Schema(description = "수신부서 ID(협조결재일 때)", example = "7")
    private Long receiverDepartmentId;

    @Schema(description = "수신부서명(협조결재일 때)", example = "생산본부")
    private String receiverDepartmentName;

    @Schema(description = "내 문서 여부", example = "true")
    private Boolean mine;

    @Schema(description = "상신일시")
    private LocalDateTime submittedAt;

    @Schema(description = "완료일시")
    private LocalDateTime completedAt;

    @Schema(description = "최초 생성일시")
    private LocalDateTime createdAt;

    @Schema(description = "최종 수정일시")
    private LocalDateTime modifiedAt;

    @Schema(description = "결재선/합의/참조/열람/수신부서 라인")
    private List<LineDto> lines;

    @Schema(description = "결재칸 표시 정보")
    private List<ApprovalBoxDto> approvalBoxes;

    @Schema(description = "첨부파일 목록")
    private List<AttachmentDto> attachments;

    @Schema(description = "의견/처리 이력")
    private List<ActionHistoryDto> actionHistories;

    @Schema(description = "의견글 목록")
    private List<CommentDto> comments;

    @Schema(description = "열람 정보")
    private List<ReadDto> reads;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "문서 라인 상세")
    public static class LineDto {
        private Long lineId;
        private ApprovalRouteRole role;
        private String roleLabel;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private Long targetDepartmentId;
        private String targetName;
        private Integer sequenceNo;
        private Boolean required;
        private ApprovalLineStatus lineStatus;
        private String lineStatusLabel;
        private Long processedByUserId;
        private String processedByUserName;
        private String processedComment;
        private LocalDateTime processedAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재칸 표시 정보")
    public static class ApprovalBoxDto {
        private Long lineId;
        private ApprovalRouteRole role;
        private String type;
        private String label;
        private Long userId;
        private String userName;
        private String departmentName;
        private String positionName;
        private Integer sequenceNo;
        private ApprovalLineStatus lineStatus;
        private String lineStatusLabel;
        private String signatureImageUrl;
        private LocalDate processedDate;
        private LocalDateTime processedAt;
        private String displayDate;
        private String displayText;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "문서 첨부파일")
    public static class AttachmentDto {
        private Long attachmentId;
        private String originalFileName;
        private String fileKey;
        private Long fileSize;
        private String viewUrl;
        private String downloadUrl;
        private Long uploadedByUserId;
        private String uploadedByUserName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "문서 처리 이력")
    public static class ActionHistoryDto {
        private Long historyId;
        private ApprovalActionType actionType;
        private String actionTypeLabel;
        private ApprovalStatus fromStatus;
        private String fromStatusLabel;
        private ApprovalStatus toStatus;
        private String toStatusLabel;
        private Long actorUserId;
        private String actorUserName;
        private String actionComment;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "문서 의견글")
    public static class CommentDto {
        private Long commentId;
        private Long writerUserId;
        private String writerUserName;
        private String writerDepartmentName;
        private String writerPositionName;
        private String content;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "문서 열람 정보")
    public static class ReadDto {
        private Long readId;
        private ApprovalReadRole readRole;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private Long targetDepartmentId;
        private String targetName;
        private Boolean read;
        private LocalDateTime readAt;
    }
}
