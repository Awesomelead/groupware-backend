package kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestType;

import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "제증명 발급 신청 생성 요청")
public class RequestHistoryCreateRequestDto {

    @NotNull(message = "증명서 구분은 필수입니다.")
    @Schema(description = "증명서 구분", example = "재직증명서")
    private RequestType requestType;

    @NotBlank(message = "용도는 필수입니다.")
    @Size(max = 100, message = "용도는 최대 100자까지 입력할 수 있습니다.")
    @Schema(description = "용도", example = "은행 제출용")
    private String purpose;

    @NotNull(message = "부수는 필수입니다.")
    @Min(value = 1, message = "부수는 1 이상이어야 합니다.")
    @Max(value = 100, message = "부수는 100 이하여야 합니다.")
    @Schema(description = "발급 부수", example = "1")
    private Integer copies;

    @NotNull(message = "발급 희망일은 필수입니다.")
    @Schema(description = "발급 희망일", example = "2026-03-10")
    private LocalDate wishDate;
}
