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
public class ApprovalTemplateAdminResponseDto {

    private Long id;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String code;
    private String name;
    private String description;
    private ApprovalEditorType editorType;
    private ApprovalType approvalType;
    private ApprovalLinePolicy linePolicy;
    private String defaultContentDelta;
    private Boolean isActive;
    private List<LineDto> lines;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LineDto {
        private Long id;
        private ApprovalRouteRole role;
        private ApprovalTargetType targetType;
        private Long targetUserId;
        private String targetUserName;
        private String targetUserPosition;
        private String targetUserDepartmentName;
        private Long targetDepartmentId;
        private String targetDepartmentName;
        private String targetName;
        private Integer sequenceNo;
        private Boolean required;
    }
}
