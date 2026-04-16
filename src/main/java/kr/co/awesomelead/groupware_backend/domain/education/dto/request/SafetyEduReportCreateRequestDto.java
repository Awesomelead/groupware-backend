package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyEduReportCreateRequestDto {

    @Schema(description = "교육 제목", example = "안전 보건 정기교육")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "교육 내용", example = "사업장 안전수칙 및 보호구 착용 기준 안내")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private boolean pinned;

    @Schema(description = "카테고리 ID (안전 보건 카테고리)", example = "1")
    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @Schema(
            description = "대상 회사 (null이면 모든 회사 공통 게시물)",
            example = "AWESOME",
            nullable = true,
            allowableValues = {"AWESOME", "MARUI"})
    private Company companyScope;
}
