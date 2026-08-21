package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;

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
    "content",
    "targetCompanies",
    "targetCompanyJobTypes",
    "targetDepartmentIds",
    "targetUserIds",
    "attachmentsIdsToRemove"
})
@Schema(
        description = "공지사항 수정 요청",
        example =
                """
                {
                  "title": "2025년 1월 전체 회의 안내 (수정)",
                  "type": "상시공지",
                  "pinned": true,
                  "content": "회의 시간이 오후 3시로 변경되었습니다.",
                  "targetCompanies": ["어썸리드"],
                  "targetCompanyJobTypes": [
                    { "company": "한국마루이", "jobType": "현장직" }
                  ],
                  "targetDepartmentIds": [11, 12],
                  "targetUserIds": [17, 22],
                  "attachmentsIdsToRemove": [1, 2]
                }
                """)
public class NoticeUpdateRequestDto {

    @Schema(description = "공지사항 제목 (수정하지 않으려면 null)", example = "2025년 1월 전체 회의 안내 (수정)")
    private String title;

    @Schema(description = "공지사항 본문 (수정하지 않으려면 null)", example = "회의 시간이 오후 3시로 변경되었습니다.")
    private String content;

    @Schema(description = "공지 유형 (수정하지 않으려면 null)", example = "상시공지")
    private NoticeType type;

    @Schema(description = "상단 고정 여부 (수정하지 않으려면 null)", example = "true")
    private Boolean pinned;

    @Schema(description = "공지 대상 회사 목록 (수정하지 않으려면 null, 전체 초기화하려면 [], MASTER_ADMIN 제외)")
    private List<Company> targetCompanies;

    @Schema(description = "공지 대상 회사/직군 조건 목록 (수정하지 않으려면 null, 전체 초기화하려면 [], MASTER_ADMIN 제외)")
    private List<NoticeCompanyJobTypeTargetDto> targetCompanyJobTypes;

    @Schema(description = "공지 대상 부서 ID 목록 (수정하지 않으려면 null, 전체 초기화하려면 [], MASTER_ADMIN 제외)")
    private List<Long> targetDepartmentIds;

    @Schema(description = "공지 대상 특정 유저 ID 목록 (수정하지 않으려면 null, 전체 초기화하려면 [], MASTER_ADMIN 제외)")
    private List<Long> targetUserIds;

    @Schema(description = "삭제할 첨부파일 ID 목록", example = "[1, 2, 3]")
    private List<Long> attachmentsIdsToRemove;
}
