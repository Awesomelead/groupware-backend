package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class EduReportAdminDetailDto {

    @Schema(description = "교육 보고서 ID", example = "1")
    private Long id;

    @Schema(description = "교육 제목", example = "안전 교육")
    private String title;

    @Schema(description = "교육 유형", example = "MANDATORY")
    private EduType eduType;

    @Schema(description = "교육 대상 인원 수", example = "50")
    private int numberOfPeople; // 교육 대상 인원 수

    @Schema(description = "출석 인원 수", example = "45")
    private int numberOfAttendees; // 출석 인원 수

    @Schema(description = "교육 내용", example = "이번 교육에서는 안전 수칙에 대해 다룹니다.")
    private String content;

    @Schema(description = "교육 날짜", example = "2024-06-15")
    private LocalDate eduDate;

    // 출석 인원 리스트 [직원명, 서명 이미지 URL]
    @Schema(description = "출석 인원 리스트")
    private List<AttendeeInfo> attendees;

    @Getter
    @Setter
    @Builder
    public static class AttendeeInfo {

        @Schema(description = "직원 이름", example = "홍길동")
        private String userName;

        @Schema(
                description = "서명 이미지 URL",
                example = "https://s3.amazonaws.com/bucket/signature.png")
        private String signatureUrl; // S3에서 생성한 조회용 URL
    }
}
