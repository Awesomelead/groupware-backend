package kr.co.awesomelead.groupware_backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "회원가입 승인", description = "관리자가 회원가입 요청에 대해 승인합니다.")
    @PatchMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveUser(
        @PathVariable("userId") Long userId, @RequestBody UserApprovalRequestDto requestDto) {

        adminService.approveUserRegistration(userId, requestDto);

        return ResponseEntity.ok(
            ApiResponse.onNoContent(userId + "번 사용자의 회원가입이 성공적으로 승인되었습니다.")
        );
    }
}
