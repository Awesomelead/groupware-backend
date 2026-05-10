package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDirectSubmitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDraftCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDraftUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDraftUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalSubmitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalDraftResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalInboxAllResponseDto;
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
        - 전체 전체 탭: GET /api/approvals/all
        - 전체 본인기안 탭: GET /api/approvals/all/my-drafted
        - 전체 본인결재 탭: GET /api/approvals/all/my-approvals
        - 참조문서 참조문서 탭: GET /api/approvals/references
        - 참조문서 열람획득문서 탭: GET /api/approvals/references/viewer-acquired
        - 참조문서 열람부여문서 탭: GET /api/approvals/references/viewer-granted
        - 결재진행 전체 탭: GET /api/approvals/inbox/all
        - 결재진행 결재하기 탭: GET /api/approvals/inbox/to-approve
        - 결재진행 결재 전단계 탭: GET /api/approvals/inbox/before-my-turn
        - 결재진행 기결 탭: GET /api/approvals/inbox/processed-by-me
        - 결재진행 반려/회수 탭: GET /api/approvals/inbox/rejected-or-recalled
        - 결재진행 임시저장함 탭: GET /api/approvals/inbox/draft-box
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
            summary = "결재진행 전체 탭 조회",
            description =
                    """
            현재 사용자 기준 `결재진행 > 전체` 문서를 조회합니다.

            ### 조회 대상
            - 임시저장함: 내가 기안한 DRAFT 문서
            - 결재하기: IN_PROGRESS 이면서 내 결재선 상태가 PENDING 인 문서
            - 결재 전단계: IN_PROGRESS 이면서 내 결재선 상태가 WAITING 인 문서
            - 기결: 내가 결재(APPROVED) 처리한 결재선이 있는 문서
            - 반려: 문서 상태가 REJECTED 이고 내/부서 결재선이 실제 REJECTED 처리된 문서
            - 회수: 문서 상태가 RECALLED 이고 내가 기안한 문서

            ### 제외 대상
            - `전체 > 본인기안` 전용 문서(결재진행 조건 불충족)
            - 참조자/열람권자만 걸린 문서(참조문서 메뉴 대상)

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/inbox/all")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getInboxAll(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(approvalWorkflowService.getInboxAll(userDetails.getId())));
    }

    @Operation(
            summary = "결재진행 결재하기 탭 조회",
            description =
                    """
            현재 사용자 기준 `결재진행 > 결재하기` 문서를 조회합니다.

            ### 조회 대상
            - IN_PROGRESS 이면서 내/부서 결재선 상태가 PENDING 인 문서

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/inbox/to-approve")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getInboxToApprove(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getInboxToApprove(userDetails.getId())));
    }

    @Operation(
            summary = "결재진행 결재 전단계 탭 조회",
            description =
                    """
            현재 사용자 기준 `결재진행 > 결재 전단계` 문서를 조회합니다.

            ### 조회 대상
            - IN_PROGRESS 이면서 내/부서 결재선 상태가 WAITING 인 문서

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/inbox/before-my-turn")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getInboxBeforeMyTurn(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getInboxBeforeMyTurn(userDetails.getId())));
    }

    @Operation(
            summary = "결재진행 기결 탭 조회",
            description =
                    """
            현재 사용자 기준 `결재진행 > 기결` 문서를 조회합니다.

            ### 조회 대상
            - 내/부서 결재선이 APPROVED 처리된 문서

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/inbox/processed-by-me")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getInboxProcessedByMe(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getInboxProcessedByMe(userDetails.getId())));
    }

    @Operation(
            summary = "결재진행 반려/회수 탭 조회",
            description =
                    """
            현재 사용자 기준 `결재진행 > 반려/회수` 문서를 조회합니다.

            ### 조회 대상
            - 반려: 문서 상태 REJECTED 이고 내/부서 결재선이 실제 REJECTED 처리된 문서
            - 회수: 문서 상태 RECALLED 이고 내가 기안한 문서

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/inbox/rejected-or-recalled")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getInboxRejectedOrRecalled(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getInboxRejectedOrRecalled(userDetails.getId())));
    }

    @Operation(
            summary = "결재진행 임시저장함 탭 조회",
            description =
                    """
            현재 사용자 기준 `결재진행 > 임시저장함` 문서를 조회합니다.

            ### 조회 대상
            - 내가 기안한 DRAFT 문서

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/inbox/draft-box")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getInboxDraftBox(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getInboxDraftBox(userDetails.getId())));
    }

    @Operation(
            summary = "전체 전체 탭 조회",
            description =
                    """
            현재 사용자 기준 `전체 > 전체` 문서를 조회합니다.

            ### 조회 대상
            - 본인기안 문서(기안자가 본인)
            - 본인결재 문서(내 결재선/부서 결재선으로 지정된 문서, DRAFT 제외)
            - 위 두 조건의 합집합(중복 문서는 1건으로 표시)

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/all")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getAllAll(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(approvalWorkflowService.getAllAll(userDetails.getId())));
    }

    @Operation(
            summary = "전체 본인기안 탭 조회",
            description =
                    """
            현재 사용자 기준 `전체 > 본인기안` 문서를 조회합니다.

            ### 조회 대상
            - 기안자가 본인인 문서(상태 전체)

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/all/my-drafted")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getAllMyDrafted(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getAllMyDrafted(userDetails.getId())));
    }

    @Operation(
            summary = "전체 본인결재 탭 조회",
            description =
                    """
            현재 사용자 기준 `전체 > 본인결재` 문서를 조회합니다.

            ### 조회 대상
            - 내 결재선(사용자/부서)으로 지정된 문서
            - 임시저장(DRAFT) 문서는 제외

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/all/my-approvals")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getAllMyApprovals(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getAllMyApprovals(userDetails.getId())));
    }

    @Operation(
            summary = "참조문서 참조문서 탭 조회",
            description =
                    """
            현재 사용자 기준 `참조문서 > 참조문서` 문서를 조회합니다.

            ### 조회 대상
            - role=REFERENCE(참조자) 라인에 본인 사용자 또는 본인 부서가 지정된 문서
            - 임시저장(DRAFT) 문서는 제외

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/references")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getReferenceDocuments(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getReferenceDocuments(userDetails.getId())));
    }

    @Operation(
            summary = "참조문서 열람획득문서 탭 조회",
            description =
                    """
            현재 사용자 기준 `참조문서 > 열람획득문서` 문서를 조회합니다.

            ### 조회 대상
            - 문서 상태가 APPROVED(완결)
            - role=VIEWER(열람권자) 라인에 본인 사용자 또는 본인 부서가 지정된 문서

            ### 제외 대상
            - 임시저장(DRAFT) 문서
            - 동일 문서에 참조자(REFERENCE)로도 지정된 경우(참조문서 탭으로 분리)

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/references/viewer-acquired")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getViewerAcquiredDocuments(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getViewerAcquiredDocuments(userDetails.getId())));
    }

    @Operation(
            summary = "참조문서 열람부여문서 탭 조회",
            description =
                    """
            현재 사용자 기준 `참조문서 > 열람부여문서` 문서를 조회합니다.

            ### 조회 대상
            - 기안자가 본인인 문서
            - role=VIEWER(열람권자) 라인이 하나 이상 존재하는 문서

            ### 제외 대상
            - 임시저장(DRAFT) 문서

            ### 응답 주요 필드
            - 문서번호(documentNo)
            - 기안자(drafterName)
            - 제목(title)
            - 결재선(approvalLines)
            - 기안일(draftedAt)
            - 완료일(completedAt)
            """)
    @GetMapping("/approvals/references/viewer-granted")
    public ResponseEntity<ApiResponse<ApprovalInboxAllResponseDto>> getViewerGrantedDocuments(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalWorkflowService.getViewerGrantedDocuments(userDetails.getId())));
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
            @Valid @RequestBody ApprovalDraftCreateRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalDraftUpsertRequestDto upsertRequest = toUpsertRequest(request);
        upsertRequest.setDocumentId(null);
        ApprovalDraftResponseDto result =
                approvalWorkflowService.upsertDraft(userDetails.getId(), upsertRequest);
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
            @Valid @RequestBody ApprovalDraftUpdateRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalDraftUpsertRequestDto upsertRequest = toUpsertRequest(request);
        upsertRequest.setDocumentId(documentId);
        ApprovalDraftResponseDto result =
                approvalWorkflowService.upsertDraft(userDetails.getId(), upsertRequest);
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
            - 상신 시 문서번호가 자동 부여됩니다.
              - 기본: [양식이름] [기안부서] [yyyyMMdd]-[양식별순번]
              - 기본양식만 예외: [기안부서] [yyyyMMdd]-[양식별순번]
            - 상신 응답에는 문서번호/기안자/제목/결재선/기안일/완료일이 포함됩니다.
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
            - 상신 시 문서번호가 자동 부여됩니다.
              - 기본: [양식이름] [기안부서] [yyyyMMdd]-[양식별순번]
              - 기본양식만 예외: [기안부서] [yyyyMMdd]-[양식별순번]
            - 상신 응답에는 문서번호/기안자/제목/결재선/기안일/완료일이 포함됩니다.
            """)
    @PostMapping("/approvals/submit-direct")
    public ResponseEntity<ApiResponse<ApprovalSubmitResponseDto>> submitDirect(
            @Valid @RequestBody ApprovalDirectSubmitRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalSubmitResponseDto result =
                approvalWorkflowService.submitDirect(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    private ApprovalDraftUpsertRequestDto toUpsertRequest(ApprovalDraftCreateRequestDto request) {
        ApprovalDraftUpsertRequestDto upsert = new ApprovalDraftUpsertRequestDto();
        upsert.setTemplateId(request.getTemplateId());
        upsert.setTitle(request.getTitle());
        upsert.setContentDelta(request.getContentDelta());
        upsert.setContentHtml(request.getContentHtml());
        upsert.setApprovalType(request.getApprovalType());
        upsert.setReceiverDepartmentId(request.getReceiverDepartmentId());
        upsert.setLines(request.getLines());
        return upsert;
    }

    private ApprovalDraftUpsertRequestDto toUpsertRequest(ApprovalDraftUpdateRequestDto request) {
        ApprovalDraftUpsertRequestDto upsert = new ApprovalDraftUpsertRequestDto();
        upsert.setTemplateId(request.getTemplateId());
        upsert.setTitle(request.getTitle());
        upsert.setContentDelta(request.getContentDelta());
        upsert.setContentHtml(request.getContentHtml());
        upsert.setApprovalType(request.getApprovalType());
        upsert.setReceiverDepartmentId(request.getReceiverDepartmentId());
        upsert.setLines(request.getLines());
        return upsert;
    }
}
