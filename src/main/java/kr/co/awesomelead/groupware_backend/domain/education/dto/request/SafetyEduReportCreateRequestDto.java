package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafetyEduReportCreateRequestDto {

    @Schema(description = "교육 제목", example = "안전 보건 정기교육")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(
            description = "레거시 평문 본문(하위 호환용). 신규 개발은 contentDelta + contentHtml 사용 권장",
            example = "사업장 안전수칙 및 보호구 착용 기준 안내")
    private String content;

    @Schema(
            description = "Quill Delta JSON 문자열(에디터 원본)",
            example = "{\"ops\":[{\"insert\":\"안전보건 교육 본문입니다.\\n\"}]}")
    private String contentDelta;

    @Schema(description = "HTML 본문(렌더링/미리보기용)", example = "<p>사업장 안전수칙 및 보호구 착용 기준 안내</p>")
    private String contentHtml;

    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private boolean pinned;

    @Schema(description = "카테고리 ID (안전 보건 카테고리)", example = "1")
    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @Schema(
            description =
                    "대상 회사 범위 (예: [AWESOME], [AWESOME, MARUI], null/빈 배열이면 모든 회사 공통 게시물,"
                            + " 알림 대상자 산정 시 MASTER_ADMIN 제외)",
            example = "[\"AWESOME\", \"MARUI\"]",
            nullable = true)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Company> companyScope;
}
