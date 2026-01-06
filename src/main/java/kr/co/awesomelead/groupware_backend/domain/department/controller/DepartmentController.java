package kr.co.awesomelead.groupware_backend.domain.department.controller;

import io.swagger.v3.oas.annotations.Operation;

import kr.co.awesomelead.groupware_backend.domain.department.dto.response.DepartmentHierarchyResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.UserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.service.DepartmentService;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "부서 계층 구조 조회", description = "회사의 전체 부서 구조를 트리 형태로 조회합니다.")
    @GetMapping("/hierarchy")
    public ResponseEntity<ApiResponse<List<DepartmentHierarchyResponseDto>>> getHierarchy(
            @RequestParam Company company) {

        List<DepartmentHierarchyResponseDto> hierarchy =
                departmentService.getDepartmentHierarchy(company);

        return ResponseEntity.ok(ApiResponse.onSuccess(hierarchy));
    }

    @Operation(summary = "부서 및 하위 부서 사용자 조회", description = "지정한 부서와 그 하위 부서에 속한 모든 사용자를 조회합니다.")
    @GetMapping("/{departmentId}/users")
    public ResponseEntity<ApiResponse<List<UserSummaryResponseDto>>> getUsersByHierarchy(
            @PathVariable Long departmentId) {

        List<UserSummaryResponseDto> users =
                departmentService.getUsersByDepartmentHierarchy(departmentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(users));
    }
}
