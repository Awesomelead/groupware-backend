package kr.co.awesomelead.groupware_backend.domain.education.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EducationCategoryNodeDto;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EducationCategoryType;
import kr.co.awesomelead.groupware_backend.domain.education.service.EducationCategoryService;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/education-categories")
@Tag(name = "Education Category", description = "PSM/안전보건 카테고리 조회 API")
public class EducationCategoryController {

    private final EducationCategoryService educationCategoryService;

    @Operation(summary = "교육 카테고리 트리 조회", description = "유형(PSM/SAFETY)에 해당하는 카테고리 트리를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EducationCategoryNodeDto>>> getCategoryTree(
            @Parameter(description = "카테고리 유형", example = "PSM", required = true) @RequestParam
                    EducationCategoryType type) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(educationCategoryService.getCategoryTree(type)));
    }
}
