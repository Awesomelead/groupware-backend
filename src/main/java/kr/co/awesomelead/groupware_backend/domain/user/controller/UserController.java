package kr.co.awesomelead.groupware_backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.UpdateMyInfoRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.service.UserService;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "User",
    description =
        """
            ## 내 정보 조회 및 수정 관련 API
            
            ### 사용되는 Enum 타입
            - **Company**: 근무 사업장
              - AWESOME: 어썸리드
              - MARUI: 한국마루이
            
            - **DepartmentName**: 부서명
              - CHUNGNAM_HQ: 충남사업본부
              - MARUI_LAB: (주)한국마루이 연구소
              - AWESOME_LAB: (주)어썸리드 연구소
              - SALES_DEPT: 영업부
              - CHUNGNAM_PLANNING: 충남 경영기획실
              - AWESOME_PROD_HQ: (주)어썸리드 생산본부
              - QUALITY_SAFETY_HQ: 품질안전본부
              - MARUI_PROD_HQ: (주)한국마루이 생산본부
              - TECHNICAL_ADVISOR: 기술고문
              - SECURITY_DEPT: 경비
              - MANAGEMENT_SUPPORT: 경영지원부
              - CHAMBER_PROD: 챔버생산부
              - PARTS_PROD: 부품생산부
              - ENVIRONMENT_SAFETY: 환경안전부
              - QUALITY_CONTROL: 품질관리부
              - PRODUCTION: 생산부
              - PRODUCTION_MANAGEMENT: 생산관리부
            
            - **JobType**: 근무 직종
              - FIELD: 현장직
              - MANAGEMENT: 관리직
            
            
            - **Role**: 사용자 역할
              - USER: 일반 사용자
              - ADMIN: 관리자
              - MASTER_ADMIN: 마스터 관리자
            
            - **Status**: 사용자 상태
              - PENDING: 승인 대기
              - AVAILABLE: 활성
              - SUSPENDED: 비활성
            
            - **Authority**: 권한
              - WRITE_MESSAGE: 메세지 작성 권한 -> jobType이 관리직일 경우 부여하고 시작
              - WRITE_EDUCATION: 교육 작성 권한 -> jobType이 관리직일 경우 부여하고 시작
              - WRITE_NOTICE: 공지 작성 권한
              - UPLOAD_ANNUAL_LEAVE: 연차 업로드 권한
            """)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MyInfoResponseDto.class),
                    examples =
                    @ExampleObject(
                        value =
                            """
                                                                        {
                                                                           "isSuccess": true,
                                                                           "code": "COMMON200",
                                                                           "message": "성공",
                                                                           "result": {
                                                                             "id": 1,
                                                                             "nameKor": "김철수",
                                                                             "nameEng": "Kim Chulsoo",
                                                                             "birthDate": "1990-01-01",
                                                                             "nationality": "대한민국",
                                                                             "zipcode": "03127",
                                                                             "address1": "충남 아산시 둔포면 아산밸리로388번길 10",
                                                                             "address2": "101호",
                                                                             "registrationNumberFront": "900101-1******",
                                                                             "phoneNumber": "01012345678",
                                                                             "email": "timber@example.com",
                                                                             "workLocation": "어썸리드",
                                                                             "departmentName": "충남사업본부",
                                                                             "position": "대리",
                                                                             "jobType": "관리직",
                                                                             "authorities": [
                                                                               { "code": "ACCESS_MESSAGE", "label": "메세지 작성", "enabled": true },
                                                                               { "code": "ACCESS_EDUCATION", "label": "교육 작성", "enabled": false },
                                                                               { "code": "ACCESS_NOTICE", "label": "공지 작성", "enabled": false },
                                                                               { "code": "ACCESS_VISIT", "label": "방문자 관리 접근", "enabled": false },
                                                                               { "code": "MANAGE_EMPLOYEE_DATA", "label": "사원 데이터 관리", "enabled": false }
                                                                             ],
                                                                             "hireDate": "2024-03-01",
                                                                             "resignationDate": null
                                                                           }
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
                                  "code": "AUTH401",
                                  "message": "인증이 필요합니다.",
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
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponseDto>> getMyInfo(
        @AuthenticationPrincipal UserDetails userDetails) {
        MyInfoResponseDto response = userService.getMyInfo(userDetails);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(
        summary = "내 정보 수정",
        description =
                "내 정보 수정 요청을 생성합니다. 요청은 관리자 승인 후 반영됩니다. "
                        + "수정 가능 필드: 영문 이름, 전화번호, 우편번호, 주소1, 주소2")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MyInfoResponseDto.class),
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": true,
                                  "code": "COMMON200",
                                  "message": "요청에 성공했습니다.",
                                  "result": {
                                    "id": 1,
                                    "nameKor": "김철수"
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
                            name = "변경 항목 없음",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "MY_INFO_UPDATE_NO_CHANGES",
                                      "message": "변경 요청할 내 정보가 없습니다.",
                                      "result": null
                                    }
                                    """),
                        @ExampleObject(
                            name = "이미 대기중인 요청 존재",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "MY_INFO_UPDATE_ALREADY_PENDING",
                                      "message": "이미 처리 대기 중인 개인정보 수정 요청이 있습니다.",
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
                            name = "전화번호 중복",
                            value =
                                """
                                    {
                                      "isSuccess": false,
                                      "code": "PHONE_NUMBER_ALREADY_EXISTS",
                                      "message": "이미 사용 중인 전화번호입니다.",
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
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponseDto>> updateMyInfo(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody UpdateMyInfoRequestDto requestDto) {
        MyInfoResponseDto response = userService.updateMyInfo(userDetails, requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "내 정보 수정 요청 취소", description = "본인이 생성한 개인정보 수정 요청(PENDING) 1건을 취소합니다.")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "취소 성공",
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
                                  "result": "개인정보 수정 요청이 취소되었습니다."
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
                                  "code": "MY_INFO_UPDATE_REQUEST_NOT_CANCELABLE",
                                  "message": "대기 상태 요청만 취소할 수 있습니다.",
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
                                  "code": "NO_AUTHORITY_FOR_MY_INFO_UPDATE_CANCEL",
                                  "message": "본인의 개인정보 수정 요청만 취소할 수 있습니다.",
                                  "result": null
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "대상 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "MY_INFO_UPDATE_REQUEST_NOT_FOUND",
                                  "message": "해당 개인정보 수정 요청을 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
        })
    @PatchMapping("/me/my-info/requests/{requestId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelMyInfoUpdateRequest(
        @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long requestId) {
        userService.cancelMyInfoUpdateRequest(userDetails, requestId);
        return ResponseEntity.ok(ApiResponse.onSuccess("개인정보 수정 요청이 취소되었습니다."));
    }
}
