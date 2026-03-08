package kr.co.awesomelead.groupware_backend.domain.requesthistory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.AdminRequestHistoryDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.AdminRequestHistorySummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestHistoryStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.service.RequestHistoryService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/request-histories")
@Tag(name = "Admin RequestHistory", description = "관리자 제증명 발급 신청 관리 API")
public class AdminRequestHistoryController {

    private final RequestHistoryService requestHistoryService;

    @Operation(summary = "개인 증명서류 신청 목록 조회", description = "관리자 화면에서 전체 신청 목록을 조회합니다.")
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
                                    "content": [
                                      {
                                        "requestId": 101,
                                        "userId": 17,
                                        "nameKor": "홍길동",
                                        "departmentName": "경영지원부",
                                        "position": "사원",
                                        "requestType": "재직증명서",
                                        "purpose": "은행 제출용",
                                        "copies": 1,
                                        "wishDate": "2026-03-10",
                                        "requestDate": "2026-03-07",
                                        "approvalStatus": "대기"
                                      }
                                    ]
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
                                  "code": "NO_AUTHORITY_FOR_CERTIFICATE_REQUEST_REVIEW",
                                  "message": "제증명 신청 승인/반려 권한이 없습니다.",
                                  "result": null
                                }
                                """)))
        })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminRequestHistorySummaryResponseDto>>> getAllRequests(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(
            description = "상태 필터 (선택값: 발급 대기, 발급 완료, 반려, 취소)",
            required = false,
            example = "발급 대기")
        @RequestParam(required = false)
        RequestHistoryStatus status,
        @ParameterObject @PageableDefault(page = 0, size = 20) Pageable pageable) {
        Page<AdminRequestHistorySummaryResponseDto> result =
            requestHistoryService.getAllRequestsForAdmin(userDetails.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "개인 증명서류 신청 상세 조회", description = "관리자 화면에서 특정 신청의 상세 정보를 조회합니다.")
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
                                    "requestId": 101,
                                    "userId": 17,
                                    "nameKor": "홍길동",
                                    "nameEng": "HONG GILDONG",
                                    "departmentName": "경영지원부",
                                    "position": "사원",
                                    "requestType": "재직증명서",
                                    "purpose": "은행 제출용",
                                    "copies": 1,
                                    "wishDate": "2026-03-10",
                                    "requestDate": "2026-03-07",
                                    "approvalStatus": "발급 대기",
                                    "processedByName": null,
                                    "processedDate": null
                                  }
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "신청 내역 없음",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "REQUEST_HISTORY_NOT_FOUND",
                                  "message": "해당 제증명 발급 신청 내역을 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            ,
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
                                  "code": "NO_AUTHORITY_FOR_CERTIFICATE_REQUEST_REVIEW",
                                  "message": "제증명 신청 승인/반려 권한이 없습니다.",
                                  "result": null
                                }
                                """)))
        })
    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<AdminRequestHistoryDetailResponseDto>> getRequestDetail(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(description = "신청 ID", required = true, example = "101") @PathVariable
        Long requestId) {
        AdminRequestHistoryDetailResponseDto result =
            requestHistoryService.getRequestDetailForAdmin(userDetails.getId(), requestId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "개인 증명서류 발급 완료 처리", description = "제증명 신청을 발급 완료 처리합니다. (PENDING -> ISSUED)")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "처리 불가 상태",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "REQUEST_HISTORY_NOT_ISSUABLE",
                                  "message": "발급 대기 상태 요청만 발급 완료 처리할 수 있습니다.",
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
                                  "code": "NO_AUTHORITY_FOR_CERTIFICATE_REQUEST_REVIEW",
                                  "message": "제증명 신청 승인/반려 권한이 없습니다.",
                                  "result": null
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "신청 내역 없음")
        })
    @PatchMapping("/{requestId}/issue")
    public ResponseEntity<ApiResponse<String>> issueRequest(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(description = "신청 ID", required = true, example = "101") @PathVariable
        Long requestId) {
        requestHistoryService.issueRequest(userDetails.getId(), requestId);
        return ResponseEntity.ok(ApiResponse.onSuccess("발급 완료 처리되었습니다."));
    }
}
