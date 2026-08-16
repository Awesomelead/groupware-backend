package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "참조자/열람권자 대상 항목")
public class SavedApprovalLineTargetRequestDto {

    @NotNull
    @Schema(description = "타겟 사용자 ID", example = "14")
    private Long targetUserId;
}
