package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminEducationCategoryResponseDto {

    @Schema(description = "카테고리 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리 코드", example = "SAFETY_RESOURCE")
    private String code;

    @Schema(description = "카테고리명", example = "안전보건 자료")
    private String name;

    @Schema(description = "카테고리 유형", example = "SAFETY")
    private EducationCategoryType categoryType;

    @Schema(description = "부모 카테고리 ID", example = "10")
    private Long parentId;

    @Schema(description = "부모 카테고리명", example = "안전보건")
    private String parentName;

    @Schema(description = "카테고리 깊이", example = "1")
    private int depth;

    @Schema(description = "정렬 순서", example = "1")
    private int sortOrder;

    @Schema(description = "활성 여부", example = "false")
    private boolean active;
}
