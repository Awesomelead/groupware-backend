package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "전자결재 상신 응답")
public class ApprovalSubmitResponseDto {

    @Schema(description = "문서 ID", example = "101")
    private Long documentId;

    @Schema(description = "문서번호", example = "기안및지출결의 경영지원부 20260108-30")
    private String documentNo;

    @Schema(
            description = "문서 상태",
            example = "IN_PROGRESS",
            allowableValues = {
                "DRAFT",
                "IN_PROGRESS",
                "APPROVED",
                "REJECTED",
                "RECALLED",
                "CANCELED"
            })
    private ApprovalStatus status;

    @Schema(description = "기안자 ID", example = "14")
    private Long drafterUserId;

    @Schema(description = "기안자명", example = "고영민")
    private String drafterUserName;

    @Schema(description = "제목", example = "국외출장여비정산서")
    private String title;

    @Schema(description = "결재선(참조자/열람권자 제외)")
    private List<ApprovalLineDto> approvalLines;

    @Schema(description = "기안일(상신일시)")
    private LocalDateTime draftedAt;

    @Schema(description = "상신일시(기안일과 동일)")
    private LocalDateTime submittedAt;

    @Schema(description = "완료일시(완결 시 세팅)")
    private LocalDateTime completedAt;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재선 항목")
    public static class ApprovalLineDto {
        @Schema(description = "결재 라인 ID", example = "1001")
        private Long lineId;

        @Schema(
                description = "결재 라인 역할",
                example = "APPROVAL_LINE",
                allowableValues = {
                    "APPROVAL_LINE",
                    "AGREEMENT_REQUIRED",
                    "AGREEMENT_OPTIONAL",
                    "REFERENCE",
                    "VIEWER",
                    "RECEIVER_DEPARTMENT"
                })
        private ApprovalRouteRole role;

        @Schema(description = "결재 라인 역할 한글 라벨", example = "결재선")
        private String roleLabel;

        @Schema(
                description = "결재 라인 타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "타겟 사용자 ID(targetType=USER일 때)", example = "14")
        private Long targetUserId;

        @Schema(description = "타겟 부서 ID(targetType=DEPARTMENT일 때)", example = "3")
        private Long targetDepartmentId;

        @Schema(description = "타겟 표시명", example = "[경영지원부] 고영민 (사원)")
        private String targetName;

        @Schema(description = "결재 순서", example = "1")
        private Integer sequenceNo;
    }
}
