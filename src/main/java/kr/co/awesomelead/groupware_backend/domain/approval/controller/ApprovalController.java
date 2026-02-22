package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalListRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalProcessRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@Tag(
        name = "Approval",
        description =
                """
    ## 전자결재 시스템 API

    다양한 결재 양식의 생성(상신), 조회, 승인 및 반려 기능을 제공합니다.

    ### 주요 기능
    - **다형성 상신**: 하나의 엔드포인트에서 `documentType`에 따라 서로 다른 양식의 문서를 상신할 수 있습니다.
    - **결재선 관리**: 기안 시 지정된 결재 순서에 따라 프로세스가 진행됩니다.
    - **부서 스냅샷**: 기안 시점의 사용자 부서 정보가 자동으로 저장되어 조직 개편 시에도 데이터의 무결성을 보장합니다.
    """)
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(
            summary = "전자결재 문서 상신 (기안)",
            description =
                    """
        새로운 결재 문서를 작성하여 상신합니다.
        `documentType` 필드값(LEAVE, CAR_FUEL, EXPENSE_DRAFT 등)에 따라 본문에 포함되어야 할 상세 필드가 달라집니다.
        """)
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "상신 성공",
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
              "result": 1
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
                    "title": "제목은 필수입니다.",
                    "approvalSteps": "결재선은 최소 1명 이상 지정해야 합니다.",
                    "details": "지출 상세 내역은 최소 1건 이상이어야 합니다."
                  }
                }
                """),
                                            @ExampleObject(
                                                    name = "잘못된 문서 타입",
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "INVALID_DOCUMENT_TYPE",
                  "message": "지원하지 않는 결재 양식입니다.",
                  "result": null
                }
                """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "연관 데이터 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "결재자 찾을 수 없음",
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "APPROVER_NOT_FOUND",
                  "message": "지정된 결재자 정보를 찾을 수 없습니다.",
                  "result": null
                }
                """),
                                            @ExampleObject(
                                                    name = "첨부파일 찾을 수 없음",
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "ATTACHMENT_NOT_FOUND",
                  "message": "요청된 첨부파일 ID 중 일부가 유효하지 않습니다.",
                  "result": null
                }
                """)
                                        }))
            })
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createApproval(
            @Parameter(description = "상신할 결재 문서 정보 (타입별 상세 필드 포함)", required = true)
                    @RequestBody
                    @Valid
                    ApprovalCreateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 기안자 ID는 세션에서 안전하게 추출하여 전달
        Long approvalId = approvalService.createApproval(requestDto, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(approvalId));
    }

    @Operation(
            summary = "결재 문서 승인",
            description =
                    """
        지정된 결재 문서를 승인합니다.

        **승인 조건**:
        - 요청자가 해당 결재 문서의 결재선(ApprovalStep)에 포함된 결재 대상자여야 합니다.
        - 요청자의 결재 순서(sequence)가 현재 차례여야 합니다 (PENDING 상태인 가장 낮은 sequence).

        **승인 후 동작**:
        - 다음 결재 단계가 있으면 자동으로 WAITING → PENDING 상태로 전환됩니다.
        - 모든 결재 단계가 승인되면 문서 전체 상태가 APPROVED로 변경됩니다.
        """)
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
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "이미 처리된 결재 단계",
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "ALREADY_PROCESSED_STEP",
                  "message": "이미 처리된 결재 단계입니다.",
                  "result": null
                }
                """),
                                            @ExampleObject(
                                                    name = "결재 순서 아님",
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "NOT_YOUR_TURN",
                  "message": "아직 본인의 결재 순서가 아닙니다.",
                  "result": null
                }
                """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "결재 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NOT_APPROVER",
              "message": "해당 결재 문서의 결재 대상자가 아닙니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "결재 문서 또는 사용자를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "APPROVAL_NOT_FOUND",
              "message": "해당 결재 문서를 찾을 수 없습니다.",
              "result": null
            }
            """)))
            })
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveApproval(
            @Parameter(description = "승인할 결재 문서 ID", required = true, example = "1") @PathVariable
                    Long id,
            @Parameter(description = "승인 요청 정보 (의견)", required = true) @RequestBody
                    ApprovalProcessRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        approvalService.approveApproval(id, userDetails.getId(), requestDto.getComment());

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(
            summary = "결재 문서 반려",
            description =
                    """
        지정된 결재 문서를 반려합니다.

        **반려 조건**:
        - 요청자가 해당 결재 문서의 결재선(ApprovalStep)에 포함된 결재 대상자여야 합니다.
        - 요청자의 결재 순서(sequence)가 현재 차례여야 합니다 (PENDING 상태인 가장 낮은 sequence).

        **반려 후 동작**:
        - 문서 전체 상태가 즉시 REJECTED로 변경됩니다.
        """)
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "반려 성공",
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
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "이미 처리된 결재 단계",
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "ALREADY_PROCESSED_STEP",
                  "message": "이미 처리된 결재 단계입니다.",
                  "result": null
                }
                """),
                                            @ExampleObject(
                                                    name = "결재 순서 아님",
                                                    value =
                                                            """
                {
                  "isSuccess": false,
                  "code": "NOT_YOUR_TURN",
                  "message": "아직 본인의 결재 순서가 아닙니다.",
                  "result": null
                }
                """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "결재 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "NOT_APPROVER",
              "message": "해당 결재 문서의 결재 대상자가 아닙니다.",
              "result": null
            }
            """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "결재 문서 또는 사용자를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
            {
              "isSuccess": false,
              "code": "APPROVAL_NOT_FOUND",
              "message": "해당 결재 문서를 찾을 수 없습니다.",
              "result": null
            }
            """)))
            })
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApproval(
            @Parameter(description = "반려할 결재 문서 ID", required = true, example = "1") @PathVariable
                    Long id,
            @Parameter(description = "반려 요청 정보 (반려 사유)", required = true) @RequestBody
                    ApprovalProcessRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        approvalService.rejectApproval(id, userDetails.getId(), requestDto.getComment());

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(
            summary = "결재 문서 목록 조회 (필터 및 페이징)",
            description =
                    """
        다양한 조건(Category, Status, 양식)에 따라 전자결재 문서 목록을 조회합니다.

        **Category 유형**:
        - `ALL`: 전체 문서 (관리자는 조건 없이 시스템 내 모든 문서 조회, 일반 사용자는 자신과 연관된 전체 문서)
        - `IN_PROGRESS`: 결재 진행 중이거나 반려된 문서 중 내가 결재선에 포함되어 있는 문서
        - `REFERENCE`: 내가 참조자(REFERRER)이거나 열람권자(VIEWER)인 문서
        - `DRAFT`: 내가 기안한 사상 문서

        **Category 별 하위 Status 필터**:
        - IN_PROGRESS -> `WAITING` (내 결재 대기), `APPROVED` (내가 이미 결재함), `REJECTED` (결재선 중 누군가 반려함)
        - REFERENCE -> `REFERENCE` (기안 시 참조됨), `READ` (완료 후 열람 권한 획득)
        - DRAFT -> `WAITING` (결재가 끝나지 않음), `APPROVED` (기결 완료됨), `REJECTED` (반려됨/취소함)

        (Status에 ALL을 전달하거나 제외하면 해당 Category의 묶음 전체를 조회합니다.)
        """)
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
                "content": [
                  {
                    "id": 1,
                    "documentNumber": "기본양식 개발팀 20250407-123",
                    "drafterName": "홍길동",
                    "title": "비품 구매 요청",
                    "status": "PENDING",
                    "approvalLine": "[홍길동 > 김팀장 > 이본부장]",
                    "draftDate": "2025-04-07T10:00:00",
                    "completedDate": null
                  }
                ],
                "pageable": { ... },
                "totalElements": 1,
                "totalPages": 1,
                "last": true
              }
            }
            """)))
            })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ApprovalSummaryResponseDto>>> getApprovalList(
            @ModelAttribute ApprovalListRequestDto condition,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        Page<ApprovalSummaryResponseDto> result =
                approvalService.getApprovalList(condition, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "결재 문서 단건 상세 조회",
            description =
                    """
        요청한 ID에 해당하는 결재 문서의 상세 정보(결재선 이력, 참조자 이력, 문서 양식별 상세 필드 등)를 조회합니다.
        해당 문서에 대한 조회 권한(기안자, 결재자, 참조자 혹은 관리자)이 없는 경우 403 예외가 발생합니다.
        """)
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "조회 권한 없음",
                        content = @Content(mediaType = "application/json")),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "문서를 찾을 수 없음",
                        content = @Content(mediaType = "application/json"))
            })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApprovalDetailResponseDto>> getApprovalDetail(
            @Parameter(description = "상세 조회할 결재 문서 ID", required = true, example = "1")
                    @PathVariable
                    Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        ApprovalDetailResponseDto result =
                approvalService.getApprovalDetail(id, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
