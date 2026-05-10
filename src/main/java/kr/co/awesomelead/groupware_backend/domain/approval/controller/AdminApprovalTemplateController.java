package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateCategoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateCategoryUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalTemplateUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalTemplateAdminResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalTemplateCategoryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalTemplateAdminService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(
        name = "전자결재 양식관리",
        description =
                """
        전자결재 양식구분/양식 CRUD API

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
        - ApprovalLinePolicy
          - FIXED (고정 결재선)
          - FLEXIBLE (가변 결재선)
        - ApprovalEditorType
          - QUILL
          - HTML
          - EXCEL
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
public class AdminApprovalTemplateController {

    private final ApprovalTemplateAdminService approvalTemplateAdminService;

    @Operation(
            summary = "전자결재 양식구분 생성",
            description =
                    """
            전자결재 양식구분을 생성합니다.

            ### 필수 입력
            - code: 고유 코드
            - name: 구분명
            - sortOrder: 정렬순서

            ### 응답
            - 생성된 양식구분 ID 반환
            """)
    @PostMapping("/approval-template-categories")
    public ResponseEntity<ApiResponse<Long>> createCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ApprovalTemplateCategoryCreateRequestDto request) {
        Long id = approvalTemplateAdminService.createCategory(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(id));
    }

    @Operation(
            summary = "전자결재 양식구분 목록 조회",
            description =
                    """
            전자결재 양식구분 목록을 조회합니다.

            ### 응답 필드
            - id, code, name, sortOrder, isActive
            """)
    @GetMapping("/approval-template-categories")
    public ResponseEntity<ApiResponse<List<ApprovalTemplateCategoryResponseDto>>> getCategories(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalTemplateAdminService.getCategories(userDetails.getId())));
    }

    @Operation(
            summary = "전자결재 양식구분 상세 조회",
            description =
                    """
            특정 양식구분의 상세 정보를 조회합니다.

            ### Path
            - categoryId: 양식구분 ID
            """)
    @GetMapping("/approval-template-categories/{categoryId}")
    public ResponseEntity<ApiResponse<ApprovalTemplateCategoryResponseDto>> getCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalTemplateAdminService.getCategory(userDetails.getId(), categoryId)));
    }

    @Operation(
            summary = "전자결재 양식구분 수정",
            description =
                    """
            특정 양식구분을 수정합니다.

            ### Path
            - categoryId: 수정 대상 양식구분 ID

            ### 수정 가능 필드
            - code, name, sortOrder, isActive
            """)
    @PutMapping("/approval-template-categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId,
            @Valid @RequestBody ApprovalTemplateCategoryUpdateRequestDto request) {
        approvalTemplateAdminService.updateCategory(userDetails.getId(), categoryId, request);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식구분이 수정되었습니다."));
    }

    @Operation(
            summary = "전자결재 양식구분 삭제",
            description =
                    """
            특정 양식구분을 삭제합니다.

            ### 주의
            - 해당 양식구분에 연결된 양식이 존재하면 삭제가 제한될 수 있습니다.
            """)
    @DeleteMapping("/approval-template-categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId) {
        approvalTemplateAdminService.deleteCategory(userDetails.getId(), categoryId);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식구분이 삭제되었습니다."));
    }

    @Operation(
            summary = "전자결재 양식 생성",
            description =
                    """
            전자결재 양식을 생성합니다.

            ### 입력 규칙
            - linePolicy=FIXED면 lines에 최소 1개 이상의 APPROVAL_LINE이 필요합니다.
            - linePolicy=FLEXIBLE면 lines는 비워둘 수 있습니다.
            - lines 항목에서 targetType=USER면 targetUserId, DEPARTMENT면 targetDepartmentId 필수입니다.
            """)
    @PostMapping("/approval-templates")
    public ResponseEntity<ApiResponse<Long>> createTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ApprovalTemplateCreateRequestDto request) {
        Long id = approvalTemplateAdminService.createTemplate(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(id));
    }

    @Operation(
            summary = "전자결재 양식 목록 조회",
            description =
                    """
            전자결재 양식 목록을 조회합니다.

            ### 응답 필드
            - 기본정보: id, categoryId/categoryName, code, name, description
            - 작성정보: editorType, approvalType, linePolicy
            - 본문기본값: defaultContentDelta
            - 기본 라인: lines(role/targetType/targetUserId/targetDepartmentId/sequenceNo/required)
            """)
    @GetMapping("/approval-templates")
    public ResponseEntity<ApiResponse<List<ApprovalTemplateAdminResponseDto>>> getTemplates(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalTemplateAdminService.getTemplates(userDetails.getId())));
    }

    @Operation(
            summary = "전자결재 양식 상세 조회",
            description =
                    """
            특정 전자결재 양식 상세를 조회합니다.

            ### Path
            - templateId: 양식 ID

            ### 응답
            - 양식 기본정보 + 기본 결재라인(lines)
            """)
    @GetMapping("/approval-templates/{templateId}")
    public ResponseEntity<ApiResponse<ApprovalTemplateAdminResponseDto>> getTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long templateId) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalTemplateAdminService.getTemplate(userDetails.getId(), templateId)));
    }

    @Operation(
            summary = "전자결재 양식 수정",
            description =
                    """
            전자결재 양식을 수정합니다.

            ### 입력 규칙
            - linePolicy=FIXED면 lines에 최소 1개 이상의 APPROVAL_LINE이 필요합니다.
            - linePolicy=FLEXIBLE면 lines는 비워둘 수 있습니다.
            - lines 항목에서 targetType=USER면 targetUserId, DEPARTMENT면 targetDepartmentId 필수입니다.
            """)
    @PutMapping("/approval-templates/{templateId}")
    public ResponseEntity<ApiResponse<Void>> updateTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long templateId,
            @Valid @RequestBody ApprovalTemplateUpdateRequestDto request) {
        approvalTemplateAdminService.updateTemplate(userDetails.getId(), templateId, request);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식이 수정되었습니다."));
    }

    @Operation(
            summary = "전자결재 양식 삭제",
            description =
                    """
            특정 전자결재 양식을 삭제합니다.

            ### 주의
            - 이미 상신된 문서와의 참조 관계가 있으면 삭제가 제한될 수 있습니다.
            """)
    @DeleteMapping("/approval-templates/{templateId}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long templateId) {
        approvalTemplateAdminService.deleteTemplate(userDetails.getId(), templateId);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식이 삭제되었습니다."));
    }
}
