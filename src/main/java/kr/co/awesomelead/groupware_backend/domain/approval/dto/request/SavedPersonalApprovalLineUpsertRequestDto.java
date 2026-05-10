package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "개인 결재선 생성/수정 요청")
public class SavedPersonalApprovalLineUpsertRequestDto {

    @NotBlank
    @Size(max = 150)
    @Schema(description = "결재선 이름", example = "내 자주 쓰는 결재선", maxLength = 150)
    private String lineName;

    @NotNull
    @Schema(
            description = "결재유형",
            example = "INTERNAL",
            allowableValues = {"INTERNAL", "COOPERATIVE"})
    private ApprovalType approvalType;

    @Schema(description = "기본결재선 지정 여부", example = "true")
    private Boolean isDefault;

    @Valid
    @Schema(description = "결재선 상세 목록")
    private List<SavedApprovalLineDetailRequestDto> lines;
}
