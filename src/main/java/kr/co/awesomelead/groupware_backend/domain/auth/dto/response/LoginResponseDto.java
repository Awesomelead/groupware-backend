package kr.co.awesomelead.groupware_backend.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoAuthorityItemDto;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "로그인 응답")
public class LoginResponseDto {

        @Schema(description = "액세스 토큰 (Authorization 헤더에 'Bearer {token}' 형식으로 사용)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InRlc3RAdGVzdC5jb20iLCJyb2xlIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzM2NDA2MDAwLCJleHAiOjE3MzY0MDk2MDB9.signature")
        private String accessToken;

        @Schema(description = "리프레쉬 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6InRlc3RAdGVzdC5jb20iLCJyb2xlIjoiUk9MRV9VU0VSIiwiaWF0IjoxNzM2NDA2MDAwLCJleHAiOjE3MzY0MDk2MDB9.signature")
        private String refreshToken;

        @Schema(description = "생성된 사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "한글 이름", example = "홍길동")
        private String nameKor;

        @Schema(description = "영어 이름", example = "HONG GILDONG")
        private String nameEng;

        @Schema(description = "직급", example = "대리")
        private Position position;

        @Schema(description = "역할", example = "관리자")
        private Role role;

        @Schema(description = "보유 권한 목록")
        private List<MyInfoAuthorityItemDto> authorities;
}
