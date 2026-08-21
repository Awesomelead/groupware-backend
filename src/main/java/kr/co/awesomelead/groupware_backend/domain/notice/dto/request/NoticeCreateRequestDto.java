package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.NoticeCompanyJobTypeTargetDto;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({
    "title",
    "type",
    "pinned",
    "contentDelta",
    "contentHtml",
    "content",
    "targetCompanies",
    "targetCompanyJobTypes",
    "targetDepartmentIds",
    "targetUserIds"
})
@Schema(
        description = "공지사항 생성 요청",
        example =
                """
                {
                  "title": "2025년 1월 전체 회의 안내",
                  "type": "상시공지",
                  "pinned": false,
                  "contentDelta": "{\\"ops\\":[{\\"insert\\":\\"공지 본문입니다.\\\\n\\"}]}",
                  "contentHtml": "<p>오는 1월 15일 오후 2시에 전체 회의가 있습니다.</p>",
                  "content": "오는 1월 15일 오후 2시에 전체 회의가 있습니다.",
                  "targetCompanies": ["어썸리드"],
                  "targetCompanyJobTypes": [
                    { "company": "한국마루이", "jobType": "현장직" }
                  ],
                  "targetDepartmentIds": [1, 5, 12],
                  "targetUserIds": [101, 205]
                }
                """)
public class NoticeCreateRequestDto {

    @NotBlank(message = "공지사항 제목은 필수입니다.")
    @Schema(description = "공지사항 제목", example = "2025년 1월 전체 회의 안내", required = true)
    private String title;

    @Schema(
            description = "레거시 평문 본문(하위 호환용). 신규 개발은 contentDelta + contentHtml 사용 권장",
            example = "오는 1월 15일 오후 2시에 전체 회의가 있습니다.")
    private String content;

    @Schema(
            description = "Quill Delta JSON 문자열(에디터 원본). 프론트 저장 기준으로 권장",
            example = "{\"ops\":[{\"insert\":\"공지 본문입니다.\\n\"}]}")
    private String contentDelta;

    @Schema(
            description = "HTML 본문(선택). 상세 렌더링/미리보기 호환용",
            example = "<p>오는 1월 15일 오후 2시에 전체 회의가 있습니다.</p>")
    private String contentHtml;

    @NotNull(message = "공지 유형은 필수입니다.")
    @Schema(description = "공지 유형", example = "상시공지", required = true)
    private NoticeType type;

    @Builder.Default
    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private Boolean pinned = false;

    @Schema(
            description = "공지 대상 회사 목록 (해당 회사의 전사 공지 시 활용, MASTER_ADMIN 제외)",
            example = "[\"어썸리드\"]")
    private List<Company> targetCompanies;

    @Schema(
            description = "공지 대상 회사/직군 조건 목록 (예: 어썸리드 관리직, 한국마루이 현장직, MASTER_ADMIN 제외)")
    private List<NoticeCompanyJobTypeTargetDto> targetCompanyJobTypes;

    @Schema(
            description = "공지 대상 부서 ID 목록 (부서 및 하위 부서원 자동 포함, MASTER_ADMIN 제외)",
            example = "[1, 5, 12]")
    private List<Long> targetDepartmentIds;

    @Schema(description = "공지 대상 특정 유저 ID 목록 (개별 지정 시 활용, MASTER_ADMIN 제외)", example = "[101, 205]")
    private List<Long> targetUserIds;
}
