package kr.co.awesomelead.groupware_backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.FindEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.LoginRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.LogoutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ReissueRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByPhoneRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SendAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SendEmailAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.VerifyAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.VerifyEmailAuthCodeRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.VerifyIdentityRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.AuthTokensDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.FindEmailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.IdentityVerificationResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.LoginResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.SignupResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.service.AuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.EmailAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.IdentityVerificationService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Auth",
        description =
                """
            ## 회원가입, 로그인, 로그아웃, 토큰 재발급 등 인증 관련 API

            ### 사용되는 Enum 타입
            - **Company**: 근무 사업장 (AWESOME, MARUI)
            - **Role**: 사용자 역할 (USER, ADMIN)
            - **Status**: 사용자 상태 (PENDING, AVAILABLE, SUSPENDED)
            """)
public class AuthController {

    private final PhoneAuthService phoneAuthService;
    private final EmailAuthService emailAuthService;
    private final AuthService authService;
    private final IdentityVerificationService identityVerificationService;

    @Operation(summary = "휴대폰 인증번호 발송", description = "휴대폰 인증번호를 발송합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "인증번호 발송 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                kr.co.awesomelead.groupware_backend
                                                                        .global.common.response
                                                                        .ApiResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "휴대폰 인증번호가 발송되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON400",
                                  "message": "입력값이 유효하지 않습니다.",
                                  "result": {
                                    "phoneNumber": "전화번호는 '-' 없이 10~11자리 숫자로 입력해주세요."
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "알림톡 전송 실패",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "ALIMTALK_SEND_FAILED",
                                  "message": "알림톡 전송에 실패했습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/send-phone-code")
    public ResponseEntity<ApiResponse<Void>> sendAuthCode(
            @Valid @RequestBody SendAuthCodeRequestDto requestDto) {
        phoneAuthService.sendAuthCode(requestDto.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.onNoContent("휴대폰 인증번호가 발송되었습니다."));
    }

    @Operation(summary = "휴대폰 인증번호 확인", description = "발송된 휴대폰 인증번호를 확인합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "인증 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "휴대폰 인증이 완료되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "입력값 검증 실패 또는 인증번호 불일치",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "입력값 검증 실패",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값이 유효하지 않습니다.",
                                      "result": {
                                        "authCode": "인증번호는 6자리 숫자여야 합니다."
                                      }
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "인증번호 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "AUTH_CODE_MISMATCH",
                                      "message": "인증번호가 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "인증번호 만료",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "AUTH_CODE_EXPIRED",
                                  "message": "인증번호가 만료되었습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/verify-phone-code")
    public ResponseEntity<ApiResponse<Void>> verifyAuthCode(
            @Valid @RequestBody VerifyAuthCodeRequestDto requestDto) {
        phoneAuthService.verifyAuthCode(requestDto.getPhoneNumber(), requestDto.getAuthCode());
        return ResponseEntity.ok(ApiResponse.onNoContent("휴대폰 인증이 완료되었습니다."));
    }

    @Operation(summary = "이메일 인증번호 발송", description = "이메일 인증번호를 발송합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "인증번호 발송 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "이메일 인증번호가 발송되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON400",
                                  "message": "입력값이 유효하지 않습니다.",
                                  "result": {
                                    "email": "유효한 이메일 형식이 아닙니다."
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "이메일 전송 실패",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "EMAIL_SEND_FAILED",
                                  "message": "이메일 전송에 실패했습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/send-email-code")
    public ResponseEntity<ApiResponse<Void>> sendEmailAuthCode(
            @Valid @RequestBody SendEmailAuthCodeRequestDto requestDto) {
        emailAuthService.sendAuthCode(requestDto.getEmail());
        return ResponseEntity.ok(ApiResponse.onNoContent("이메일 인증번호가 발송되었습니다."));
    }

    @Operation(summary = "이메일 인증번호 확인", description = "발송된 이메일 인증번호를 확인합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "인증 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "이메일 인증이 완료되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "입력값 검증 실패 또는 인증번호 불일치",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "입력값 검증 실패",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값이 유효하지 않습니다.",
                                      "result": {
                                        "authCode": "인증번호는 6자리 숫자여야 합니다."
                                      }
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "인증번호 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "AUTH_CODE_MISMATCH",
                                      "message": "인증번호가 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "인증번호 만료",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "AUTH_CODE_EXPIRED",
                                  "message": "인증번호가 만료되었습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/verify-email-code")
    public ResponseEntity<ApiResponse<Void>> verifyEmailAuthCode(
            @Valid @RequestBody VerifyEmailAuthCodeRequestDto requestDto) {
        emailAuthService.verifyAuthCode(requestDto.getEmail(), requestDto.getAuthCode());
        return ResponseEntity.ok(ApiResponse.onNoContent("이메일 인증이 완료되었습니다."));
    }

    @Operation(summary = "회원가입", description = "회원가입을 요청합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "회원가입 요청 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "회원가입 요청이 완료되었습니다. 관리자 승인을 기다려주세요.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "입력값 검증 실패",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "입력값 검증 실패",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값이 유효하지 않습니다.",
                                      "result": {
                                        "email": "유효한 이메일 형식이 아닙니다."
                                      }
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "비밀번호 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "PASSWORD_MISMATCH",
                                      "message": "비밀번호가 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "전화번호 인증 미완료",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "PHONE_NOT_VERIFIED",
                                      "message": "전화번호 인증이 필요합니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "이메일 인증 미완료",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "EMAIL_NOT_VERIFIED",
                                      "message": "이메일 인증이 필요합니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "409",
                        description = "중복된 정보",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "이메일 중복",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "DUPLICATE_LOGIN_ID",
                                      "message": "이미 사용 중인 아이디입니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "주민등록번호 중복",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "DUPLICATE_REGISTRATION_NUMBER",
                                      "message": "이미 가입된 주민등록번호입니다.",
                                      "result": null
                                    }
                                    """)
                                        }))
            })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto joinDto) {
        SignupResponseDto responseDto = authService.signup(joinDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "로그인", description = "로그인을 합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "로그인 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON200",
                                  "message": "요청에 성공했습니다.",
                                  "result": {
                                    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ1...",
                                    "userId": 1,
                                    "nameKor": "홍길동",
                                    "nameEng": "GILDONG HONG",
                                    "position": "대리"

                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "입력값 검증 실패",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON400",
                                  "message": "입력값이 유효하지 않습니다.",
                                  "result": {
                                    "email": "이메일을 입력해주세요."
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "로그인 실패 (인증 실패)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON401",
                                  "message": "인증에 실패했습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @RequestBody LoginRequestDto requestDto) {

        LoginResponseDto loginResponseDto = authService.login(requestDto);

        return ResponseEntity.ok(ApiResponse.onSuccess(loginResponseDto));
    }

    @Operation(
            summary = "로그아웃",
            description =
                    """
            로그아웃을 수행합니다.

            - Authorization 헤더에 Bearer Access Token이 필요합니다.
            - Request Body에 Refresh Token을 포함해야 합니다.
            - 본인 소유의 Refresh Token만 로그아웃할 수 있습니다.
            """)
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "로그아웃 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                            {
                              "isSuccess": true,
                              "code": "COMMON204",
                              "message": "로그아웃되었습니다.",
                              "result": null
                            }
                            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "유효하지 않거나 만료된 토큰",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "Invalid Token",
                                                    value =
                                                            """
                                {
                                  "isSuccess": false,
                                  "code": "INVALID_TOKEN",
                                  "message": "유효하지 않은 토큰입니다.",
                                  "result": null
                                }
                                """),
                                            @ExampleObject(
                                                    name = "Expired Token",
                                                    value =
                                                            """
                                {
                                  "isSuccess": false,
                                  "code": "EXPIRED_TOKEN",
                                  "message": "만료된 토큰입니다.",
                                  "result": null
                                }
                                """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "본인 소유의 리프레시 토큰이 아님",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                            {
                              "isSuccess": false,
                              "code": "REFRESH_TOKEN_MISMATCH",
                              "message": "해당 리프레시 토큰에 대한 권한이 없습니다.",
                              "result": null
                            }
                            """)))
            })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication, @RequestBody LogoutRequestDto requestDto) {

        String email = authentication.getName();
        authService.logout(email, requestDto.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.onNoContent("로그아웃되었습니다."));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "토큰 재발급 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON200",
                                  "message": "요청에 성공했습니다.",
                                  "result": {
                                    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "토큰 만료 또는 유효하지 않음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "토큰 만료",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "EXPIRED_TOKEN",
                                      "message": "만료된 토큰입니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "유효하지 않은 토큰",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "INVALID_TOKEN",
                                      "message": "유효하지 않은 토큰입니다.",
                                      "result": null
                                    }
                                    """)
                                        }))
            })
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AuthTokensDto>> reissue(
            @RequestBody ReissueRequestDto requestDto) {

        AuthTokensDto tokens = authService.reissue(requestDto.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.onSuccess(tokens));
    }

    @Operation(summary = "아이디 찾기", description = "해시 기반 검색하여 휴대폰 번호로 아이디를 찾습니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "아이디 찾기 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON200",
                                  "message": "요청에 성공했습니다.",
                                  "result": {
                                    "email": "ho***@example.com"
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "전화번호 인증 미완료",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "PHONE_NOT_VERIFIED",
                                  "message": "전화번호 인증이 필요합니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "USER_NOT_FOUND",
                                  "message": "해당 사용자를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/find-email")
    public ResponseEntity<ApiResponse<FindEmailResponseDto>> findEmail(
            @Valid @RequestBody FindEmailRequestDto requestDto) {

        FindEmailResponseDto response =
                authService.findEmail(requestDto.getName(), requestDto.getPhoneNumber());

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "이메일로 비밀번호 재설정", description = "이메일 인증을 완료한 후, 비밀번호 재설정합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "비밀번호 재설정 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "비밀번호가 성공적으로 재설정되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "이메일 인증 미완료 또는 비밀번호 불일치",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "이메일 인증 미완료",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "EMAIL_NOT_VERIFIED",
                                      "message": "이메일 인증이 필요합니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "비밀번호 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "PASSWORD_MISMATCH",
                                      "message": "비밀번호가 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "USER_NOT_FOUND",
                                  "message": "해당 사용자를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PatchMapping("/reset-password/email")
    public ResponseEntity<ApiResponse<Void>> resetPasswordByEmail(
            @Valid @RequestBody ResetPasswordByEmailRequestDto requestDto) {
        authService.resetPasswordByEmail(requestDto);
        return ResponseEntity.ok(ApiResponse.onNoContent("비밀번호가 성공적으로 재설정되었습니다."));
    }

    @Operation(summary = "휴대폰으로 비밀번호 재설정", description = "이메일과 휴대폰 인증을 완료한 후, 비밀번호를 재설정합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "비밀번호 재설정 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "비밀번호가 성공적으로 재설정되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "입력값 검증 실패",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값이 유효하지 않습니다.",
                                      "result": {
                                        "email": "유효한 이메일 형식이 아닙니다."
                                      }
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "전화번호 인증 미완료",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "PHONE_NOT_VERIFIED",
                                      "message": "전화번호 인증이 필요합니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "전화번호 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "PHONE_NUMBER_MISMATCH",
                                      "message": "입력한 전화번호가 계정의 전화번호와 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "비밀번호 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "PASSWORD_MISMATCH",
                                      "message": "비밀번호가 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "USER_NOT_FOUND",
                                  "message": "해당 사용자를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PatchMapping("/reset-password/phone")
    public ResponseEntity<ApiResponse<Void>> resetPasswordByPhone(
            @Valid @RequestBody ResetPasswordByPhoneRequestDto requestDto) {
        authService.resetPasswordByPhone(requestDto);
        return ResponseEntity.ok(ApiResponse.onNoContent("비밀번호가 성공적으로 재설정되었습니다."));
    }

    @Operation(summary = "로그인 후 비밀번호 재설정", description = "로그인 한 사용자가 비밀번호 재설정합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "비밀번호 변경 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON204",
                                  "message": "비밀번호가 성공적으로 변경되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "비밀번호 불일치 또는 현재 비밀번호와 동일",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "비밀번호 확인 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "PASSWORD_MISMATCH",
                                      "message": "비밀번호가 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "현재 비밀번호 불일치",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "CURRENT_PASSWORD_MISMATCH",
                                      "message": "현재 비밀번호가 일치하지 않습니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "새 비밀번호가 현재 비밀번호와 동일",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "SAME_AS_CURRENT_PASSWORD",
                                      "message": "새 비밀번호는 현재 비밀번호와 달라야 합니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "USER_NOT_FOUND",
                                  "message": "해당 사용자를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PatchMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.resetPassword(requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onNoContent("비밀번호가 성공적으로 변경되었습니다."));
    }

    @Operation(summary = "[테스트] 계정 삭제", description = "로그인한 본인 계정과 연관 데이터를 함께 삭제합니다.")
    @DeleteMapping("/user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.deleteUser(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onNoContent("계정이 성공적으로 삭제되었습니다."));
    }

    @Operation(summary = "본인인증 확인", description = "포트원 본인인증 결과를 확인하고 인증 정보를 반환합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "본인인증 확인 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON200",
                                  "message": "요청에 성공했습니다.",
                                  "result": {
                                    "identityVerificationId": "identity-1768232040185",
                                    "status": "VERIFIED",
                                    "name": "홍길동",
                                    "phoneNumber": "01012345678",
                                    "birthDate": "1990-01-01",
                                    "gender": "MALE"
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "입력값 검증 실패",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값이 유효하지 않습니다.",
                                      "result": {
                                        "identityVerificationId": "identityVerificationId는 필수입니다."
                                      }
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "본인인증 미완료",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "IDENTITY_VERIFICATION_NOT_COMPLETED",
                                      "message": "본인인증이 완료되지 않았습니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "유효하지 않은 인증 ID",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "IDENTITY_VERIFICATION_NOT_FOUND",
                                      "message": "해당 본인인증 정보를 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "500",
                        description = "서버 오류",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "IDENTITY_VERIFICATION_FAILED",
                                  "message": "본인인증 조회에 실패했습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/verify-identity")
    public ResponseEntity<ApiResponse<IdentityVerificationResponseDto>> verifyIdentity(
            @Valid @RequestBody VerifyIdentityRequestDto requestDto) {

        IdentityVerificationResponseDto response =
                identityVerificationService.verifyIdentity(requestDto.getIdentityVerificationId());

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
