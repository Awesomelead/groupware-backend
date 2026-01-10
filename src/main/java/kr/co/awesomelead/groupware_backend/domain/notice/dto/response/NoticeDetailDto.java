package kr.co.awesomelead.groupware_backend.domain.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "공지사항 상세 조회 응답")
public class NoticeDetailDto {

    @Schema(description = "공지사항 ID", example = "1")
    private Long id;

    @Schema(description = "공지사항 제목", example = "2025년 1월 전체 회의 안내")
    private String title;

    @Schema(description = "공지사항 내용", example = "오는 1월 15일 오후 2시에 전체 회의가 있습니다.")
    private String content;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String authorName;

    @Schema(description = "수정일시", example = "2025-01-10T14:30:00")
    private LocalDateTime updatedDate;

    @Schema(description = "조회수", example = "42")
    private int viewCount;

    @Schema(description = "첨부파일 목록")
    private List<AttachmentResponse> attachments;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "첨부파일 정보")
    public static class AttachmentResponse {

        @Schema(description = "첨부파일 ID", example = "1")
        private Long id;

        @Schema(description = "원본 파일명", example = "회의자료.pdf")
        private String originalFileName;

        @Schema(description = "파일 크기 (bytes)", example = "1048576")
        private long fileSize;

        @Schema(
                description = "파일 조회 URL",
                example = "https://bucket.s3.amazonaws.com/notices/uuid_file.pdf")
        private String viewUrl; // S3에서 바로 열기 위한 URL
    }
}
