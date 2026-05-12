package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalDecisionAction;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLineStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전자결재 결재처리 응답")
public class ApprovalDecisionResponseDto {

    @Schema(description = "문서 ID", example = "101")
    private Long documentId;

    @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
    private String documentNo;

    @Schema(
            description = "문서 상태",
            example = "IN_PROGRESS",
            allowableValues = {"DRAFT", "IN_PROGRESS", "APPROVED", "REJECTED", "RECALLED"})
    private ApprovalStatus status;

    @Schema(description = "문서 상태 한글 라벨", example = "결재진행")
    private String statusLabel;

    @Schema(description = "처리된 내 결재선 정보")
    private ProcessedLineDto processedLine;

    @Schema(description = "의견 이력(승인/반려/보류 시 입력한 의견)")
    private List<OpinionDto> opinions;

    @Schema(description = "완료일시(최종 완결 시)")
    private LocalDateTime completedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "처리된 결재선 정보")
    public static class ProcessedLineDto {
        @Schema(description = "결재 라인 ID", example = "2001")
        private Long lineId;

        @Schema(
                description = "요청 액션",
                example = "APPROVE",
                allowableValues = {"APPROVE", "REJECT", "HOLD"})
        private ApprovalDecisionAction action;

        @Schema(
                description = "결재 라인 상태",
                example = "APPROVED",
                allowableValues = {"WAITING", "PENDING", "APPROVED", "REJECTED", "SKIPPED"})
        private ApprovalLineStatus lineStatus;

        @Schema(description = "결재 라인 상태 한글 라벨", example = "승인")
        private String lineStatusLabel;

        @Schema(description = "처리자 사용자 ID", example = "14")
        private Long processedByUserId;

        @Schema(description = "처리자 이름", example = "고영민")
        private String processedByUserName;

        @Schema(description = "처리 일시", example = "2026-05-12T00:13:45")
        private LocalDateTime processedAt;

        @Schema(description = "서명 표시 텍스트(예: 승인 05/12)", example = "승인 05/12")
        private String stampLabel;

        @Schema(description = "처리 시점 서명이미지 URL(없으면 null)")
        private String signatureImageUrl;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재 의견 이력")
    public static class OpinionDto {
        @Schema(
                description = "액션",
                example = "APPROVE",
                allowableValues = {"APPROVE", "REJECT", "HOLD"})
        private ApprovalDecisionAction action;

        @Schema(description = "작성자 사용자 ID", example = "14")
        private Long actorUserId;

        @Schema(description = "작성자 이름", example = "고영민")
        private String actorUserName;

        @Schema(description = "작성 시각")
        private LocalDateTime actionAt;

        @Schema(description = "결재의견 Delta(JSON 문자열, 없으면 null)")
        private String commentDelta;

        @Schema(description = "결재의견 HTML, 없으면 null")
        private String commentHtml;

        @Schema(description = "결재의견 텍스트(목록 표시용)")
        private String commentText;
    }
}
