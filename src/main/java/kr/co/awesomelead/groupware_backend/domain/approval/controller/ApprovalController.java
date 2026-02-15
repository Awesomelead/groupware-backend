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
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalProcessRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@Tag(name = "Approval", description = """
    ## 전자결재 시스템 API

    다양한 결재 양식의 생성(상신), 조회, 승인 및 반려 기능을 제공합니다.

    ### 주요 기능
    - **다형성 상신**: 하나의 엔드포인트에서 `documentType`에 따라 서로 다른 양식의 문서를 상신할 수 있습니다.
    - **결재선 관리**: 기안 시 지정된 결재 순서에 따라 프로세스가 진행됩니다.
    - **부서 스냅샷**: 기안 시점의 사용자 부서 정보가 자동으로 저장되어 조직 개편 시에도 데이터의 무결성을 보장합니다.
    """)
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "전자결재 문서 상신 (기안)", description = """
        새로운 결재 문서를 작성하여 상신합니다.
        `documentType` 필드값(LEAVE, CAR_FUEL, EXPENSE_DRAFT 등)에 따라 본문에 포함되어야 할 상세 필드가 달라집니다.
        """)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상신 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
            {
              "isSuccess": true,
              "code": "COMMON200",
              "message": "요청에 성공했습니다.",
              "result": 1
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "입력값 검증 실패", value = """
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
            @ExampleObject(name = "잘못된 문서 타입", value = """
                {
                  "isSuccess": false,
                  "code": "INVALID_DOCUMENT_TYPE",
                  "message": "지원하지 않는 결재 양식입니다.",
                  "result": null
                }
                """)})),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "연관 데이터 없음", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "결재자 찾을 수 없음", value = """
                {
                  "isSuccess": false,
                  "code": "APPROVER_NOT_FOUND",
                  "message": "지정된 결재자 정보를 찾을 수 없습니다.",
                  "result": null
                }
                """),
            @ExampleObject(name = "첨부파일 찾을 수 없음", value = """
                {
                  "isSuccess": false,
                  "code": "ATTACHMENT_NOT_FOUND",
                  "message": "요청된 첨부파일 ID 중 일부가 유효하지 않습니다.",
                  "result": null
                }
                """)}))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createApproval(
        @Parameter(description = "상신할 결재 문서 정보 (타입별 상세 필드 포함)", required = true) @RequestBody @Valid ApprovalCreateRequestDto requestDto,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        // 기안자 ID는 세션에서 안전하게 추출하여 전달
        Long approvalId = approvalService.createApproval(requestDto, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(approvalId));
    }

    @Operation(summary = "결재 문서 승인", description = """
        지정된 결재 문서를 승인합니다.

        **승인 조건**:
        - 요청자가 해당 결재 문서의 결재선(ApprovalStep)에 포함된 결재 대상자여야 합니다.
        - 요청자의 결재 순서(sequence)가 현재 차례여야 합니다 (PENDING 상태인 가장 낮은 sequence).

        **승인 후 동작**:
        - 다음 결재 단계가 있으면 자동으로 WAITING → PENDING 상태로 전환됩니다.
        - 모든 결재 단계가 승인되면 문서 전체 상태가 APPROVED로 변경됩니다.
        """)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "승인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
            {
              "isSuccess": true,
              "code": "COMMON200",
              "message": "요청에 성공했습니다.",
              "result": null
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "이미 처리된 결재 단계", value = """
                {
                  "isSuccess": false,
                  "code": "ALREADY_PROCESSED_STEP",
                  "message": "이미 처리된 결재 단계입니다.",
                  "result": null
                }
                """),
            @ExampleObject(name = "결재 순서 아님", value = """
                {
                  "isSuccess": false,
                  "code": "NOT_YOUR_TURN",
                  "message": "아직 본인의 결재 순서가 아닙니다.",
                  "result": null
                }
                """)})),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "결재 권한 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "isSuccess": false,
              "code": "NOT_APPROVER",
              "message": "해당 결재 문서의 결재 대상자가 아닙니다.",
              "result": null
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결재 문서 또는 사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
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
        @Parameter(description = "승인할 결재 문서 ID", required = true, example = "1") @PathVariable Long id,
        @Parameter(description = "승인 요청 정보 (의견)", required = true) @RequestBody ApprovalProcessRequestDto requestDto,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        approvalService.approveApproval(id, userDetails.getId(), requestDto.getComment());

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(summary = "결재 문서 반려", description = """
        지정된 결재 문서를 반려합니다.

        **반려 조건**:
        - 요청자가 해당 결재 문서의 결재선(ApprovalStep)에 포함된 결재 대상자여야 합니다.
        - 요청자의 결재 순서(sequence)가 현재 차례여야 합니다 (PENDING 상태인 가장 낮은 sequence).

        **반려 후 동작**:
        - 문서 전체 상태가 즉시 REJECTED로 변경됩니다.
        """)
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "반려 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
            {
              "isSuccess": true,
              "code": "COMMON200",
              "message": "요청에 성공했습니다.",
              "result": null
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "이미 처리된 결재 단계", value = """
                {
                  "isSuccess": false,
                  "code": "ALREADY_PROCESSED_STEP",
                  "message": "이미 처리된 결재 단계입니다.",
                  "result": null
                }
                """),
            @ExampleObject(name = "결재 순서 아님", value = """
                {
                  "isSuccess": false,
                  "code": "NOT_YOUR_TURN",
                  "message": "아직 본인의 결재 순서가 아닙니다.",
                  "result": null
                }
                """)})),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "결재 권한 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
            {
              "isSuccess": false,
              "code": "NOT_APPROVER",
              "message": "해당 결재 문서의 결재 대상자가 아닙니다.",
              "result": null
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "결재 문서 또는 사용자를 찾을 수 없음", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
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
        @Parameter(description = "반려할 결재 문서 ID", required = true, example = "1") @PathVariable Long id,
        @Parameter(description = "반려 요청 정보 (반려 사유)", required = true) @RequestBody ApprovalProcessRequestDto requestDto,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        approvalService.rejectApproval(id, userDetails.getId(), requestDto.getComment());

        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
