package kr.co.awesomelead.groupware_backend.domain.fcm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.fcm.dto.request.FcmTokenRegisterRequestDto;
import kr.co.awesomelead.groupware_backend.domain.fcm.service.FcmTokenService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "FCM Token",
        description =
                """
        ## FCM 토큰 관리 API

        사용자 디바이스의 FCM(Firebase Cloud Messaging) 토큰을 관리합니다. 주로 로그인 시 호출되어 푸시 알림 수신을 위한 토큰을 서버에 등록하거나 갱신합니다.

        ### 사용되는 Enum 타입
        - **DeviceType**: 디바이스 유형 (`WEB`: 웹 브라우저, `IOS`: 아이폰, `ANDROID`: 안드로이드)
        """)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm/tokens")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @Operation(
            summary = "FCM 토큰 등록/갱신",
            description = "로그인 후 또는 토큰 갱신 시 FCM 토큰을 등록합니다. 동일 디바이스 타입은 Upsert 처리됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "등록/갱신 성공",
                content =
                        @io.swagger.v3.oas.annotations.media.Content(
                                mediaType = "application/json",
                                schema =
                                        @io.swagger.v3.oas.annotations.media.Schema(
                                                implementation =
                                                        kr.co.awesomelead.groupware_backend.global
                                                                .common.response.ApiResponse.class),
                                examples =
                                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                value =
                                                        """
                    {
                      "isSuccess": true,
                      "code": "COMMON200",
                      "message": "요청에 성공했습니다.",
                      "result": null
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청",
                content =
                        @io.swagger.v3.oas.annotations.media.Content(
                                mediaType = "application/json",
                                examples =
                                        @io.swagger.v3.oas.annotations.media.ExampleObject(
                                                value =
                                                        """
                    {
                      "isSuccess": false,
                      "code": "COMMON400",
                      "message": "입력값이 유효하지 않습니다.",
                      "result": {
                        "token": "FCM 토큰은 필수입니다."
                      }
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content =
                        @io.swagger.v3.oas.annotations.media.Content(
                                mediaType = "application/json",
                                examples =
                                        @io.swagger.v3.oas.annotations.media.ExampleObject(
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
    @PostMapping
    public ResponseEntity<
                    kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse<Void>>
            registerToken(
                    @Parameter(hidden = true) @AuthenticationPrincipal
                            CustomUserDetails userDetails,
                    @RequestBody @Valid FcmTokenRegisterRequestDto requestDto) {
        fcmTokenService.registerToken(
                userDetails.getId(), requestDto.getToken(), requestDto.getDeviceType());
        return ResponseEntity.ok(
                kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse.onSuccess(
                        null));
    }
}
