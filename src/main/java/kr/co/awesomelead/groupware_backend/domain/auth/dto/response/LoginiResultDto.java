package kr.co.awesomelead.groupware_backend.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginiResultDto {

    private LoginResponseDto loginResponse;
    private String refreshToken;
}
