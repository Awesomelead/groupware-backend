package kr.co.awesomelead.groupware_backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyAuthCodeRequestDto {

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 '-' 없이 10~11자리 숫자로 입력해주세요.")
    private String phoneNumber;

    @NotBlank(message = "인증번호는 필수입니다.")
    @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
    private String authCode;
}
