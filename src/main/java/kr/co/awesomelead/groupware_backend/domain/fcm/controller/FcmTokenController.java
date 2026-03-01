package kr.co.awesomelead.groupware_backend.domain.fcm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.domain.fcm.dto.request.FcmTokenRegisterRequestDto;
import kr.co.awesomelead.groupware_backend.domain.fcm.service.FcmTokenService;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FCM 토큰 관리", description = "FCM 토큰 등록/삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm/tokens")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    @Operation(summary = "FCM 토큰 등록/갱신", description = "로그인 후 또는 토큰 갱신 시 FCM 토큰을 등록합니다. 동일 디바이스 타입은 Upsert 처리됩니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "등록/갱신 성공"),
        @ApiResponse(responseCode = "400", description = "유효하지 않은 요청"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<Void> registerToken(
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody @Valid FcmTokenRegisterRequestDto requestDto) {
        Long userId = extractUserId(authorizationHeader);
        fcmTokenService.registerToken(userId, requestDto.getToken(), requestDto.getDeviceType());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "FCM 토큰 삭제", description = "로그아웃 시 해당 디바이스의 FCM 토큰을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "토큰을 찾을 수 없음")
    })
    @DeleteMapping("/{token}")
    public ResponseEntity<Void> deleteToken(
        @RequestHeader("Authorization") String authorizationHeader,
        @PathVariable String token) {
        Long userId = extractUserId(authorizationHeader);
        fcmTokenService.deleteToken(userId, token);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(String authorizationHeader) {
        String accessToken = authorizationHeader.replace("Bearer ", "");
        String email = jwtUtil.getUsername(accessToken);
        return userRepository
            .findByEmail(email)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
            .getId();
    }
}
