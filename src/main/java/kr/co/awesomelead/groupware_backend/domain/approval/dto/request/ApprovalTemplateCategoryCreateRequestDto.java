package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "전자결재 양식구분 생성 요청")
public class ApprovalTemplateCategoryCreateRequestDto {

    @NotBlank
    @Size(max = 50)
    @Schema(description = "양식구분 코드(고유)", example = "COMMON", maxLength = 50)
    private String code;

    @NotBlank
    @Size(max = 100)
    @Schema(description = "양식구분명", example = "공통양식", maxLength = 100)
    private String name;

    @NotNull
    @Schema(description = "정렬순서(오름차순)", example = "1")
    private Integer sortOrder;

    @NotNull
    @Schema(description = "사용여부", example = "true")
    private Boolean isActive;
}
