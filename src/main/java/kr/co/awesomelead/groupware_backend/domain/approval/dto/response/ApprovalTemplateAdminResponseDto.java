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
@Schema(description = "전자결재 양식 상세 응답(관리자)")
public class ApprovalTemplateAdminResponseDto {

    @Schema(description = "양식 ID", example = "1")
    private Long id;

    @Schema(description = "양식구분 ID", example = "1")
    private Long categoryId;

    @Schema(description = "양식구분 코드", example = "COMMON")
    private String categoryCode;

    @Schema(description = "양식구분명", example = "공통양식")
    private String categoryName;

    @Schema(description = "양식 코드", example = "EXPENSE_DRAFT")
    private String code;

    @Schema(description = "양식명", example = "기안및지출결의")
    private String name;

    @Schema(description = "양식 설명", example = "기안 및 지출결의 공통 양식")
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

    @Schema(description = "기본 본문 Delta(JSON 문자열)")
    private String defaultContentDelta;

    @Schema(description = "사용여부", example = "true")
    private Boolean isActive;

    @Schema(description = "기본 결재선 목록")
    private List<LineDto> lines;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "양식 기본 결재선 항목")
    public static class LineDto {
        @Schema(description = "라인 ID", example = "101")
        private Long id;

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

        @Schema(description = "타겟 사용자 ID", example = "14")
        private Long targetUserId;

        @Schema(description = "타겟 사용자명", example = "고영민")
        private String targetUserName;

        @Schema(description = "타겟 사용자 직급", example = "사원")
        private String targetUserPosition;

        @Schema(description = "타겟 사용자 부서명", example = "경영지원부")
        private String targetUserDepartmentName;

        @Schema(description = "타겟 부서 ID", example = "3")
        private Long targetDepartmentId;

        @Schema(description = "타겟 부서명", example = "환경안전부")
        private String targetDepartmentName;

        @Schema(description = "화면 표시용 타겟명", example = "[경영지원부] 고영민 (사원)")
        private String targetName;

        @Schema(description = "결재 순서", example = "1")
        private Integer sequenceNo;

        @Schema(description = "필수 여부", example = "true")
        private Boolean required;
    }
}
