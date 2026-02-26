package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "조직도 부서 노드")
public class OrganizationDepartmentNodeResponseDto {

    @Schema(description = "부서 ID", example = "12")
    private Long id;

    @Schema(description = "부서 코드", example = "MANAGEMENT_SUPPORT")
    private String code;

    @Schema(description = "부서명", example = "경영지원부")
    private String label;

    @Schema(description = "소속 회사", example = "AWESOME")
    private Company company;

    @Schema(description = "해당 부서 사용자 목록")
    private List<OrganizationUserNodeResponseDto> users;

    @Schema(description = "하위 부서 목록")
    private List<OrganizationDepartmentNodeResponseDto> children;
}
