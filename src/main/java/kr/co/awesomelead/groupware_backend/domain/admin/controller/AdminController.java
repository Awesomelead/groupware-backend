package kr.co.awesomelead.groupware_backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(
    name = "Admin",
    description =
        """
            ## 관리자 전용 API
                        
            시스템 관리자 권한(`ROLE_ADMIN`)이 필요한 관리 기능을 제공합니다.
                        
            ### 주요 기능
            - **회원가입 승인**: 신규 가입 요청을 검토하고 부서 및 직급을 부여하여 최종 승인합니다.
            """)
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "회원가입 승인", description = "관리자가 회원가입 요청에 대해 승인합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "승인 완료",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                        value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "1번 사용자의 회원가입이 성공적으로 승인되었습니다.",
                              "result": null
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (이미 승인된 사용자이거나 데이터 오류)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "isSuccess": false,
                              "code": "DUPLICATED_SIGNUP_REQUEST",
                              "message": "이미 처리된 가입 요청입니다.",
                              "result": null
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음 (관리자만 가능)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "isSuccess": false,
                              "code": "COMMON403",
                              "message": "회원가입 승인 권한이 없습니다.",
                              "result": null
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "사용자 또는 부서를 찾을 수 없음",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "isSuccess": false,
                              "code": "USER_NOT_FOUND",
                              "message": "해당 사용자를 찾을 수 없습니다.",
                              "result": null
                            }
                            """
                    )
                )
            )
        }
    )
    @PatchMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveUser(
        @Parameter(description = "승인할 사용자의 ID", example = "1", required = true)
        @PathVariable("userId") Long userId,

        @Parameter(description = "사용자 승인 정보 (부서 ID, 직급 등)", required = true)
        @RequestBody UserApprovalRequestDto requestDto,

        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        adminService.approveUserRegistration(userId, requestDto, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onNoContent(userId + "번 사용자의 회원가입이 성공적으로 승인되었습니다."));
    }
}
