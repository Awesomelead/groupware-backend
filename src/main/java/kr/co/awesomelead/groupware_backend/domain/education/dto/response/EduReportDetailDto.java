package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduReportStatus;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

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

    @Schema(description = "교육 게시물 ID", example = "1")
    private Long id;

    @Schema(description = "교육 제목", example = "안전 교육")
    private String title;

    @Schema(description = "교육 유형", example = "안전 보건")
    private EduType eduType;

    @Schema(description = "교육 카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "교육 카테고리명", example = "유해위험물질")
    private String categoryName;

    @Schema(description = "부서 교육 시 대상 부서명 (부서 교육이 아닌 경우 null)", example = "영업부")
    private String departmentName;

    @Schema(description = "교육 날짜", example = "2024-06-15")
    private LocalDate eduDate;

    @Schema(description = "교육 내용", example = "이번 교육에서는 안전 수칙에 대해 다룹니다.")
    private String content;

    @Schema(description = "출석 여부", example = "true")
    private boolean attendance;

    @Schema(description = "서명 필수 여부", example = "false")
    private boolean signatureRequired;

    @Schema(description = "교육 상태", example = "OPEN")
    private EduReportStatus status;

    @Schema(description = "첨부 파일 목록")
    private List<AttachmentResponse> attachments;

    @Schema(description = "교육 대상 인원 수 (MANAGE_DEPARTMENT_EDUCATION 권한 없으면 null)", example = "50")
    private Integer numberOfPeople;

    @Schema(description = "출석 인원 수 (MANAGE_DEPARTMENT_EDUCATION 권한 없으면 null)", example = "45")
    private Integer numberOfAttendees;

    @Schema(description = "대상 인원 수(부서교육 상세 조회 기준)", example = "50")
    private Integer targetCount;

    @Schema(description = "서명 인원 수(부서교육 상세 조회 기준)", example = "45")
    private Integer signedCount;

    @Schema(description = "미서명 인원 수(부서교육 상세 조회 기준)", example = "5")
    private Integer unsignedCount;

    @Schema(description = "출석자 목록 (MANAGE_DEPARTMENT_EDUCATION 권한 없으면 null)")
    private List<AttendeeInfo> attendees;

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
        private String viewUrl;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendeeInfo {

        @Schema(description = "직원 이름", example = "홍길동")
        private String userName;

        @Schema(
                description = "서명 이미지 URL",
                example = "https://s3.amazonaws.com/bucket/signature.png")
        private String signatureUrl;
    }
}
