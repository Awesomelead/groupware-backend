package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalSavedLineType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "저장 결재선 응답")
public class SavedApprovalLineResponseDto {

    private Long id;

    @Schema(
            description = "결재선 타입",
            example = "PERSONAL",
            allowableValues = {"PERSONAL", "DEPARTMENT"})
    private ApprovalSavedLineType lineType;

    private String lineTypeLabel;
    private String lineName;

    @Schema(
            description = "결재유형",
            example = "INTERNAL",
            allowableValues = {"INTERNAL", "COOPERATIVE"})
    private ApprovalType approvalType;

    private String approvalTypeLabel;

    @Schema(description = "기본결재선 여부", example = "true")
    private Boolean isDefault;
    private Long ownerUserId;
    private String ownerUserName;
    private Long departmentId;
    private String departmentName;
    private Long createdByUserId;
    private String createdByUserName;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private List<LineDetailDto> lines;
    private ApprovalBoxPreviewDto approvalBoxPreview;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재칸 미리보기")
    public static class ApprovalBoxPreviewDto {
        @Schema(description = "결재칸에 표시될 결재선 목록 (sequenceNo 오름차순)")
        private List<ApprovalBoxSlotDto> slots;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "결재칸 단일 슬롯")
    public static class ApprovalBoxSlotDto {
        private Integer sequenceNo;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private String targetUserName;
        private String targetUserPosition;
        private String targetUserDepartmentName;
        private Long targetDepartmentId;
        private String targetDepartmentName;
        private String targetName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "저장 결재선 상세 항목")
    public static class LineDetailDto {
        private Long id;

        @Schema(
                description = "결재선 역할",
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

        @Schema(
                description = "타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        private Long targetUserId;
        private String targetUserName;
        private String targetUserPosition;
        private String targetUserDepartmentName;
        private Long targetDepartmentId;
        private String targetDepartmentName;
        private String targetName;
        private Integer sequenceNo;
        private Boolean required;
    }
}
