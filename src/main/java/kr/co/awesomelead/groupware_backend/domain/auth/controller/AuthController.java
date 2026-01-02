package kr.co.awesomelead.groupware_backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.Iterator;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.LoginRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SendAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SendEmailAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.VerifyAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.VerifyEmailAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.LoginResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.service.AuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.EmailAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.RefreshTokenService;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "로그인, 로그아웃 관련 API")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PhoneAuthService phoneAuthService;
    private final EmailAuthService emailAuthService;
    private final AuthService authService;

    @Operation(summary = "휴대폰 인증번호 발송", description = "휴대폰 인증번호를 발송합니다.")
    @PostMapping("/send-phone-code")
    public ResponseEntity<String> sendAuthCode(
        @Valid @RequestBody SendAuthCodeRequestDto requestDto) {
        phoneAuthService.sendAuthCode(requestDto.getPhoneNumber());
        return ResponseEntity.ok("휴대폰 인증번호가 발송되었습니다.");
    }

    @Operation(summary = "휴대폰 인증번호 확인", description = "발송된 휴대폰 인증번호를 확인합니다.")
    @PostMapping("/verify-phone-code")
    public ResponseEntity<String> verifyAuthCode(
        @Valid @RequestBody VerifyAuthCodeRequestDto requestDto) {
        phoneAuthService.verifyAuthCode(requestDto.getPhoneNumber(), requestDto.getAuthCode());
        return ResponseEntity.ok("휴대폰 인증이 완료되었습니다.");
    }

    @Operation(summary = "이메일 인증번호 발송", description = "이메일 인증번호를 발송합니다.")
    @PostMapping("/send-email-code")
    public ResponseEntity<String> sendEmailAuthCode(
        @Valid @RequestBody SendEmailAuthCodeRequestDto requestDto) {
        emailAuthService.sendAuthCode(requestDto.getEmail());
        return ResponseEntity.ok("이메일 인증번호가 발송되었습니다.");
    }

    @Operation(summary = "이메일 인증번호 확인", description = "발송된 이메일 인증번호를 확인합니다.")
    @PostMapping("/verify-email-code")
    public ResponseEntity<String> verifyEmailAuthCode(
        @Valid @RequestBody VerifyEmailAuthCodeRequestDto requestDto) {
        emailAuthService.verifyAuthCode(requestDto.getEmail(), requestDto.getAuthCode());
        return ResponseEntity.ok("이메일 인증이 완료되었습니다.");
    }

    @Operation(summary = "회원가입", description = "회원가입을 요청합니다.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequestDto joinDto) {
        authService.signup(joinDto);
        return ResponseEntity.ok("회원가입 요청이 성공적으로 완료되었습니다. 관리자 승인을 기다려주세요.");
    }

    @Operation(summary = "로그인", description = "로그인을 합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
        @RequestBody LoginRequestDto requestDto, HttpServletResponse response) {

        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                requestDto.getEmail(), requestDto.getPassword(), null);

        Authentication authentication = authenticationManager.authenticate(authToken);

        String username = authentication.getName();

        // 사용자의 역할(Role) 정보 추출
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority().replace("ROLE_", "");

        // JWTUtil을 사용하여 Access Token 생성 (유효기간 1시간으로 설정)
        String accessToken = jwtUtil.createJwt(username, role, 60 * 60 * 1000L);
        String refreshToken = refreshTokenService.createAndSaveRefreshToken(username, role);

        // 리프레쉬 토큰을 HttpOnly 쿠키에 담아 응답
        response.addCookie(createCookie("refresh", refreshToken));

        // JWT를 DTO에 담아 응답
        return ResponseEntity.ok(new LoginResponseDto(accessToken));
    }

    @Operation(summary = "로그아웃", description = "로그아웃을 합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
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

        if (refreshToken != null) {
            // DB에서 Refresh Token 삭제
            refreshTokenService.deleteRefreshToken(refreshToken);
        }

        // 클라이언트 측의 쿠키도 만료시켜서 삭제하도록 응답 설정
        Cookie cookie = new Cookie("refresh", null); // value를 null로 설정
        cookie.setMaxAge(0); // 유효기간을 0으로 만들어 즉시 만료
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.noContent().build();
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 쿠키 유효기간 24시간
        // cookie.setSecure(true); // HTTPS 통신에서만 쿠키 전송
        cookie.setPath("/"); // 모든 경로에서 쿠키 접근 가능
        cookie.setHttpOnly(true); // JavaScript가 쿠키에 접근 불가 (XSS 방지)
        return cookie;
    }
}
