package kr.co.awesomelead.groupware_backend.domain.visit.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.service.VisitService;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visits")
@Tag(
        name = "Visit",
        description =
                """
            ## 방문 관리 API

            내방객의 사전 예약 및 현장 방문 접수, 방문 정보 조회, 입/퇴실 처리 등을 수행합니다.

            ### 사용되는 Enum 타입
            - **VisitType**: 방문 유형 (PRE_REGISTRATION: 사전 예약, ON_SITE: 현장 방문)
            - **VisitPurpose**: 방문 목적 (CUSTOMER_INSPECTION: 고객 검수, GOODS_DELIVERY: 물품 납품, FACILITY_CONSTRUCTION: 시설공사, LOGISTICS: 입출고, MEETING: 미팅, OTHER: 기타)
            """)
public class VisitController {

    private final VisitService visitService;

    @Operation(
            summary = "사전 방문 접수",
            description = "내방객이 온라인으로 사전방문접수를 수행합니다. 서명 이미지(PNG)와 4자리 비밀번호가 필수입니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "사전 방문 접수 성공",
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
                                  "code": "COMMON201",
                                  "message": "성공적으로 생성되었습니다.",
                                  "result": {
                                    "id": 1,
                                    "visitType": "PRE_REGISTRATION",
                                    "visitorName": "홍길동",
                                    "visitorPhone": "01012345678",
                                    "visitorCompany": "어썸리드",
                                    "carNumber": "12가3456",
                                    "purpose": "MEETING",
                                    "visitStartDate": "2025-01-15T14:00:00",
                                    "visitEndDate": null,
                                    "hostUserId": 1,
                                    "hostName": "이담당",
                                    "hostDepartment": "개발팀",
                                    "visited": false,
                                    "verified": false,
                                    "companions": []
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
                                        "visitorName": "내방객 이름은 필수입니다.",
                                        "visitorPhone": "올바른 전화번호 형식이 아닙니다.",
                                        "visitorPassword": "비밀번호는 4자리여야 합니다."
                                      }
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "비밀번호 미입력",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "VISITOR_PASSWORD_REQUIRED_FOR_PRE_REGISTRATION",
                                      "message": "사전 방문 예약 시 내방객 비밀번호가 필요합니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "서명 미제공",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "NO_SIGNATURE_PROVIDED",
                                      "message": "서명이 제공되지 않았습니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "잘못된 서명 형식",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "INVALID_SIGNATURE_FORMAT",
                                      "message": "서명은 PNG 파일 형식만 지원합니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "담당자를 찾을 수 없음",
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
                        responseCode = "500",
                        description = "서버 에러 (S3 업로드 실패 등)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON500",
                                  "message": "서버 내부 오류가 발생했습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping(value = "/pre-registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisitResponseDto>> createPreVisit(
            @Parameter(
                            description = "방문 정보 JSON (방문자 성명, 연락처, 담당자 ID 등)",
                            required = true,
                            schema = @Schema(implementation = VisitCreateRequestDto.class))
                    @RequestPart("requestDto")
                    @Valid
                    VisitCreateRequestDto requestDto,
            @Parameter(description = "내방객 서명 이미지 (PNG 형식 필수)", required = true)
                    @RequestPart(value = "signatureFile")
                    MultipartFile signatureFile)
            throws IOException {

        VisitResponseDto responseDto = visitService.createPreVisit(requestDto, signatureFile);

        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/visits/{id}")
                        .buildAndExpand(responseDto.getId())
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(responseDto));
    }

    @Operation(
            summary = "현장 방문 접수",
            description = "내방객이 현장에서 방문접수를 수행합니다. 서명 이미지(PNG)가 필수이며, 비밀번호는 선택사항입니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "현장 방문 접수 성공",
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
                                  "code": "COMMON201",
                                  "message": "성공적으로 생성되었습니다.",
                                  "result": {
                                    "id": 2,
                                    "visitType": "ON_SITE",
                                    "visitorName": "김철수",
                                    "visitorPhone": "01087654321",
                                    "visitorCompany": "테스트회사",
                                    "carNumber": null,
                                    "purpose": "GOODS_DELIVERY",
                                    "visitStartDate": "2025-01-15T10:00:00",
                                    "visitEndDate": null,
                                    "hostUserId": 2,
                                    "hostName": "박담당",
                                    "hostDepartment": "영업팀",
                                    "visited": true,
                                    "verified": true,
                                    "companions": []
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
                                        "visitorName": "내방객 이름은 필수입니다.",
                                        "purpose": "방문 목적은 필수입니다."
                                      }
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "서명 미제공",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "NO_SIGNATURE_PROVIDED",
                                      "message": "서명이 제공되지 않았습니다.",
                                      "result": null
                                    }
                                    """),
                                            @ExampleObject(
                                                    name = "잘못된 서명 형식",
                                                    value =
                                                            """
                                    {
                                      "isSuccess": false,
                                      "code": "INVALID_SIGNATURE_FORMAT",
                                      "message": "서명은 PNG 파일 형식만 지원합니다.",
                                      "result": null
                                    }
                                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "담당자를 찾을 수 없음",
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
                        responseCode = "500",
                        description = "서버 에러 (S3 업로드 실패 등)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON500",
                                  "message": "서버 내부 오류가 발생했습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping(value = "/on-site", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VisitResponseDto>> createOnSiteVisit(
            @Parameter(
                            description = "방문 정보 JSON (방문자 성명, 연락처, 담당자 ID 등)",
                            required = true,
                            schema = @Schema(implementation = VisitCreateRequestDto.class))
                    @RequestPart("requestDto")
                    @Valid
                    VisitCreateRequestDto requestDto,
            @Parameter(description = "내방객 서명 이미지 (PNG 형식 필수)", required = true)
                    @RequestPart("signatureFile")
                    MultipartFile signatureFile)
            throws IOException {

        VisitResponseDto responseDto = visitService.createOnSiteVisit(requestDto, signatureFile);

        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/visits/{id}")
                        .buildAndExpand(responseDto.getId())
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(responseDto));
    }

    @Operation(summary = "내 방문 정보 조회", description = "내방객이 이름, 전화번호, 비밀번호로 사전등록 정보를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
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
                                  "result": [
                                    {
                                      "visitId": 1,
                                      "visitorName": "홍길동",
                                      "visitorCompany": "어썸리드",
                                      "visitStartDate": "2025-01-15T14:00:00",
                                      "visitEndDate": null,
                                      "visited": false
                                    },
                                    {
                                      "visitId": 3,
                                      "visitorName": "홍길동",
                                      "visitorCompany": "어썸리드",
                                      "visitStartDate": "2025-01-20T09:00:00",
                                      "visitEndDate": null,
                                      "visited": false
                                    }
                                  ]
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "입력값 검증 실패",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "COMMON400",
                                  "message": "입력값이 유효하지 않습니다.",
                                  "result": {
                                    "name": "이름은 필수입니다.",
                                    "phoneNumber": "올바른 전화번호 형식이 아닙니다.",
                                    "password": "비밀번호는 4자리여야 합니다."
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "인증 실패 (이름 또는 비밀번호 불일치)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "VISITOR_AUTHENTICATION_FAILED",
                                  "message": "내방객 인증에 실패했습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "내방객을 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "VISITOR_NOT_FOUND",
                                  "message": "해당 내방객을 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PostMapping("/visitor")
    public ResponseEntity<ApiResponse<List<VisitSummaryResponseDto>>> getMyVisits(
            @RequestBody @Valid VisitSearchRequestDto requestDto) {

        List<VisitSummaryResponseDto> responseDto = visitService.getMyVisits(requestDto);

        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "내 방문 상세 정보 조회", description = "내방객이 사전등록 정보에 대한 상세 정보를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
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
                                  "result": {
                                    "visitId": 1,
                                    "visitorCompany": "어썸리드",
                                    "visitorName": "홍길동",
                                    "purpose": "MEETING",
                                    "hostDepartment": "개발팀",
                                    "phoneNumber": "01012345678",
                                    "visitStartDate": "2025-01-15T14:00:00",
                                    "visitEndDate": null,
                                    "visited": false
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "방문 정보를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "VISIT_NOT_FOUND",
                                  "message": "해당 방문정보를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @GetMapping("/visitor/{visitId}")
    public ResponseEntity<ApiResponse<MyVisitResponseDto>> getMyVisitDetail(
            @Parameter(description = "방문 ID", required = true) @PathVariable Long visitId) {
        MyVisitResponseDto responseDto = visitService.getMyVisitDetail(visitId);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(
            summary = "부서별 방문 정보 조회",
            description = "직원이 특정 부서 혹은 전체 내방객 정보 목록을 조회합니다. 경비 부서는 전체 조회 가능합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
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
                                  "result": [
                                    {
                                      "visitId": 1,
                                      "visitorName": "홍길동",
                                      "visitorCompany": "어썸리드",
                                      "visitStartDate": "2025-01-15T14:00:00",
                                      "visitEndDate": null,
                                      "visited": false
                                    },
                                    {
                                      "visitId": 2,
                                      "visitorName": "김철수",
                                      "visitorCompany": "테스트회사",
                                      "visitStartDate": "2025-01-15T10:00:00",
                                      "visitEndDate": "2025-01-15T12:00:00",
                                      "visited": true
                                    }
                                  ]
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 또는 부서를 찾을 수 없음",
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
    @GetMapping("/department")
    public ResponseEntity<ApiResponse<List<VisitSummaryResponseDto>>> getVisitsByDepartment(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "부서 ID (생략 시 전체 부서 대상 조회)", required = false)
                    @RequestParam(required = false)
                    Long departmentId) {

        List<VisitSummaryResponseDto> responseDtoList =
                visitService.getVisitsByDepartment(userDetails.getId(), departmentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(responseDtoList));
    }

    @Operation(
            summary = "직원용 방문 상세 정보 조회",
            description = "직원이 내방객 정보에 대한 상세 정보를 조회합니다. 서명 이미지 URL이 포함됩니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
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
                                  "result": {
                                    "visitId": 1,
                                    "visitorCompany": "어썸리드",
                                    "visitorName": "홍길동",
                                    "purpose": "MEETING",
                                    "hostDepartment": "개발팀",
                                    "hostName": "이담당",
                                    "phoneNumber": "01012345678",
                                    "visitStartDate": "2025-01-15T14:00:00",
                                    "visitEndDate": null,
                                    "signatureUrl": "https://bucket.s3.amazonaws.com/signatures/uuid_signature.png",
                                    "visited": false
                                  }
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "방문 정보를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "VISIT_NOT_FOUND",
                                  "message": "해당 방문정보를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @GetMapping("/employee/{visitId}")
    public ResponseEntity<ApiResponse<VisitDetailResponseDto>> getVisitDetailByEmployee(
            @Parameter(description = "방문 ID", required = true) @PathVariable Long visitId) {

        VisitDetailResponseDto responseDto = visitService.getVisitDetailByEmployee(visitId);

        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(
            summary = "방문 처리",
            description = "사전 예약된 내방객이 현장에 도착하여 방문처리를 수행합니다. 방문 시작 시간이 현재 시각으로 갱신됩니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "방문 처리 성공",
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
                                  "code": "COMMON204",
                                  "message": "성공적으로 처리되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "방문 정보를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "VISIT_NOT_FOUND",
                                  "message": "해당 방문정보를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PatchMapping("/{visitId}/check-in")
    public ResponseEntity<ApiResponse<Void>> checkIn(
            @Parameter(description = "방문 ID", required = true) @PathVariable Long visitId) {
        visitService.checkIn(visitId);

        return ResponseEntity.ok(ApiResponse.onNoContent());
    }

    @Operation(summary = "퇴실 처리", description = "내방객의 방문에 대해 퇴실 처리를 수행합니다. 방문 종료 시간이 기록됩니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "퇴실 처리 성공",
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
                                  "code": "COMMON204",
                                  "message": "성공적으로 처리되었습니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "이미 퇴실 처리됨",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "VISIT_ALREADY_CHECKED_OUT",
                                  "message": "이미 체크아웃된 방문정보입니다.",
                                  "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "방문 정보를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "VISIT_NOT_FOUND",
                                  "message": "해당 방문정보를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @PatchMapping("/check-out")
    public ResponseEntity<ApiResponse<Void>> checkOut(@RequestBody CheckOutRequestDto requestDto) {
        visitService.checkOut(requestDto);

        return ResponseEntity.ok(ApiResponse.onNoContent());
    }
}
