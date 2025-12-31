package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

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

    private Long id;
    private String title;
    private EduType eduType;
    private int numberOfPeople; // 교육 대상 인원 수
    private int numberOfAttendees; // 출석 인원 수
    private String content;
    private LocalDate eduDate;

    // 출석 인원 리스트 [직원명, 서명 이미지 URL]
    private List<AttendeeInfo> attendees;

    @Getter
    @Setter
    @Builder
    public static class AttendeeInfo {

        private String userName;
        private String signatureUrl; // S3에서 생성한 조회용 URL
    }
}
