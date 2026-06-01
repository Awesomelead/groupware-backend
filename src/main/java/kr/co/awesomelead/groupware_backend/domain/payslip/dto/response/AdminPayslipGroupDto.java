package kr.co.awesomelead.groupware_backend.domain.payslip.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "관리자용 급여명세서 월별 그룹 응답")
public class AdminPayslipGroupDto {

    @Schema(description = "급여지급월(YYYYMM)", example = "202606")
    private String yearMonth;

    @Schema(description = "월별 그룹 제목", example = "2026년 6월")
    private String title;

    @Schema(description = "해당 월 명세서 건수", example = "18")
    private int totalCount;

    @Schema(description = "해당 월 명세서 목록")
    private List<AdminPayslipSummaryDto> items;
}
