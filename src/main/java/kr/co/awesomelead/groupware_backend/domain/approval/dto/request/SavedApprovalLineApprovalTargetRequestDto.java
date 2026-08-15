package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalAgreementMethod;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "결재선 대상 항목")
public class SavedApprovalLineApprovalTargetRequestDto {

    @NotNull
    @Schema(
            description = "타겟 타입 (USER면 targetUserId 필수, DEPARTMENT면 targetDepartmentId 필수)",
            example = "USER",
            allowableValues = {"USER", "DEPARTMENT"})
    private ApprovalTargetType targetType;

    @Schema(description = "타겟 사용자 ID (targetType=USER일 때 필수)", example = "14")
    private Long targetUserId;

    @Schema(description = "타겟 부서 ID (targetType=DEPARTMENT일 때 필수)", example = "3")
    private Long targetDepartmentId;

    @Schema(description = "결재 순서 (미입력 시 1부터 자동 부여)", example = "1")
    private Integer sequenceNo;

    @NotNull
    @Schema(
            description = "해당 결재 대상의 결재방법",
            example = "APPROVAL_LINE",
            allowableValues = {"APPROVAL_LINE", "AGREEMENT_REQUIRED", "AGREEMENT_OPTIONAL"})
    private ApprovalRouteRole approvalLineRole;

    @Schema(
            description = "합의부서 내 합의방법. approvalLineRole이 AGREEMENT_REQUIRED/AGREEMENT_OPTIONAL일 때 사용",
            example = "SEQUENTIAL",
            allowableValues = {"SEQUENTIAL", "PARALLEL"})
    private ApprovalAgreementMethod agreementMethod;

    @Schema(description = "필수 여부 (미입력 시 결재방법 기준 자동 처리)", example = "true")
    private Boolean required;
}
