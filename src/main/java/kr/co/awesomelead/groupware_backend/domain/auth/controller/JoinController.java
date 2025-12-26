package kr.co.awesomelead.groupware_backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.JoinRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SendAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.VerifyAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.service.JoinService;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JoinController {

    private final JoinService joinService;
    private final PhoneAuthService phoneAuthService;

    @Operation(summary = "인증번호 발송", description = "회원가입을 위한 휴대폰 인증번호를 발송합니다.")
    @PostMapping("/join/send-code")
    public ResponseEntity<String> sendAuthCode(
        @Valid @RequestBody SendAuthCodeRequestDto requestDto) {
        phoneAuthService.sendAuthCode(requestDto.getPhoneNumber());
        return ResponseEntity.ok("인증번호가 발송되었습니다.");
    }

    @Operation(summary = "인증번호 확인", description = "발송된 인증번호를 확인합니다.")
    @PostMapping("/join/verify-code")
    public ResponseEntity<String> verifyAuthCode(
        @Valid @RequestBody VerifyAuthCodeRequestDto requestDto) {
        phoneAuthService.verifyAuthCode(requestDto.getPhoneNumber(), requestDto.getAuthCode());
        return ResponseEntity.ok("인증이 완료되었습니다.");
    }

    @Operation(summary = "회원가입", description = "회원가입을 요청합니다.")
    @PostMapping("/join")
    public ResponseEntity<String> joinProcess(@Valid @RequestBody JoinRequestDto joinDto) {
        joinService.joinProcess(joinDto);
        return ResponseEntity.ok("회원가입 요청이 성공적으로 완료되었습니다. 관리자 승인을 기다려주세요.");
    }
}
