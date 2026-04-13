package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "교육 수정 요청")
public class EduReportUpdateRequestDto {

    @Schema(description = "교육 제목", example = "2026년 상반기 보안 교육 (수정)")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "교육 내용", example = "개인정보 보호 및 사내 보안 규정 안내 (수정)")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private boolean pinned;

    @Schema(description = "서명 필요 여부", example = "true", defaultValue = "false")
    private boolean signatureRequired;

    @Schema(description = "부서 ID (부서 교육 수정 시 필수)", example = "3")
    private Long departmentId;

    @Schema(description = "카테고리 ID (PSM/안전보건 수정 시 선택)", example = "2")
    private Long categoryId;
}
