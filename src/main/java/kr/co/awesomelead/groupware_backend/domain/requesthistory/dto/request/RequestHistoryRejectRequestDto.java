package kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;

@Getter
@Schema(description = "제증명 신청 반려 요청")
public class RequestHistoryRejectRequestDto {

    @NotBlank(message = "반려 사유는 필수입니다.")
    @Size(max = 500, message = "반려 사유는 최대 500자까지 입력할 수 있습니다.")
    @Schema(description = "반려 사유", example = "신청 정보가 인사 시스템 정보와 일치하지 않습니다.")
    private String reason;
}
