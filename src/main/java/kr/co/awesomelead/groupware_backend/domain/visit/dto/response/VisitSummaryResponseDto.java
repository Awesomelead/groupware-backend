package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class VisitSummaryResponseDto {

    private Long visitId;
    private String visitorName;
    private String visitorCompany;
    private LocalDateTime visitStartDate;
    private LocalDateTime visitEndDate;
    private boolean visited;
}
