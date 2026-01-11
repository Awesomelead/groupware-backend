package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EduReportRequestDto {

    @Schema(
            description = "교육 유형",
            example = "LEGAL",
            allowableValues = {"LEGAL", "JOB", "DEPARTMENT", "OTHER"})
    @NotNull(message = "교육 유형은 필수입니다.")
    private EduType eduType;

    @Schema(description = "교육 제목", example = "2026년 상반기 보안 교육")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "교육 내용", example = "개인정보 보호 및 사내 보안 규정 안내")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "교육 실시 일자", example = "2026-01-11")
    @NotNull(message = "교육 날짜는 필수입니다.")
    private LocalDate eduDate;

    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private boolean pinned;

    @Schema(description = "서명 필요 여부", example = "true", defaultValue = "false")
    private boolean signatureRequired;

    @Schema(description = "부서 ID (부서 교육인 경우 필수)", example = "3")
    private Long departmentId; // 부서교육인 경우에만 작성
}
