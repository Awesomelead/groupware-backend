package kr.co.awesomelead.groupware_backend.domain.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "공지사항 목록 조회 응답")
public class NoticeSummaryDto {

    @Schema(description = "공지사항 ID", example = "1")
    private Long id;

    @Schema(description = "공지사항 유형", example = "상시공지")
    private NoticeType type;

    @Schema(description = "공지사항 제목", example = "2025년 1월 전체 회의 안내")
    private String title;

    @Schema(description = "수정일시", example = "2025-01-10T14:30:00")
    private LocalDateTime updatedDate;
}
