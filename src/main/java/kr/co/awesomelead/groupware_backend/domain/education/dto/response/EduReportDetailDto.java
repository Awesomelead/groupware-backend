package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EduReportDetailDto {

    @Schema(description = "교육 보고서 ID", example = "1")
    private Long id;

    @Schema(description = "교육 제목", example = "안전 교육")
    private String title;

    @Schema(description = "교육 날짜", example = "2024-06-15")
    private LocalDate eduDate;

    @Schema(description = "교육 내용", example = "이번 교육에서는 안전 수칙에 대해 다룹니다.")
    private String content;

    @Schema(description = "출석 여부", example = "true")
    private boolean attendance;

    @Schema(description = "첨부 파일 목록")
    private List<AttachmentResponse> attachments;

    @Getter
    @Setter
    @Builder
    public static class AttachmentResponse {

        @Schema(description = "첨부 파일 ID", example = "1")
        private Long id;

        @Schema(description = "원본 파일 이름", example = "report.pdf")
        private String originalFileName;

        @Schema(description = "파일 크기 (바이트)", example = "204800")
        private long fileSize;

        @Schema(description = "파일 조회 URL", example = "https://s3.amazonaws.com/bucket/report.pdf")
        private String viewUrl; // S3에서 바로 열기 위한 URL
    }
}
