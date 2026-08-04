package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "전자결재 의견글 등록 요청")
public class ApprovalCommentCreateRequestDto {

    @NotBlank(message = "의견 내용을 입력해주세요.")
    @Size(max = 1000, message = "의견은 1000자 이하로 입력해주세요.")
    @Schema(description = "의견 내용", example = "확인했습니다.")
    private String content;
}
