package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
