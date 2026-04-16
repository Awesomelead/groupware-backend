package kr.co.awesomelead.groupware_backend.domain.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;

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

    @Schema(
            description = "작성일시 (KST, Asia/Seoul)",
            type = "string",
            format = "date-time",
            example = "2026-02-27T10:30:00")
    private LocalDateTime createdDate;

    @Schema(description = "조회수", example = "42")
    private int viewCount;

    @Schema(description = "첨부파일 목록")
    private List<AttachmentResponse> attachments;

    @Schema(
            description = "공지사항 유형 (영문 코드)",
            example = "REGULAR",
            allowableValues = {"REGULAR", "MENU", "ETC"})
    private String type;

    @Schema(description = "대상 회사 목록", example = "[\"어썸리드\", \"마루이\"]")
    private List<Company> targetCompanies;

    @Schema(description = "대상 부서 ID 목록", example = "[1, 2, 3]")
    private List<Long> targetDepartmentIds;

    @Schema(description = "대상 유저 ID 목록", example = "[10, 20]")
    private List<Long> targetUserIds;

    @Setter
    @Schema(description = "이전 공지사항 정보 (없으면 null)")
    private NoticeInfo prevNotice;

    @Setter
    @Schema(description = "다음 공지사항 정보 (없으면 null)")
    private NoticeInfo nextNotice;

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

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "이전/다음 공지사항 요약 정보")
    public static class NoticeInfo {

        @Schema(description = "공지사항 ID", example = "1")
        private Long id;

        @Schema(description = "공지사항 제목", example = "2025년 1월 전체 회의 안내")
        private String title;

        @Schema(description = "작성자 이름", example = "홍길동")
        private String authorName;

        @Schema(
                description = "작성일시 (KST, Asia/Seoul)",
                type = "string",
                format = "date-time",
                example = "2026-02-27T10:30:00")
        private LocalDateTime createdDate;
    }
}
