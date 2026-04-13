package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduReportStatus;
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

    @Schema(description = "게시물 ID", example = "101")
    private Long id;

    @Schema(description = "제목", example = "2026년 상반기 보안 교육")
    private String title;

    @Schema(description = "교육 유형", example = "부서 교육")
    private EduType eduType;

    @Schema(description = "교육 날짜", example = "2026-03-15")
    private LocalDate eduDate;

    @Schema(description = "교육 내용", example = "이번 교육에서는 안전 수칙에 대해 다룹니다.")
    private String content;

    @Schema(description = "출석 여부", example = "true")
    private boolean attendance;

    @Schema(description = "상단 고정 여부", example = "false")
    private boolean pinned;

    @Schema(description = "서명 필수 여부", example = "false")
    private boolean signatureRequired;

    @Schema(description = "교육 상태", example = "OPEN")
    private EduReportStatus status;

    @Schema(description = "교육 카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "교육 카테고리명", example = "유해위험물질")
    private String categoryName;
}
