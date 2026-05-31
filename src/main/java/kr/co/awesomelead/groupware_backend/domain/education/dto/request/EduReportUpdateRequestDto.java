package kr.co.awesomelead.groupware_backend.domain.education.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

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
@Schema(description = "교육 수정 요청")
public class EduReportUpdateRequestDto {

    @Schema(description = "교육 제목", example = "2026년 상반기 보안 교육 (수정)")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(
            description = "레거시 평문 본문(하위 호환용). PSM은 contentDelta/contentHtml과 함께 사용할 수 있습니다.",
            example = "개인정보 보호 및 사내 보안 규정 안내 (수정)")
    private String content;

    @Schema(
            description = "Quill Delta JSON 문자열(에디터 원본, 선택)",
            example = "{\"ops\":[{\"insert\":\"PSM 수정 본문입니다.\\n\"}]}")
    private String contentDelta;

    @Schema(
            description = "HTML 본문(선택)",
            example = "<p>PSM 수정 본문입니다.</p>")
    private String contentHtml;

    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private boolean pinned;

    @Schema(description = "서명 필요 여부", example = "true", defaultValue = "false")
    private boolean signatureRequired;

    @Schema(description = "부서 ID (부서 교육 수정 시 필수)", example = "3")
    private Long departmentId;

    @Schema(description = "카테고리 ID (PSM/안전보건 수정 시 선택)", example = "2")
    private Long categoryId;

    @Schema(
            description = "회사 범위(PSM/안전보건 수정 시 선택)." + " [AWESOME, MARUI]를 함께 넣으면 공통 게시물로 저장됩니다.",
            example = "[\"AWESOME\"]")
    private List<Company> companyScope;

    @Schema(description = "삭제할 첨부파일 ID 목록(선택)", example = "[10, 11]")
    private List<Long> deleteAttachmentIds;
}
