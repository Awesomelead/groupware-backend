package kr.co.awesomelead.groupware_backend.domain.fcm.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.awesomelead.groupware_backend.domain.fcm.enums.DeviceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "FCM 토큰 등록/갱신 요청")
public class FcmTokenRegisterRequestDto {

    @Schema(description = "Firebase FCM 토큰", example = "dY3jk2...firebase-token", required = true)
    @NotBlank(message = "FCM 토큰은 필수입니다.")
    private String token;

    @Schema(description = "디바이스 유형", example = "ANDROID", required = true, implementation = DeviceType.class)
    @NotNull(message = "디바이스 유형은 필수입니다.")
    private DeviceType deviceType;
}
