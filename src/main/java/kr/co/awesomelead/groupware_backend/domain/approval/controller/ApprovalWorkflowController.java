package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDirectSubmitRequestDto;
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
import org.springframework.web.bind.annotation.PutMapping;
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
        - 임시저장 생성/수정
        - 상신(상신 시점 결재선 스냅샷 고정)

        ### 권장 엔드포인트(헷갈림 방지)
        - 임시저장 생성: POST /api/approvals/drafts
        - 임시저장 수정: PUT /api/approvals/drafts/{documentId}
        - 임시저장 문서 상신: POST /api/approvals/drafts/{documentId}/submit
        - 바로 상신(임시저장 없이 1회 요청): POST /api/approvals/submit-direct

        ### 권한 정보
        - 로그인 필요
        - 명시된 @PreAuthorize 없음 (서비스 레이어 권한 검증)

        ### 사용 Enum
        - Company
          - AWESOME (어썸리드)
          - MARUI (마루이)
        - JobType
          - FIELD (현장직)
          - MANAGEMENT (관리직)
        - Position
          - CEO (대표이사)
          - VICE_PRESIDENT (부사장)
          - SENIOR_MANAGING_DIRECTOR (전무이사)
          - MANAGING_DIRECTOR (상무이사)
          - DIRECTOR (이사)
          - GENERAL_MANAGER (부장)
          - DEPUTY_GENERAL_MANAGER (차장)
          - MANAGER (과장)
          - ASSISTANT_MANAGER (대리)
          - SENIOR_STAFF (주임)
          - STAFF (사원)
          - SECTION_HEAD (반장)
          - ADVISOR (전문위원)
          - SECURITY_GUARD (경비원)
        - Role
          - ADMIN (관리자)
          - USER (일반 사용자)
          - MASTER_ADMIN (마스터 관리자)
        - Status
          - PENDING
          - AVAILABLE
          - SUSPENDED
        - ApprovalType
          - INTERNAL (내부결재)
          - COOPERATIVE (협조결재)
        - ApprovalRouteRole
          - APPROVAL_LINE (결재선)
          - AGREEMENT_REQUIRED (합의부서 필수)
          - AGREEMENT_OPTIONAL (합의부서 선택)
          - REFERENCE (참조자)
          - VIEWER (열람권자)
          - RECEIVER_DEPARTMENT (수신부서)
        - ApprovalTargetType
          - USER (사용자)
          - DEPARTMENT (부서)
        """)
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    @Operation(summary = "전자결재 양식 목록 조회")
    @GetMapping("/approval/templates")
    public ResponseEntity<ApiResponse<ApprovalTemplateListResponseDto>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.onSuccess(approvalWorkflowService.getTemplateList()));
    }

    @Operation(
            summary = "전자결재 임시저장 생성",
            description =
                    """
            임시저장 문서를 생성합니다.

            ### 입력 규칙
            - templateId: 필수
            - title/contentDelta: 임시저장 단계에서는 선택 (상신 시 필수)
            - approvalType=COOPERATIVE일 때 receiverDepartmentId 지정 권장
            - lines 미입력 시 양식 기본 결재선이 자동 적용됩니다.
            - lines 입력 시 targetType=USER면 targetUserId, DEPARTMENT면 targetDepartmentId 필수입니다.
            """)
    @PostMapping("/approvals/drafts")
    public ResponseEntity<ApiResponse<ApprovalDraftResponseDto>> createDraft(
            @Valid @RequestBody ApprovalDraftUpsertRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        request.setDocumentId(null);
        ApprovalDraftResponseDto result =
                approvalWorkflowService.upsertDraft(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "전자결재 임시저장 수정",
            description =
                    """
            기존 임시저장 문서를 수정합니다.

            ### 입력 규칙
            - 수정 가능한 상태는 DRAFT(임시저장)만 허용됩니다.
            - lines 미입력 시 기존 결재선을 유지합니다.
            - lines 입력 시 전체 결재선 스냅샷이 교체됩니다.
            - lines 입력 시 targetType=USER면 targetUserId, DEPARTMENT면 targetDepartmentId 필수입니다.
            """)
    @PutMapping("/approvals/drafts/{documentId}")
    public ResponseEntity<ApiResponse<ApprovalDraftResponseDto>> updateDraft(
            @PathVariable Long documentId,
            @Valid @RequestBody ApprovalDraftUpsertRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        request.setDocumentId(documentId);
        ApprovalDraftResponseDto result =
                approvalWorkflowService.upsertDraft(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "전자결재 임시저장 문서 상신",
            description =
                    """
            이미 임시저장된 전자결재 문서를 상신합니다.

            - {documentId}: POST /api/approvals/drafts 로 생성한 임시저장 문서 ID
            - 임시저장 문서가 없으면 먼저 POST /api/approvals/drafts 로 생성해야 합니다.
            - 임시저장 없이 바로 상신하려면 POST /api/approvals/submit-direct 를 사용하세요.
            - 상신 시 title/contentDelta는 필수입니다.
            - approvalType=COOPERATIVE면 receiverDepartmentId를 반드시 지정해야 합니다.
            - lines 미입력 시 기존 임시저장 결재선을 사용하며, 없으면 양식 기본 결재선을 사용합니다.
            """)
    @PostMapping("/approvals/drafts/{documentId}/submit")
    public ResponseEntity<ApiResponse<ApprovalSubmitResponseDto>> submitDraft(
            @PathVariable Long documentId,
            @Valid @RequestBody ApprovalSubmitRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalSubmitResponseDto result =
                approvalWorkflowService.submit(userDetails.getId(), documentId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(
            summary = "전자결재 바로 상신(임시저장 없이)",
            description =
                    """
            임시저장 생성과 상신을 한 번에 처리합니다.

            - 내부 동작: 임시저장 생성 -> 상신
            - 결과로 최종 상신된 documentId를 반환합니다.
            - title/contentDelta는 필수입니다.
            - approvalType=COOPERATIVE면 receiverDepartmentId를 반드시 지정해야 합니다.
            - lines 미입력 시 양식 기본 결재선을 사용합니다.
            """)
    @PostMapping("/approvals/submit-direct")
    public ResponseEntity<ApiResponse<ApprovalSubmitResponseDto>> submitDirect(
            @Valid @RequestBody ApprovalDirectSubmitRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalSubmitResponseDto result =
                approvalWorkflowService.submitDirect(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
