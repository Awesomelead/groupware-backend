package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "참조자/열람권자 대상 항목")
public class SavedApprovalLineTargetRequestDto {

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
}
