package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalEditorType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLinePolicy;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ApprovalTemplateListResponseDto {

    private List<CategoryDto> categories;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CategoryDto {
        private Long id;
        private String code;
        private String name;
        private Integer sortOrder;
        private List<TemplateDto> templates;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TemplateDto {
        private Long id;
        private String code;
        private String name;
        private String description;
        private ApprovalEditorType editorType;
        private ApprovalType approvalType;
        private ApprovalLinePolicy linePolicy;
        private String defaultContentDelta;
        private List<LineDto> defaultLines;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LineDto {
        private ApprovalRouteRole role;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private Long targetDepartmentId;
        private String targetName;
        private Integer sequenceNo;
        private Boolean required;
    }
}
