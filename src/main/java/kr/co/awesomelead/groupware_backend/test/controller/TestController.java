package kr.co.awesomelead.groupware_backend.test.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.FindEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.SignupResponseDto;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import kr.co.awesomelead.groupware_backend.test.dto.request.BootstrapAdminPromoteRequestDto;
import kr.co.awesomelead.groupware_backend.test.dto.request.DummyUsersCreateRequestDto;
import kr.co.awesomelead.groupware_backend.test.dto.response.DummyUsersCreateResponseDto;
import kr.co.awesomelead.groupware_backend.test.service.TestService;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Profile("local")
@Tag(name = "Test", description = """
            ## 개발 중 테스트 API
            """)
public class TestController {

    private final TestService testService;

    @Operation(summary = "[테스트] 아이디 찾기 (전체 조회)", description = "성능 테스트용 - 인증 우회")
    @PostMapping("/find-email")
    public ResponseEntity<FindEmailResponseDto> testFindEmail(
            @Valid @RequestBody FindEmailRequestDto requestDto) {

        // 인증 체크 없음
        FindEmailResponseDto response =
                testService.findEmailByAll(requestDto.getName(), requestDto.getPhoneNumber());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[테스트] 아이디 찾기 (해시)", description = "성능 테스트용 - 인증 우회")
    @PostMapping("/find-email/hash")
    public ResponseEntity<FindEmailResponseDto> testFindEmailByHash(
            @Valid @RequestBody FindEmailRequestDto requestDto) {

        // 인증 체크 없음
        FindEmailResponseDto response =
                testService.findEmailByHash(requestDto.getName(), requestDto.getPhoneNumber());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[테스트] 더미 유저 생성", description = "이메일/휴대폰 인증 없이 더미 유저를 생성합니다.")
    @PostMapping("/generate-users/{count}")
    public ResponseEntity<ApiResponse<DummyUsersCreateResponseDto>> generateUsers(
            @PathVariable int count) {
        DummyUsersCreateRequestDto request = new DummyUsersCreateRequestDto();
        request.setCount(count);
        DummyUsersCreateResponseDto result = testService.createDummyUsers(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "[테스트] 더미 유저 생성(상세 옵션)", description = "이메일/휴대폰 인증 없이 더미 유저를 생성합니다.")
    @PostMapping("/users/dummy")
    public ResponseEntity<ApiResponse<DummyUsersCreateResponseDto>> createDummyUsers(
            @Valid @RequestBody DummyUsersCreateRequestDto request) {
        DummyUsersCreateResponseDto result = testService.createDummyUsers(request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "[테스트] 인증 우회 회원가입", description = "전화번호/이메일 인증 없이 테스트용 계정을 생성합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signupWithoutVerification(
            @Valid @RequestBody SignupRequestDto requestDto) {
        SignupResponseDto result = testService.signupWithoutVerification(requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "[테스트] 초기 관리자 승격",
            description = "로컬 DB 세팅용으로 가입된 사용자를 초기 관리자(ADMIN)로 승격합니다.")
    @PostMapping("/bootstrap-promote-admin")
    public ResponseEntity<ApiResponse<SignupResponseDto>> bootstrapPromoteAdmin(
            @Valid @RequestBody BootstrapAdminPromoteRequestDto requestDto) {
        SignupResponseDto result = testService.bootstrapPromoteAdmin(requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
