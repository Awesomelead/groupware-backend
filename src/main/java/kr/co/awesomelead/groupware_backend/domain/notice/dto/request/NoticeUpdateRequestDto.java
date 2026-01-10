package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "공지사항 수정 요청")
public class NoticeUpdateRequestDto {

    @Schema(description = "공지사항 제목 (수정하지 않으려면 null)", example = "2025년 1월 전체 회의 안내 (수정)")
    private String title;

    @Schema(description = "공지사항 내용 (수정하지 않으려면 null)", example = "회의 시간이 오후 3시로 변경되었습니다.")
    private String content;

    @Schema(description = "상단 고정 여부 (수정하지 않으려면 null)", example = "true")
    private Boolean pinned;

    @Schema(description = "삭제할 첨부파일 ID 목록", example = "[1, 2, 3]")
    private List<Long> attachmentsIdsToRemove;
}