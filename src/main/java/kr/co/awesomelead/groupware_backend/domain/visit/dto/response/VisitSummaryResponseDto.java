package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VisitSummaryResponseDto {

    private Long visitId;
    private String visitorName;
    private String visitorCompany;
    private LocalDateTime visitStartDate;

}
