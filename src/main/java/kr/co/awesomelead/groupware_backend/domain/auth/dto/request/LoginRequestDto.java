package kr.co.awesomelead.groupware_backend.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import kr.co.awesomelead.groupware_backend.domain.fcm.enums.DeviceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "로그인 요청")
public class LoginRequestDto {

    @Schema(description = "이메일", example = "test@example.com", required = true)
    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String email;

    @Schema(description = "비밀번호", example = "test1234!", required = true)
    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @Schema(description = "FCM 토큰 (선택)", example = "dY3jk2...firebase-token")
    private String fcmToken;

    @Schema(description = "디바이스 유형 (FCM 토큰이 있을 경우 필수)", example = "ANDROID")
    private DeviceType deviceType;
}
