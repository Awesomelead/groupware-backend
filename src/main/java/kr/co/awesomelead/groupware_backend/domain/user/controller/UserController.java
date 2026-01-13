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
                                "registrationNumberFront": "900101-1******",
                                "phoneNumber": "01012345678",
                                "email": "timber@example.com",
                                "workLocation": "AWESOME",
                                "departmentName": "CHUNGNAM_HQ",
                                "jobType": "MANAGEMENT",
                                "position": "대리"
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
            description = "영문 이름과 전화번호를 수정합니다. 전화번호 변경 시 알리고 알림톡 인증이 필요합니다.")
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
                              "message": "성공",
                              "result": {
                                "id": 1,
                                "nameKor": "김철수",
                                "nameEng": "Kim Chulsoo Updated",
                                "birthDate": "1990-01-01",
                                "nationality": "대한민국",
                                "registrationNumberFront": "900101-1******",
                                "phoneNumber": "01098765432",
                                "email": "timber@example.com",
                                "workLocation": "AWESOME",
                                "departmentName": "CHUNGNAM_HQ",
                                "jobType": "MANAGEMENT",
                                "position": "대리"
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
                                    "phoneNumber": "전화번호는 '-' 없이 10~11자리 숫자로 입력해주세요."
                                  }
                                }
                                """),
                                            @ExampleObject(
                                                    name = "영문 이름 동일",
                                                    value =
                                                            """
                                {
                                  "isSuccess": false,
                                  "code": "NAME_ENG_ALREADY_SAME",
                                  "message": "입력한 영문 이름이 현재 영문 이름과 동일합니다.",
                                  "result": null
                                }
                                """),
                                            @ExampleObject(
                                                    name = "전화번호 동일",
                                                    value =
                                                            """
                                {
                                  "isSuccess": false,
                                  "code": "PHONE_NUMBER_ALREADY_SAME",
                                  "message": "입력한 전화번호가 현재 전화번호와 동일합니다.",
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
}
