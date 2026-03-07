package kr.co.awesomelead.groupware_backend.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import kr.co.awesomelead.groupware_backend.domain.notification.dto.response.NotificationResponseDto;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Notification", description = """
        ## 알림 관리 API

        시스템에서 발생하는 각종 알림(결재 요청, 공지사항 등)을 사용자에게 제공합니다. 알림 목록 조회, 미읽음 건수 조회, 개별 알림 읽음 처리 기능이 포함되어 있습니다.

        ### 사용되는 Enum 타입
        - **NotificationDomainType**: 알림 관련 도메인 유형 (`VISIT`: 방문, `APPROVAL`: 전자결재, `NOTICE`: 공지사항, `ANNUAL_LEAVE`: 연차휴가, `GENERAL`: 일반알림, `AUTH`: 계정관련, `EDUCATION`: 교육, `PAYSLIP`: 급여명세서, `REQUEST_HISTORY`: 신청내역, `MY_INFO_UPDATE`: 내정보수정, `CHECK_SHEET`: 체크시트)
        """)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JWTUtil jwtUtil;

    @Operation(summary = "알림 목록 조회", description = "로그인한 유저의 알림 목록을 페이지로 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponseDto>>> getNotifications(
            @RequestHeader("Authorization") String authorizationHeader,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = extractUserId(authorizationHeader);
        return ResponseEntity.ok(ApiResponse.onSuccess(notificationService.getNotifications(userId, pageable)));
    }

    @Operation(summary = "미읽음 알림 수 조회", description = "읽지 않은 알림 건수를 반환합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class)))
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @RequestHeader("Authorization") String authorizationHeader) {
        Long userId = extractUserId(authorizationHeader);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(Map.of("unreadCount", count)));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "본인 알림이 아님", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                    {
                      "isSuccess": false,
                      "code": "NOT_OWNER_OF_NOTIFICATION",
                      "message": "본인의 알림만 읽음 처리할 수 있습니다.",
                      "result": null
                    }
                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                    {
                      "isSuccess": false,
                      "code": "NOTIFICATION_NOT_FOUND",
                      "message": "해당 알림을 찾을 수 없습니다.",
                      "result": null
                    }
                    """)))
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @RequestHeader("Authorization") String authorizationHeader, @PathVariable Long id) {
        Long userId = extractUserId(authorizationHeader);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
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
