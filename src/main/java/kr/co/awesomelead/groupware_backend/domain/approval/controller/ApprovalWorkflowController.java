package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDraftUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalSubmitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalDraftResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalSubmitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalTemplateListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalWorkflowService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(
        name = "전자결재",
        description =
                """
        전자결재 작성 1차 API
        - 양식 목록 조회
        - 임시저장
        - 상신(상신 시점 결재선 스냅샷 고정)
        """)
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    @Operation(summary = "전자결재 양식 목록 조회")
    @GetMapping("/approval/templates")
    public ResponseEntity<ApiResponse<ApprovalTemplateListResponseDto>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.onSuccess(approvalWorkflowService.getTemplateList()));
    }

    @Operation(summary = "전자결재 임시저장")
    @PostMapping("/approvals/draft")
    public ResponseEntity<ApiResponse<ApprovalDraftResponseDto>> upsertDraft(
            @Valid @RequestBody ApprovalDraftUpsertRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalDraftResponseDto result =
                approvalWorkflowService.upsertDraft(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "전자결재 상신")
    @PostMapping("/approvals/{documentId}/submit")
    public ResponseEntity<ApiResponse<ApprovalSubmitResponseDto>> submit(
            @PathVariable Long documentId,
            @Valid @RequestBody ApprovalSubmitRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalSubmitResponseDto result =
                approvalWorkflowService.submit(userDetails.getId(), documentId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
