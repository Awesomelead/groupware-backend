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
@Tag(name = "전자결재 양식관리", description = "전자결재 양식구분/양식 CRUD API")
public class AdminApprovalTemplateController {

    private final ApprovalTemplateAdminService approvalTemplateAdminService;

    @Operation(summary = "전자결재 양식구분 생성")
    @PostMapping("/approval-template-categories")
    public ResponseEntity<ApiResponse<Long>> createCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ApprovalTemplateCategoryCreateRequestDto request) {
        Long id = approvalTemplateAdminService.createCategory(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(id));
    }

    @Operation(summary = "전자결재 양식구분 목록 조회")
    @GetMapping("/approval-template-categories")
    public ResponseEntity<ApiResponse<List<ApprovalTemplateCategoryResponseDto>>> getCategories(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(approvalTemplateAdminService.getCategories(userDetails.getId())));
    }

    @Operation(summary = "전자결재 양식구분 상세 조회")
    @GetMapping("/approval-template-categories/{categoryId}")
    public ResponseEntity<ApiResponse<ApprovalTemplateCategoryResponseDto>> getCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalTemplateAdminService.getCategory(userDetails.getId(), categoryId)));
    }

    @Operation(summary = "전자결재 양식구분 수정")
    @PutMapping("/approval-template-categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId,
            @Valid @RequestBody ApprovalTemplateCategoryUpdateRequestDto request) {
        approvalTemplateAdminService.updateCategory(userDetails.getId(), categoryId, request);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식구분이 수정되었습니다."));
    }

    @Operation(summary = "전자결재 양식구분 삭제")
    @DeleteMapping("/approval-template-categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId) {
        approvalTemplateAdminService.deleteCategory(userDetails.getId(), categoryId);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식구분이 삭제되었습니다."));
    }

    @Operation(summary = "전자결재 양식 생성")
    @PostMapping("/approval-templates")
    public ResponseEntity<ApiResponse<Long>> createTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ApprovalTemplateCreateRequestDto request) {
        Long id = approvalTemplateAdminService.createTemplate(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(id));
    }

    @Operation(summary = "전자결재 양식 목록 조회")
    @GetMapping("/approval-templates")
    public ResponseEntity<ApiResponse<List<ApprovalTemplateAdminResponseDto>>> getTemplates(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(approvalTemplateAdminService.getTemplates(userDetails.getId())));
    }

    @Operation(summary = "전자결재 양식 상세 조회")
    @GetMapping("/approval-templates/{templateId}")
    public ResponseEntity<ApiResponse<ApprovalTemplateAdminResponseDto>> getTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long templateId) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalTemplateAdminService.getTemplate(userDetails.getId(), templateId)));
    }

    @Operation(summary = "전자결재 양식 수정")
    @PutMapping("/approval-templates/{templateId}")
    public ResponseEntity<ApiResponse<Void>> updateTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long templateId,
            @Valid @RequestBody ApprovalTemplateUpdateRequestDto request) {
        approvalTemplateAdminService.updateTemplate(userDetails.getId(), templateId, request);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식이 수정되었습니다."));
    }

    @Operation(summary = "전자결재 양식 삭제")
    @DeleteMapping("/approval-templates/{templateId}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long templateId) {
        approvalTemplateAdminService.deleteTemplate(userDetails.getId(), templateId);
        return ResponseEntity.ok(ApiResponse.onNoContent("전자결재 양식이 삭제되었습니다."));
    }
}
