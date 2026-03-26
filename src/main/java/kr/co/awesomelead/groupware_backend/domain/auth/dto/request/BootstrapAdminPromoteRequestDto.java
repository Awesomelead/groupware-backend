package kr.co.awesomelead.groupware_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "테스트용 초기 관리자 승격 요청")
public class BootstrapAdminPromoteRequestDto {

    @Schema(description = "승격할 사용자 이메일", example = "ko07073@gmail.com", required = true)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;
}
