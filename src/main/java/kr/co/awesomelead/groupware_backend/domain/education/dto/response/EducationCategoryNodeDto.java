package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class EducationCategoryNodeDto {

    @Schema(description = "카테고리 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리 코드", example = "PSM_OVERVIEW")
    private String code;

    @Schema(description = "카테고리명", example = "사업개요")
    private String name;

    @Builder.Default
    @Schema(description = "하위 카테고리 목록")
    private List<EducationCategoryNodeDto> children = new ArrayList<>();
}

