package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalTemplateCategoryCreateRequestDto {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull private Integer sortOrder;

    @NotNull private Boolean isActive;
}
