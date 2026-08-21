package kr.co.awesomelead.groupware_backend.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지 대상 회사/직군 조건")
public class NoticeCompanyJobTypeTargetDto {

    @Schema(description = "대상 회사", example = "어썸리드")
    private Company company;

    @Schema(description = "대상 직군", example = "관리직")
    private JobType jobType;
}
