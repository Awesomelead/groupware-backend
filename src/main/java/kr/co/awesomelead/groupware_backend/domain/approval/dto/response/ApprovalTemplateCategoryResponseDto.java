package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateCategory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApprovalTemplateCategoryResponseDto {

    private Long id;
    private String code;
    private String name;
    private Integer sortOrder;
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
