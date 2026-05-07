package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "전자결재 임시저장 생성/수정 요청")
public class ApprovalDraftUpsertRequestDto {

    @Schema(description = "임시저장 문서 ID (수정 시 path의 documentId 사용, body 미입력 권장)", example = "101")
    private Long documentId;

    @NotNull
    @Schema(description = "전자결재 양식 ID", example = "1")
    private Long templateId;

    @Schema(description = "문서 제목", example = "국외출장여비정산서")
    private String title;

    @Schema(
            description =
                    "Quill Delta JSON 문자열. 예: {\"ops\":[{\"insert\":\"출장 목적: 일본 법인 미팅\\n\"}]}",
            example = "{\"ops\":[{\"insert\":\"출장 목적: 일본 법인 미팅\\n\"}]}")
    private String contentDelta;

    @Schema(description = "HTML 본문(선택)")
    private String contentHtml;

    @Schema(description = "결재유형", example = "INTERNAL", allowableValues = {"INTERNAL", "COOPERATIVE"})
    private ApprovalType approvalType;

    @Schema(description = "수신 부서 ID (approvalType=COOPERATIVE일 때 사용)", example = "5")
    private Long receiverDepartmentId;

    @Schema(description = "문서 결재선/참조자/열람권자 설정. 미입력 시 기존값(생성은 양식 기본값) 사용")
    @Valid private List<ApprovalLineRequestDto> lines;
}
