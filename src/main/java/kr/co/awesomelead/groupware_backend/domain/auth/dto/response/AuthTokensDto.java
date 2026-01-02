package kr.co.awesomelead.groupware_backend.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokensDto {

    private String accessToken;
    private String refreshToken;
}
