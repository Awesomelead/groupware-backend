package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EducationCategoryCreateRequestDto {

    @Schema(description = "카테고리명", example = "사업개요", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "카테고리명은 필수입니다.")
    private String name;

    @Schema(
            description = "카테고리 코드(고유)",
            example = "PSM_OVERVIEW",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "카테고리 코드는 필수입니다.")
    private String code;

    @Schema(description = "카테고리 유형", example = "PSM", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "카테고리 유형은 필수입니다.")
    private EducationCategoryType categoryType;

    @Schema(description = "부모 카테고리 ID (대분류면 null)", example = "1")
    private Long parentId;

    @Schema(description = "정렬 순서", example = "1")
    private Integer sortOrder;
}
