package kr.co.awesomelead.groupware_backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.ReissueResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.entity.RefreshToken;
import kr.co.awesomelead.groupware_backend.domain.auth.service.RefreshTokenService;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReissueController {

    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "토큰 재발급", description = "리프래쉬 토큰을 통해 재발급을 요청합니다.")
    @PostMapping("/api/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {
        // 1. 요청의 쿠키에서 리프레쉬 토큰 추출
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return new ResponseEntity<>("Refresh token is required.", HttpStatus.UNAUTHORIZED);
        }

        try {
            // 2. RefreshTokenService를 통해 DB에서 토큰 검증
            RefreshToken storedToken = refreshTokenService.validateRefreshToken(refreshToken);

            // 3. 토큰에서 사용자 정보 추출
            String username = jwtUtil.getUsername(storedToken.getTokenValue());
            String role = jwtUtil.getRole(storedToken.getTokenValue());

            // 4. 새로운 Access Token 생성
            String newAccessToken = jwtUtil.createJwt(username, role, 60 * 60 * 1000L); // 1시간 유효

            // 5. 새로운 Refresh Token 생성 및 DB 업데이트
            String newRefreshToken = refreshTokenService.createAndSaveRefreshToken(username, role);

            // 6. 응답 설정
            response.addCookie(createCookie("refresh", newRefreshToken));
            return ResponseEntity.ok(new ReissueResponseDto(newAccessToken));

        } catch (IllegalArgumentException e) {
            // validateRefreshToken에서 발생한 예외 처리
            return new ResponseEntity<>(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(7 * 24 * 60 * 60); // 쿠키 유효기간 7일
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // 배포 시 HTTPS 환경에서 주석 해제
        return cookie;
    }
}
