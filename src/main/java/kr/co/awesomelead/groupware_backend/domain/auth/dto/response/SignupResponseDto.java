package kr.co.awesomelead.groupware_backend.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "회원가입 응답")
public class SignupResponseDto {

    @Schema(description = "생성된 사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;
}
