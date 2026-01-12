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
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(
            summary = "회원가입 승인",
            description = "대기 상태(PENDING)인 사용자의 정보를 수정/보완하여 최종 승인(AVAILABLE) 처리합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "승인 성공",
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
                      "result": null
                    }
                    """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "승인 불가 (이미 승인된 사용자 등)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                    {
                      "isSuccess": false,
                      "code": "DUPLICATED_SIGNUP_REQUEST",
                      "message": "이미 승인 처리가 완료된 사용자입니다.",
                      "result": null
                    }
                    """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "대상 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "사용자 없음",
                                                    value =
                                                            "{\"isSuccess\": false, \"code\":"
                                                                    + " \"USER_NOT_FOUND\","
                                                                    + " \"message\": \"해당 사용자를 찾을 수"
                                                                    + " 없습니다.\"}"),
                                            @ExampleObject(
                                                    name = "부서 없음",
                                                    value =
                                                            "{\"isSuccess\": false, \"code\":"
                                                                    + " \"DEPARTMENT_NOT_FOUND\","
                                                                    + " \"message\": \"해당 부서를 찾을 수"
                                                                    + " 없습니다.\"}")
                                        }))
            })
    @PatchMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<String>> approveUser(
            @Parameter(description = "승인할 사용자의 ID", example = "1", required = true)
                    @PathVariable("userId")
                    Long userId,
            @Parameter(description = "사용자 승인 정보 (부서 ID, 직급 등)", required = true) @RequestBody
                    UserApprovalRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        adminService.approveUserRegistration(userId, requestDto, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(userId + "번 사용자의 회원가입이 성공적으로 승인되었습니다."));
    }

    @Operation(
            summary = "사용자 역할(Role) 변경",
            description =
                    "특정 사용자의 역할을 변경하고, 역할에 따른 기본 권한(Authority)을 자동으로 부여/회수합니다. (ADMIN,"
                            + " MASTER_ADMIN만 호출 가능)")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "역할 변경 성공",
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
                      "result": null
                    }
                    """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "권한 부족",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "권한 업데이트 권한 없음",
                                                        value =
                                                                """
                    {
                      "isSuccess": false,
                      "code": "NO_AUTHORITY_FOR_ROLE_UPDATE",
                      "message": "사용자 역할 변경 권한이 없습니다.",
                      "result": null
                    }
                    """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 없음",
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
    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateUserRole(
            @Parameter(description = "역할을 변경할 사용자 ID", example = "5") @PathVariable Long userId,
            @Parameter(description = "변경할 역할 (USER, ADMIN 등)", example = "ADMIN") @RequestParam
                    Role role,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        adminService.updateUserRole(userId, role, userDetails.getId());
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        userId + "번 사용자의 역할이 " + role.getDescription() + "(으)로 성공적으로 변경되었습니다."));
    }
}
