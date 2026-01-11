package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class EduReportSummaryDto {

    @Schema(description = "보고서 ID", example = "101")
    private Long id;

    @Schema(description = "제목", example = "2026년 상반기 보안 교육")
    private String title;

    @Schema(description = "교육 유형", example = "MANDATORY")
    private EduType eduType;

    @Schema(description = "교육 날짜", example = "2026-03-15")
    private LocalDate eduDate;

    @Schema(description = "출석 여부", example = "true")
    private boolean attendance;

    @Schema(description = "상단 고정 여부", example = "false")
    private boolean pinned;
}
