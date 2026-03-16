package kr.co.awesomelead.groupware_backend.domain.education.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EducationCategoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EducationCategoryReorderRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EducationCategoryUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.service.AdminEducationCategoryService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/education-categories")
@Tag(name = "Admin Education Category", description = "관리자용 교육 카테고리 관리 API")
public class AdminEducationCategoryController {

    private final AdminEducationCategoryService adminEducationCategoryService;

    @Operation(summary = "교육 카테고리 생성", description = "관리자가 교육 카테고리를 생성합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody EducationCategoryCreateRequestDto requestDto) {
        Long id = adminEducationCategoryService.createCategory(userDetails.getId(), requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(id));
    }

    @Operation(summary = "교육 카테고리 수정", description = "관리자가 교육 카테고리를 수정합니다.")
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> updateCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId,
            @Valid @RequestBody EducationCategoryUpdateRequestDto requestDto) {
        adminEducationCategoryService.updateCategory(userDetails.getId(), categoryId, requestDto);
        return ResponseEntity.ok(ApiResponse.onNoContent("교육 카테고리가 수정되었습니다."));
    }

    @Operation(summary = "교육 카테고리 비활성화", description = "관리자가 교육 카테고리를 비활성화합니다.")
    @PatchMapping("/{categoryId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId) {
        adminEducationCategoryService.deactivateCategory(userDetails.getId(), categoryId);
        return ResponseEntity.ok(ApiResponse.onNoContent("교육 카테고리가 비활성화되었습니다."));
    }

    @Operation(summary = "교육 카테고리 활성화", description = "관리자가 교육 카테고리를 활성화합니다.")
    @PatchMapping("/{categoryId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateCategory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId) {
        adminEducationCategoryService.activateCategory(userDetails.getId(), categoryId);
        return ResponseEntity.ok(ApiResponse.onNoContent("교육 카테고리가 활성화되었습니다."));
    }

    @Operation(summary = "교육 카테고리 정렬 변경", description = "같은 부모를 가진 카테고리들의 정렬 순서를 변경합니다.")
    @PatchMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderCategories(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody EducationCategoryReorderRequestDto requestDto) {
        adminEducationCategoryService.reorderCategories(userDetails.getId(), requestDto);
        return ResponseEntity.ok(ApiResponse.onNoContent("교육 카테고리 정렬이 변경되었습니다."));
    }
}
