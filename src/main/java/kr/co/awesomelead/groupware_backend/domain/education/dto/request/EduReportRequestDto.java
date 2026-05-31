package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EduReportRequestDto {

    @Schema(
            description = "교육 유형",
            example = "부서 교육",
            allowableValues = {"PSM", "안전 보건", "부서 교육"})
    @NotNull(message = "교육 유형은 필수입니다.")
    private EduType eduType;

    @Schema(description = "교육 제목", example = "2026년 상반기 보안 교육")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "교육 내용", example = "개인정보 보호 및 사내 보안 규정 안내")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(
            description = "Quill Delta JSON 문자열(에디터 원본, 선택)",
            example = "{\"ops\":[{\"insert\":\"PSM 교육 본문입니다.\\n\"}]}")
    private String contentDelta;

    @Schema(
            description = "HTML 본문(선택)",
            example = "<p>PSM 교육 본문입니다.</p>")
    private String contentHtml;

    @Schema(description = "본문 검색용 평문(서버에서 생성, 내부 전달용)")
    private String contentText;

    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private boolean pinned;

    @Schema(description = "서명 필요 여부", example = "true", defaultValue = "false")
    private boolean signatureRequired;

    @Schema(description = "부서 ID (부서 교육인 경우 필수)", example = "3")
    private Long departmentId; // 부서교육인 경우에만 작성

    @Schema(description = "카테고리 ID (PSM/안전 보건 교육 생성 시 필수)", example = "1")
    private Long categoryId;
}
