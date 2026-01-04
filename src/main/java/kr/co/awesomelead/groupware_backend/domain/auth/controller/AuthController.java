package kr.co.awesomelead.groupware_backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.FindEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.LoginRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByPhoneRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SendAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SendEmailAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.VerifyAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.VerifyEmailAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.AuthTokensDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.LoginResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.ReissueResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.service.AuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.EmailAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.util.CookieUtil;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입, 로그인, 로그아웃, 토큰 재발급 등 인증 관련 API")
public class AuthController {

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

        AuthTokensDto tokens = authService.login(requestDto);

        response.addCookie(CookieUtil.createRefreshTokenCookie(tokens.getRefreshToken()));

        return ResponseEntity.ok(new LoginResponseDto(tokens.getAccessToken()));
    }

    @Operation(summary = "로그아웃", description = "로그아웃을 합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = CookieUtil.getCookieValue(request, "refresh");

        // 2. DB에서 토큰 삭제 (Service 호출)
        authService.logout(refreshToken);

        // 3. 클라이언트 쿠키 만료 처리
        response.addCookie(CookieUtil.createExpiredCookie("refresh"));

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponseDto> reissue(
            HttpServletRequest request, HttpServletResponse response) {

        // 1. 쿠키에서 Refresh Token 추출
        String refreshToken = CookieUtil.getCookieValue(request, "refresh");

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 토큰 재발급 (Service)
        AuthTokensDto tokens = authService.reissue(refreshToken);

        // 3. 새로운 Refresh Token을 쿠키에 저장
        response.addCookie(CookieUtil.createRefreshTokenCookie(tokens.getRefreshToken()));

        // 4. 새로운 Access Token 응답
        return ResponseEntity.ok(new ReissueResponseDto(tokens.getAccessToken()));
    }

    @Operation(summary = "아이디 찾기", description = "해시 기반 검색하여 휴대폰 번호로 아이디를 찾습니다.")
    @PostMapping("/find-email")
    public ResponseEntity<FindEmailResponseDto> findEmail(
            @Valid @RequestBody FindEmailRequestDto requestDto) {

        FindEmailResponseDto response =
                authService.findEmail(requestDto.getName(), requestDto.getPhoneNumber());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일로 비밀번호 재설정", description = "이메일 인증을 완료한 후, 비밀번호 재설정합니다.")
    @PatchMapping("/reset-password/email")
    public ResponseEntity<String> resetPasswordByEmail(
            @Valid @RequestBody ResetPasswordByEmailRequestDto requestDto) {
        authService.resetPasswordByEmail(requestDto);
        return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다.");
    }

    @Operation(summary = "휴대폰으로 비밀번호 재설정", description = "휴대폰 인증을 완료한 후, 비밀번호 재설정합니다.")
    @PatchMapping("/reset-password/phone")
    public ResponseEntity<String> resetPasswordByPhone(
            @Valid @RequestBody ResetPasswordByPhoneRequestDto requestDto) {
        authService.resetPasswordByPhone(requestDto);
        return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다.");
    }

    @Operation(summary = "로그인 후 비밀번호 재설정", description = "로그인 한 사용자가 비밀번호 재설정합니다.")
    @PatchMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.resetPassword(requestDto, userDetails.getId());
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }
}
