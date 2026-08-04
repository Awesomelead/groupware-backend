package kr.co.awesomelead.groupware_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import kr.co.awesomelead.groupware_backend.domain.aligo.enums.PhoneAuthChannel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "휴대폰 인증번호 발송 요청")
public class SendAuthCodeRequestDto {

    @Schema(description = "전화번호 ('-' 없이 10~11자리)", example = "01012345678", required = true)
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 '-' 없이 10~11자리 숫자로 입력해주세요.")
    private String phoneNumber;

    @Schema(description = "인증번호 발송 방식 (KAKAO: 카카오톡, SMS: 문자)", example = "KAKAO")
    private PhoneAuthChannel channel = PhoneAuthChannel.KAKAO;

    @JsonIgnore
    public PhoneAuthChannel getChannelOrDefault() {
        return channel != null ? channel : PhoneAuthChannel.KAKAO;
    }
}
