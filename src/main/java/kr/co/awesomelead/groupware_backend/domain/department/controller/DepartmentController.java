package kr.co.awesomelead.groupware_backend.domain.department.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import kr.co.awesomelead.groupware_backend.domain.department.dto.response.DepartmentHierarchyResponseDto;
import kr.co.awesomelead.groupware_backend.domain.department.dto.response.OrganizationRootTreeResponseDto;
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
@Tag(
        name = "Department",
        description =
                """
            ## 부서 관리 API

            회사의 부서 계층 구조 조회 및 특정 부서에 속한 사용자 목록 조회 기능을 제공합니다.

            ### 사용되는 Enum 타입
            - **Company**: 회사 구분 (예: AWESOMELEAD, MARUI 등 - 프로젝트 정의에 따름)

            ### 주요 기능
            - 전체 부서 트리 구조 조회
            - 특정 부서 및 그 하위 부서에 포함된 모든 사용자 목록 조회
            """)
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "조직도 트리 조회", description = "충남사업본부 기준 부서 트리와 회사 전체 선택 옵션을 함께 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조직도 조회 성공",
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
                                    "rootDepartment": {
                                      "id": 1,
                                      "code": "CHUNGNAM_HQ",
                                      "label": "충남사업본부",
                                      "users": [],
                                      "children": [
                                        {
                                          "id": 12,
                                          "code": "MANAGEMENT_SUPPORT",
                                          "label": "경영지원부",
                                          "users": [
                                            {
                                              "id": 17,
                                              "name": "홍길동",
                                              "position": "사원",
                                              "status": "AVAILABLE"
                                            }
                                          ],
                                          "children": []
                                        }
                                      ]
                                    },
                                    "companyOptions": [
                                      {
                                        "company": "어썸리드",
                                        "companyName": "어썸리드"
                                      },
                                      {
                                        "company": "마루이",
                                        "companyName": "마루이"
                                      }
                                    ]
                                  }
                                }
                                """)))
            })
    @GetMapping("/organization-tree")
    public ResponseEntity<ApiResponse<OrganizationRootTreeResponseDto>> getOrganizationTree() {
        OrganizationRootTreeResponseDto result = departmentService.getOrganizationTree();
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }

    @Operation(summary = "부서 계층 구조 조회", description = "회사의 전체 부서 구조를 트리 형태로 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "계층 구조 조회 성공",
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
                                  "result": [
                                    {
                                      "id": 1,
                                      "name": "경영지원본부",
                                      "children": [
                                        {
                                          "id": 2,
                                          "name": "인사팀",
                                          "children": []
                                        },
                                        {
                                          "id": 3,
                                          "name": "재무팀",
                                          "children": []
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """)))
            })
    @GetMapping("/hierarchy")
    public ResponseEntity<ApiResponse<List<DepartmentHierarchyResponseDto>>> getHierarchy(
            @Parameter(description = "조회할 회사 구분", example = "AWESOMELEAD", required = true)
                    @RequestParam
                    Company company) {

        List<DepartmentHierarchyResponseDto> hierarchy =
                departmentService.getDepartmentHierarchy(company);

        return ResponseEntity.ok(ApiResponse.onSuccess(hierarchy));
    }

    @Operation(summary = "부서 및 하위 부서 사용자 조회", description = "지정한 부서와 그 하위 부서에 속한 모든 사용자를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "사용자 목록 조회 성공",
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
                                  "result": [
                                    {
                                      "userId": 1,
                                      "name": "홍길동",
                                      "position": "팀장",
                                      "departmentName": "인사팀"
                                    },
                                    {
                                      "userId": 5,
                                      "name": "김철수",
                                      "position": "사원",
                                      "departmentName": "인사팀"
                                    }
                                  ]
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "부서를 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                  "isSuccess": false,
                                  "code": "DEPARTMENT_NOT_FOUND",
                                  "message": "해당 부서를 찾을 수 없습니다.",
                                  "result": null
                                }
                                """)))
            })
    @GetMapping("/{departmentId}/users")
    public ResponseEntity<ApiResponse<List<UserSummaryResponseDto>>> getUsersByHierarchy(
            @Parameter(description = "기준 부서 ID", example = "1", required = true) @PathVariable
                    Long departmentId) {

        List<UserSummaryResponseDto> users =
                departmentService.getUsersByDepartmentHierarchy(departmentId);

        return ResponseEntity.ok(ApiResponse.onSuccess(users));
    }
}
