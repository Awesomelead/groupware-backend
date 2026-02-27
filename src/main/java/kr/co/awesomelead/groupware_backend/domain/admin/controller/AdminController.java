package kr.co.awesomelead.groupware_backend.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.MyInfoUpdateRejectRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.MyInfoUpdateRequestSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.PendingUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.enums.AuthorityAction;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
            
            시스템 관리 권한이 필요한 기능을 제공합니다. 주로 사용자 승인 및 권한 관리를 담당합니다.
            
            ### 권한 안내
            - **회원가입 승인**: `ROLE_ADMIN` 이상의 권한이 필요합니다.
            - **역할 변경**: `ROLE_ADMIN` 또는 `ROLE_MASTER_ADMIN` 권한이 필요합니다.
            """)
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "직원 목록 조회", description = "직원 관리 화면용 사용자 목록을 조회합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
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
                                  "result": [
                                    {
                                      "userId": 17,
                                      "nameKor": "홍길동",
                                      "position": "사원",
                                      "jobType": "관리직",
                                      "departmentName": "경영지원부",
                                      "signupStatus": "AVAILABLE",
                                      "hasPendingMyInfoRequest": true
                                    }
                                  ]
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "NO_AUTHORITY_FOR_REGISTRATION",
                                  "message": "회원가입 승인 권한이 없습니다.",
                                  "result": null
                                }
                                """)))
        })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryResponseDto>>> getUsers(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(description = "이름/이메일 검색어", required = false, example = "홍길동")
        @RequestParam(required = false)
        String keyword,
        @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {

        Page<AdminUserSummaryResponseDto> result =
            adminService.getUsers(userDetails.getId(), keyword, pageable);

        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "직원 상세 조회", description = "직원 관리 화면용 사용자 상세 정보를 조회합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
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
                                    "userId": 17,
                                    "nameKor": "홍길동",
                                    "nameEng": "HONG GILDONG",
                                    "birthDate": "2000-01-01",
                                    "nationality": "대한민국",
                                    "zipcode": "06234",
                                    "address1": "서울특별시 강남구 테헤란로 123",
                                    "address2": "어썸리드빌딩 5층",
                                    "registrationNumber": "0001013123456",
                                    "phoneNumber": "01012345678",
                                    "email": "hg@gmail.com",
                                    "workLocation": "어썸리드",
                                    "departmentId": 11,
                                    "departmentName": "경영지원부",
                                    "position": "사원",
                                    "jobType": "관리직",
                                    "authorities": [
                                      { "code": "ACCESS_MESSAGE", "label": "메세지 작성", "enabled": true },
                                      { "code": "ACCESS_EDUCATION", "label": "교육 작성", "enabled": false }
                                    ],
                                    "hireDate": "2025-09-22",
                                    "resignationDate": null,
                                    "role": "일반 사용자",
                                    "signupStatus": "AVAILABLE",
                                    "hasPendingMyInfoRequest": false
                                  }
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "NO_AUTHORITY_FOR_REGISTRATION",
                                  "message": "회원가입 승인 권한이 없습니다.",
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
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponseDto>> getUserDetail(
        @Parameter(description = "상세 조회할 사용자 ID", required = true, example = "17")
        @PathVariable
        Long userId,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminUserDetailResponseDto result =
            adminService.getUserDetail(userDetails.getId(), userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "회원가입 승인 대기 목록 조회", description = "승인 대기(PENDING) 사용자 목록을 조회합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
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
                                  "result": [
                                    {
                                      "userId": 21,
                                      "nameKor": "홍길동",
                                      "nameEng": "HONG GILDONG",
                                      "birthDate": "1990-01-01",
                                      "nationality": "대한민국",
                                      "zipcode": "06234",
                                      "address1": "서울특별시 강남구 테헤란로 123",
                                      "address2": "어썸리드빌딩 5층",
                                      "registrationNumber": "9001011234567",
                                      "phoneNumber": "01012345678",
                                      "email": "hong@test.com",
                                      "workLocation": "어썸리드",
                                      "departmentName": "경영지원부",
                                      "position": "사원",
                                      "jobType": "관리직",
                                      "authorities": ["메세지 작성"],
                                      "hireDate": "2025-09-22",
                                      "resignationDate": null,
                                      "role": "일반 사용자",
                                      "status": "PENDING"
                                    }
                                  ]
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
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
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "NO_AUTHORITY_FOR_REGISTRATION",
                                  "message": "회원가입 승인 권한이 없습니다.",
                                  "result": null
                                }
                                """)))
        })
    @GetMapping("/users/pending")
    public ResponseEntity<ApiResponse<List<PendingUserSummaryResponseDto>>> getPendingUsers(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PendingUserSummaryResponseDto> result =
            adminService.getPendingSignupUsers(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
        summary = "회원가입 승인",
        description = "대기 상태(PENDING)인 사용자의 부서 및 직급을 설정하고 최종 승인(AVAILABLE) 처리합니다.")
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
                                "result": "1번 사용자의 회원가입이 성공적으로 승인되었습니다."
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
                            name = "이미 승인된 사용자",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "DUPLICATED_SIGNUP_REQUEST",
                                      "message": "이미 승인 처리가 완료된 사용자입니다.",
                                      "result": null
                                    }
                                    """),
                        @ExampleObject(
                            name = "입력값 검증 실패",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "COMMON400",
                                      "message": "입력값이 유효하지 않습니다.",
                                      "result": {
                                        "departmentId": "부서 ID는 필수입니다.",
                                        "position": "직급 정보가 누락되었습니다."
                                      }
                                    }
                                    """)
                    })),
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
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "USER_NOT_FOUND",
                                      "message": "해당 사용자를 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """),
                        @ExampleObject(
                            name = "부서 없음",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "DEPARTMENT_NOT_FOUND",
                                      "message": "해당 부서를 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """)
                    }))
        })
    @PatchMapping("/users/{userId}/approve")
    public ResponseEntity<ApiResponse<String>> approveUser(
        @Parameter(description = "승인할 사용자의 ID", example = "1", required = true)
        @PathVariable("userId")
        Long userId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "사용자 승인 정보",
            required = true,
            content =
            @Content(
                mediaType = "application/json",
                schema =
                @Schema(
                    implementation =
                        UserApprovalRequestDto.class),
                examples = {
                    @ExampleObject(
                        name = "권한 1개 선택 예시",
                        value =
                            """
                                {
                                  "nameKor": "홍길동",
                                  "nameEng": "HONG GILDONG",
                                  "birthDate": "1990-01-01",
                                  "nationality": "대한민국",
                                  "zipcode": "06234",
                                  "address1": "서울특별시 강남구 테헤란로 123",
                                  "address2": "어썸리드빌딩 5층",
                                  "registrationNumber": "9001011234567",
                                  "phoneNumber": "01099998888",
                                  "workLocation": "어썸리드",
                                  "departmentId": 1,
                                  "position": "사원",
                                  "jobType": "관리직",
                                  "authorities": ["메세지 작성"],
                                  "hireDate": "2025-09-22",
                                  "resignationDate": null,
                                  "role": "일반 사용자"
                                }
                                """),
                    @ExampleObject(
                        name = "권한 5개 선택 예시",
                        value =
                            """
                                {
                                  "nameKor": "홍길동",
                                  "nameEng": "HONG GILDONG",
                                  "birthDate": "1990-01-01",
                                  "nationality": "대한민국",
                                  "zipcode": "06234",
                                  "address1": "서울특별시 강남구 테헤란로 123",
                                  "address2": "어썸리드빌딩 5층",
                                  "registrationNumber": "9001011234567",
                                  "phoneNumber": "01099998888",
                                  "workLocation": "어썸리드",
                                  "departmentId": 1,
                                  "position": "사원",
                                  "jobType": "관리직",
                                  "authorities": [
                                    "메세지 작성",
                                    "교육 작성",
                                    "공지 작성",
                                    "방문자 관리 접근",
                                    "사원 데이터 관리"
                                  ],
                                  "hireDate": "2025-09-22",
                                  "resignationDate": null,
                                  "role": "일반 사용자"
                                }
                                """)
                }))
        @Parameter(description = "사용자 승인 정보 (부서 ID, 직급 등)", required = true)
        @Valid
        @RequestBody
        UserApprovalRequestDto requestDto,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        adminService.approveUserRegistration(userId, requestDto, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(userId + "번 사용자의 회원가입이 성공적으로 승인되었습니다."));
    }

    @Operation(
        summary = "사용자 역할(Role) 변경",
        description = "특정 사용자의 역할을 변경합니다. 관리자(`ADMIN`) 또는 마스터 관리자(`MASTER_ADMIN`) 권한이 필요합니다.")
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
                                "result": "5번 사용자의 역할이 관리자(으)로 성공적으로 변경되었습니다."
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 부족",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        name = "역할 변경 권한 없음",
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

    @Operation(
        summary = "사용자 세부 권한 변경",
        description =
            "특정 사용자에게 여러 권한을 일괄 추가(ADD)하거나 제거(REMOVE)합니다. ADMIN 또는 MASTER_ADMIN만 가능합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "권한 변경 성공",
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
                                "result": "권한이 성공적으로 변경되었습니다."
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터",
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
                                    "result": null
                                    }
                                    """),
                        @ExampleObject(
                            name = "이미 부여된 권한 추가",
                            value =
                                """
                                    {
                                    "isSuccess": false,
                                    "code": "AUTHORITY_ALREADY_ASSIGNED",
                                    "message": "이미 부여된 권한입니다.",
                                    "result": null
                                    }
                                    """),
                        @ExampleObject(
                            name = "없는 권한 제거",
                            value =
                                """
                                    {
                                    "isSuccess": false,
                                    "code": "AUTHORITY_NOT_ASSIGNED",
                                    "message": "부여되지 않은 권한은 제거할 수 없습니다.",
                                    "result": null
                                    }
                                    """)
                    })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 부족",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
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
    @PatchMapping("/users/{userId}/authority")
    public ResponseEntity<ApiResponse<String>> updateUserAuthority(
        @Parameter(description = "권한 변경 대상 사용자 ID", example = "22", required = true)
        @PathVariable
        Long userId,
        @Parameter(
            name = "authorities",
            description = "변경할 권한 목록 (복수 선택 가능, 영문/한글 설명값 모두 입력 가능)",
            style = ParameterStyle.FORM,
            explode = Explode.TRUE)
        @RequestParam
        List<Authority> authorities,
        @Parameter(
            description = "권한 변경 동작 (ADD/REMOVE 또는 추가/제거)",
            example = "추가",
            required = true,
            schema = @Schema(implementation = AuthorityAction.class))
        @RequestParam
        AuthorityAction action,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        adminService.updateUserAuthority(userId, authorities, action, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess("권한이 성공적으로 변경되었습니다."));
    }

    @Operation(
        summary = "내 정보 수정 요청 승인",
        description = "해당 사용자(userId)의 최신 PENDING 개인정보 수정 요청 1건을 승인하고 실제 사용자 정보에 반영합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "승인 성공",
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
                                  "result": "22번 사용자의 개인정보 수정 요청이 승인되었습니다."
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "NO_AUTHORITY_FOR_MY_INFO_UPDATE_APPROVAL",
                                  "message": "개인정보 수정 승인 권한이 없습니다.",
                                  "result": null
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "요청 또는 사용자 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "사용자 없음",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "USER_NOT_FOUND",
                                      "message": "해당 사용자를 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """),
                        @ExampleObject(
                            name = "요청 없음",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "MY_INFO_UPDATE_REQUEST_NOT_FOUND",
                                      "message": "해당 개인정보 수정 요청을 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """)
                    }))
        })
    @PatchMapping("/users/{userId}/my-info/approve")
    public ResponseEntity<ApiResponse<String>> approveMyInfoUpdate(
        @Parameter(description = "승인 대상 사용자 ID", required = true, example = "22") @PathVariable
        Long userId,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        adminService.approveMyInfoUpdate(userId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(userId + "번 사용자의 개인정보 수정 요청이 승인되었습니다."));
    }

    @Operation(
        summary = "내 정보 수정 요청 반려",
        description = "해당 사용자(userId)의 최신 PENDING 개인정보 수정 요청 1건을 반려합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "반려 성공",
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
                                  "result": "22번 사용자의 개인정보 수정 요청이 반려되었습니다."
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
                                  "code": "MY_INFO_UPDATE_REJECT_REASON_REQUIRED",
                                  "message": "반려 사유를 입력해주세요.",
                                  "result": null
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "NO_AUTHORITY_FOR_MY_INFO_UPDATE_APPROVAL",
                                  "message": "개인정보 수정 승인 권한이 없습니다.",
                                  "result": null
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "요청 또는 사용자 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples = {
                        @ExampleObject(
                            name = "사용자 없음",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "USER_NOT_FOUND",
                                      "message": "해당 사용자를 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """),
                        @ExampleObject(
                            name = "요청 없음",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "MY_INFO_UPDATE_REQUEST_NOT_FOUND",
                                      "message": "해당 개인정보 수정 요청을 찾을 수 없습니다.",
                                      "result": null
                                    }
                                    """)
                    }))
        })
    @PatchMapping("/users/{userId}/my-info/reject")
    public ResponseEntity<ApiResponse<String>> rejectMyInfoUpdate(
        @Parameter(description = "반려 대상 사용자 ID", required = true, example = "22") @PathVariable
        Long userId,
        @Valid @RequestBody MyInfoUpdateRejectRequestDto requestDto,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        adminService.rejectMyInfoUpdate(userId, requestDto.getReason(), userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(userId + "번 사용자의 개인정보 수정 요청이 반려되었습니다."));
    }

    @Operation(
        summary = "개인정보 수정 대기 요청 목록 조회",
        description = "승인 대기(PENDING) 개인정보 수정 요청 목록을 조회합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
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
                                  "result": [
                                    {
                                      "requestId": 101,
                                      "userId": 17,
                                      "nameKor": "홍길동",
                                      "email": "hgd@gmail.com",
                                      "requestedNameEng": "HONG GILDONG",
                                      "requestedPhoneNumber": null,
                                      "requestedZipcode": "06234",
                                      "requestedAddress1": "서울특별시 강남구 테헤란로 123",
                                      "requestedAddress2": "어썸리드빌딩 5층",
                                      "status": "PENDING",
                                      "requestedAt": "2026-02-26T15:20:00"
                                    }
                                  ]
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "NO_AUTHORITY_FOR_MY_INFO_UPDATE_APPROVAL",
                                  "message": "개인정보 수정 승인 권한이 없습니다.",
                                  "result": null
                                }
                                """)))
        })
    @GetMapping("/users/my-info/requests/pending")
    public ResponseEntity<ApiResponse<List<MyInfoUpdateRequestSummaryResponseDto>>>
    getPendingMyInfoUpdateRequests(
        @Parameter(hidden = true) @AuthenticationPrincipal
        CustomUserDetails userDetails) {
        List<MyInfoUpdateRequestSummaryResponseDto> result =
            adminService.getPendingMyInfoUpdateRequests(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

}
