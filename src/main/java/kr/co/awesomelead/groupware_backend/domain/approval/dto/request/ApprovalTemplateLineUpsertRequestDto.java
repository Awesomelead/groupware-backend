package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "전자결재 양식 기본 라인 항목")
public class ApprovalTemplateLineUpsertRequestDto {

    @NotNull
    @Schema(
            description = "라인 역할",
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

    @NotNull
    @Schema(
            description = "타겟 타입(USER면 targetUserId 필수, DEPARTMENT면 targetDepartmentId 필수)",
            example = "USER",
            allowableValues = {"USER", "DEPARTMENT"})
    private ApprovalTargetType targetType;

    @Schema(description = "타겟 사용자 ID(targetType=USER일 때 필수)", example = "14")
    private Long targetUserId;

    @Schema(description = "타겟 부서 ID(targetType=DEPARTMENT일 때 필수)", example = "3")
    private Long targetDepartmentId;

    @Schema(description = "결재 순서(APPROVAL_LINE 위주, 미입력 시 자동 보정)", example = "1")
    private Integer sequenceNo;

    @NotNull
    @Schema(description = "필수 여부(합의 선택은 false 가능)", example = "true")
    private Boolean required;
}
