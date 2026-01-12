package kr.co.awesomelead.groupware_backend.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "토큰 재발급 응답")
public class ReissueResponseDto {

    @Schema(
            description = "새로 발급된 액세스 토큰",
            example =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InRlc3RAdGVzdC5jb20iLCJyb2xlIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzM2NDA2MDAwLCJleHAiOjE3MzY0MDk2MDB9.newSignature")
    private String accessToken;
}
