package kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "한비로 계정 연동 요청")
public class HanbiroAccountLinkRequestDto {

    @NotBlank(message = "한비로 아이디는 필수입니다.")
    @Schema(description = "한비로 로그인 ID", example = "awesomelead.user")
    private String hanbiroId;

    @NotBlank(message = "한비로 비밀번호는 필수입니다.")
    @Schema(description = "한비로 로그인 비밀번호", example = "password123!")
    private String hanbiroPassword;
}
