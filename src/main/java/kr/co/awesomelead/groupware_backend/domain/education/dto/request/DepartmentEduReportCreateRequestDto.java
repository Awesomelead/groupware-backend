package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentEduReportCreateRequestDto {

    @Schema(description = "교육 제목", example = "경영지원부 월간 교육")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "교육 내용", example = "부서 운영 규정 및 공지사항 교육")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private boolean pinned;

    @Schema(description = "서명 필요 여부", example = "true", defaultValue = "false")
    private boolean signatureRequired;

    @Schema(description = "대상 부서 ID", example = "3")
    @NotNull(message = "부서 ID는 필수입니다.")
    private Long departmentId;
}
