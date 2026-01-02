package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class VisitDetailResponseDto {

    private Long visitId;
    private String visitorCompany;
    private String visitorName;
    private VisitPurpose purpose;
    private String hostDepartment; // 담당 부서
    private String hostName; // 담당자 이름
    private String phoneNumber; // 내방객휴대전화 번호
    private LocalDateTime visitStartDate;
    private LocalDateTime visitEndDate;
    private String signatureUrl; // 서명 이미지 URL
    private boolean visited;

}
