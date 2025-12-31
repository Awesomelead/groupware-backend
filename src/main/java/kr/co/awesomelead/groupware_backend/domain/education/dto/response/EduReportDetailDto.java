package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EduReportDetailDto {

    private Long id;
    private String title;
    private LocalDate eduDate;
    private String content;
    private boolean attendance;
    private List<AttachmentResponse> attachments;

    @Getter
    @Setter
    @Builder
    public static class AttachmentResponse {

        private Long id;
        private String originalFileName;
        private long fileSize;
        private String viewUrl; // S3에서 바로 열기 위한 URL
    }
}
