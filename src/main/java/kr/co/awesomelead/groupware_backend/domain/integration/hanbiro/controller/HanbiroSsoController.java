package kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.dto.request.HanbiroAccountLinkRequestDto;
import kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.dto.response.HanbiroMailRedirectResponseDto;
import kr.co.awesomelead.groupware_backend.domain.integration.hanbiro.service.HanbiroSsoService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations/hanbiro")
@RequiredArgsConstructor
@Tag(name = "Hanbiro SSO", description = "한비로 메일 자동 로그인 연동 API")
public class HanbiroSsoController {

    private final HanbiroSsoService hanbiroSsoService;

    @Operation(summary = "한비로 계정 연동/재인증", description = "사용자별 한비로 ID/PW를 검증 후 저장합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "연동 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "한비로 인증 실패(재인증 필요)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "HANBIRO_REAUTH_REQUIRED",
                                  "message": "한비로 재인증이 필요합니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/link")
    public ResponseEntity<ApiResponse<Void>> linkAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody HanbiroAccountLinkRequestDto requestDto) {
        hanbiroSsoService.linkAccount(userDetails.getId(), requestDto);
        return ResponseEntity.ok(ApiResponse.onNoContent("한비로 계정 연동이 완료되었습니다."));
    }

    @Operation(
            summary = "한비로 메일 리다이렉트 URL 발급",
            description = "로그인 사용자가 호출하면 한비로 메일 자동 로그인 URL을 반환합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "URL 발급 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "502",
                        description = "한비로 연동 실패",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "HANBIRO_ACCOUNT_NOT_LINKED",
                                  "message": "한비로 계정 연동이 필요합니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "한비로 재인증 필요",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "HANBIRO_REAUTH_REQUIRED",
                                  "message": "한비로 재인증이 필요합니다.",
                                  "result": null
                                }
                                """)))
            })
    @GetMapping("/mail-redirect")
    public ResponseEntity<ApiResponse<HanbiroMailRedirectResponseDto>> getMailRedirect(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        HanbiroMailRedirectResponseDto response =
                hanbiroSsoService.createMailRedirectUri(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
