package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalDecisionAction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "전자결재 결재처리 요청(승인/반려/보류)")
public class ApprovalDecisionRequestDto {

    @NotNull
    @Schema(
            description = "결재처리 액션",
            example = "APPROVE",
            allowableValues = {"APPROVE", "REJECT", "HOLD"})
    private ApprovalDecisionAction action;

    @Schema(
            description = "결재의견 Delta(JSON 문자열, 선택)",
            example = "{\"ops\":[{\"insert\":\"확인 완료했습니다.\\n\"}]}")
    private String commentDelta;

    @Schema(description = "결재의견 HTML(선택)", example = "<p>확인 완료했습니다.</p>")
    private String commentHtml;

    @Schema(description = "결재의견 일반 텍스트(선택)", example = "확인 완료했습니다.")
    private String commentText;
}
