package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EducationCategoryUpdateRequestDto {

    @Schema(description = "카테고리명", example = "사업개요(개정)")
    @NotBlank(message = "카테고리명은 필수입니다.")
    private String name;

    @Schema(description = "카테고리 코드(고유)", example = "PSM_OVERVIEW")
    @NotBlank(message = "카테고리 코드는 필수입니다.")
    private String code;

    @Schema(description = "부모 카테고리 ID (대분류면 null)", example = "1")
    private Long parentId;

    @Schema(description = "정렬 순서", example = "2")
    private Integer sortOrder;
}

