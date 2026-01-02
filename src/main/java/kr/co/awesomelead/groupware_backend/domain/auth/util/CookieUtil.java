package kr.co.awesomelead.groupware_backend.domain.auth.util;

import jakarta.servlet.http.Cookie;

import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    // HttpOnly 쿠키 생성
    public static Cookie createHttpOnlyCookie(String key, String value, int maxAge) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // 배포 시 HTTPS 활성화
        return cookie;
    }

    // Refresh Token용 쿠키 생성 (24시간)
    public static Cookie createRefreshTokenCookie(String value) {
        return createHttpOnlyCookie("refresh", value, 24 * 60 * 60);
    }

    // 쿠키 삭제용 (만료된 쿠키 생성)
    public static Cookie createExpiredCookie(String key) {
        return createHttpOnlyCookie(key, null, 0);
    }

    // HttpServletRequest에서 특정 쿠키 추출
    public static String getCookieValue(
            jakarta.servlet.http.HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
