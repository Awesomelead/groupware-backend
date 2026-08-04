package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalDecisionAction;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLineStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전자결재 결재처리 응답")
public class ApprovalDecisionResponseDto {

    @Schema(description = "문서 ID", example = "101")
    private Long documentId;

    @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
    private String documentNo;

    @Schema(description = "처리 액션", example = "APPROVE")
    private ApprovalDecisionAction action;

    @Schema(description = "처리 액션 한글 라벨", example = "승인")
    private String actionLabel;

    @Schema(description = "문서 상태", example = "IN_PROGRESS")
    private ApprovalStatus status;

    @Schema(description = "문서 상태 한글 라벨", example = "결재진행")
    private String statusLabel;

    @Schema(description = "처리된 결재 라인 ID", example = "1001")
    private Long lineId;

    @Schema(description = "처리된 결재 라인 상태", example = "APPROVED")
    private ApprovalLineStatus lineStatus;

    @Schema(description = "처리된 결재 라인 상태 한글 라벨", example = "승인")
    private String lineStatusLabel;

    @Schema(description = "다음 결재 라인 ID")
    private Long nextLineId;

    @Schema(description = "다음 결재 대상 표시명")
    private String nextTargetName;

    @Schema(description = "처리자 사용자 ID", example = "14")
    private Long processedByUserId;

    @Schema(description = "처리자 이름", example = "고영민")
    private String processedByUserName;

    @Schema(description = "처리 의견")
    private String comment;

    @Schema(description = "처리 일시")
    private LocalDateTime processedAt;

    @Schema(description = "완료 일시")
    private LocalDateTime completedAt;
}
