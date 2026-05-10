package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalEditorType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLinePolicy;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "전자결재 양식 생성 요청")
public class ApprovalTemplateCreateRequestDto {

    @NotNull
    @Schema(description = "양식구분 ID", example = "1")
    private Long categoryId;

    @NotBlank
    @Size(max = 80)
    @Schema(description = "양식 코드(고유)", example = "EXPENSE_DRAFT", maxLength = 80)
    private String code;

    @NotBlank
    @Size(max = 150)
    @Schema(description = "양식명", example = "기안및지출결의", maxLength = 150)
    private String name;

    @Size(max = 500)
    @Schema(description = "양식 설명", example = "기안 및 지출결의 공통 양식", maxLength = 500)
    private String description;

    @NotNull
    @Schema(
            description = "에디터 타입",
            example = "QUILL",
            allowableValues = {"QUILL", "HTML", "EXCEL"})
    private ApprovalEditorType editorType;

    @NotNull
    @Schema(
            description = "결재유형",
            example = "INTERNAL",
            allowableValues = {"INTERNAL", "COOPERATIVE"})
    private ApprovalType approvalType;

    @NotNull
    @Schema(
            description = "결재선 정책(FIXED=고정결재선, FLEXIBLE=가변결재선)",
            example = "FLEXIBLE",
            allowableValues = {"FIXED", "FLEXIBLE"})
    private ApprovalLinePolicy linePolicy;

    @Schema(
            description = "기본 본문 Delta(JSON 문자열). QUILL/HTML/EXCEL 공통으로 텍스트 저장 가능",
            example = "{\"ops\":[{\"insert\":\"기본양식 본문\\n\"}]}")
    private String defaultContentDelta;

    @NotNull
    @Schema(description = "사용여부", example = "true")
    private Boolean isActive;

    @Valid
    @Schema(
            description =
                    "기본 결재선 목록. linePolicy=FIXED면 최소 1개 이상의 APPROVAL_LINE 필요, FLEXIBLE이면 비워둘 수 있음")
    private List<ApprovalTemplateLineUpsertRequestDto> lines;
}
