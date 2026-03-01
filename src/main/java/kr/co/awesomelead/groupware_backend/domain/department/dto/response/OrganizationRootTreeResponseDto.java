package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "조직도 루트 트리")
public class OrganizationRootTreeResponseDto {

    @Schema(description = "루트 부서 노드")
    private OrganizationDepartmentNodeResponseDto rootDepartment;

    @Schema(description = "회사 전체 선택 옵션 목록")
    private List<OrganizationCompanyOptionResponseDto> companyOptions;
}
