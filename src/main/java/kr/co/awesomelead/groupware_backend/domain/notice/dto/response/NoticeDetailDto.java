package kr.co.awesomelead.groupware_backend.domain.notice.dto.response;

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
public class NoticeDetailDto {

    private Long id;
    private String title;
    private String content;
    private String authorName;
    private LocalDateTime updatedDate;
    private int viewCount;

    private List<AttachmentResponse> attachments;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachmentResponse {

        private Long id;
        private String originalFileName;
        private long fileSize;
        private String viewUrl; // S3에서 바로 열기 위한 URL
    }
}
