package kr.co.awesomelead.groupware_backend.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponseDto {

    @Schema(
        description = "액세스 토큰 (Authorization 헤더에 'Bearer {token}' 형식으로 사용)",
        example =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InRlc3RAdGVzdC5jb20iLCJyb2xlIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzM2NDA2MDAwLCJleHAiOjE3MzY0MDk2MDB9.signature")
    private String accessToken;

    @Schema(description = "생성된 사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "한글 이름", example = "홍길동")
    private String nameKor;

    @Schema(description = "영어 이름", example = "HONG GILDONG")
    private String nameEng;

    @Schema(description = "직급", example = "대리")
    private Position position;
}
