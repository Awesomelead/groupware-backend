package kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "한비로 메일 자동 로그인 리다이렉트 응답")
public class HanbiroMailRedirectResponseDto {

    @Schema(description = "한비로 메일로 즉시 이동 가능한 URL")
    private String redirectUri;
}
