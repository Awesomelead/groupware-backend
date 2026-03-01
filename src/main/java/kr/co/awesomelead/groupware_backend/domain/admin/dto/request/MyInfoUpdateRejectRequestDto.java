package kr.co.awesomelead.groupware_backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "내 정보 수정 요청 반려 DTO")
public class MyInfoUpdateRejectRequestDto {

    @NotBlank(message = "반려 사유는 필수입니다.")
    @Size(max = 255, message = "반려 사유는 최대 255자까지 입력 가능합니다.")
    @Schema(description = "반려 사유", example = "전화번호 증빙이 확인되지 않았습니다.")
    private String reason;
}
