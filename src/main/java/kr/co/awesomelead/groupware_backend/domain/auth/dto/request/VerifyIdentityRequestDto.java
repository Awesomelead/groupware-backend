package kr.co.awesomelead.groupware_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "본인인증 확인 요청")
public class VerifyIdentityRequestDto {

    @NotBlank(message = "identityVerificationId는 필수입니다.")
    @Schema(description = "본인인증 ID", example = "identity-verification-39ecfa97", required = true)
    private String identityVerificationId;
}
