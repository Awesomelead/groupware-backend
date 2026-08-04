package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalDecisionAction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "전자결재 결재처리 요청")
public class ApprovalDecisionRequestDto {

    @NotNull(message = "처리 액션은 필수입니다.")
    @Schema(description = "처리 액션", example = "APPROVE", allowableValues = {"APPROVE", "REJECT", "HOLD"})
    private ApprovalDecisionAction action;

    @Size(max = 1000, message = "결재 의견은 1000자 이하로 입력해주세요.")
    @Schema(description = "결재 의견", example = "확인했습니다.")
    private String comment;
}
