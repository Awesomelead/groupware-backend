package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "전자결재 양식 목록 응답(양식구분 + 양식 + 기본 라인)")
public class ApprovalTemplateListResponseDto {

    @Schema(description = "양식구분 목록")
    private List<CategoryDto> categories;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "양식구분")
    public static class CategoryDto {
        @Schema(description = "양식구분 ID", example = "1")
        private Long id;

        @Schema(description = "양식구분 코드", example = "COMMON")
        private String code;

        @Schema(description = "양식구분명", example = "공통")
        private String name;

        @Schema(description = "정렬순서(오름차순)", example = "1")
        private Integer sortOrder;

        @Schema(description = "양식 목록")
        private List<TemplateDto> templates;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "전자결재 양식")
    public static class TemplateDto {
        @Schema(description = "양식 ID", example = "11")
        private Long id;

        @Schema(description = "양식 코드", example = "OVERSEAS_TRIP")
        private String code;

        @Schema(description = "양식명", example = "국외출장여비정산서")
        private String name;

        @Schema(description = "양식 설명", example = "국외출장 경비 정산용 양식")
        private String description;

        @Schema(
                description = "에디터 타입",
                example = "QUILL",
                allowableValues = {"QUILL", "HTML", "EXCEL"})
        private ApprovalEditorType editorType;

        @Schema(
                description = "결재유형",
                example = "INTERNAL",
                allowableValues = {"INTERNAL", "COOPERATIVE"})
        private ApprovalType approvalType;

        @Schema(
                description = "결재선 정책",
                example = "FLEXIBLE",
                allowableValues = {"FIXED", "FLEXIBLE"})
        private ApprovalLinePolicy linePolicy;

        @Schema(description = "양식 기본 본문 Delta(JSON 문자열)")
        private String defaultContentDelta;

        @Schema(description = "양식 기본 결재선/참조자/열람권자")
        private List<LineDto> defaultLines;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "양식 기본 라인 항목")
    public static class LineDto {

        @Schema(
                description = "라인 역할",
                example = "APPROVAL_LINE",
                allowableValues = {
                    "APPROVAL_LINE",
                    "AGREEMENT_REQUIRED",
                    "AGREEMENT_OPTIONAL",
                    "REFERENCE",
                    "VIEWER",
                    "RECEIVER_DEPARTMENT"
                })
        private ApprovalRouteRole role;

        @Schema(
                description = "타겟 타입",
                example = "USER",
                allowableValues = {"USER", "DEPARTMENT"})
        private ApprovalTargetType targetType;

        @Schema(description = "사용자 타겟 ID(targetType=USER일 때)", example = "14")
        private Long targetUserId;

        @Schema(description = "부서 타겟 ID(targetType=DEPARTMENT일 때)", example = "3")
        private Long targetDepartmentId;

        @Schema(description = "표시용 타겟 이름", example = "[경영지원부] 고영민 (사원)")
        private String targetName;

        @Schema(description = "결재 순서(APPROVAL_LINE에서 사용)", example = "1")
        private Integer sequenceNo;

        @Schema(description = "필수 여부", example = "true")
        private Boolean required;
    }
}
