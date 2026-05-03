package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalEditorType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLinePolicy;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ApprovalTemplateUpdateRequestDto {

    @NotNull private Long categoryId;

    @NotBlank
    @Size(max = 80)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull private ApprovalEditorType editorType;

    @NotNull private ApprovalType approvalType;

    @NotNull private ApprovalLinePolicy linePolicy;

    private String defaultContentDelta;

    @NotNull private Boolean isActive;

    @Valid private List<ApprovalTemplateLineUpsertRequestDto> lines;
}
