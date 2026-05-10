package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateCategory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "전자결재 양식구분 응답")
public class ApprovalTemplateCategoryResponseDto {

    @Schema(description = "양식구분 ID", example = "1")
    private Long id;

    @Schema(description = "양식구분 코드", example = "COMMON")
    private String code;

    @Schema(description = "양식구분명", example = "공통양식")
    private String name;

    @Schema(description = "정렬순서", example = "1")
    private Integer sortOrder;

    @Schema(description = "사용여부", example = "true")
    private Boolean isActive;

    public static ApprovalTemplateCategoryResponseDto from(ApprovalTemplateCategory category) {
        return ApprovalTemplateCategoryResponseDto.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .build();
    }
}
