package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "회사 전체 선택 옵션")
public class OrganizationCompanyOptionResponseDto {

    @Schema(description = "회사 Enum", example = "AWESOME")
    private Company company;

    @Schema(description = "회사명", example = "어썸리드")
    private String companyName;
}
