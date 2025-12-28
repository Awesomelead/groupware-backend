package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor // MapStruct 빌더 지원을 위해 추가 권장
public class VisitResponseDto {

    private Long id;
    private VisitType visitType;

    private String visitorName;
    private String visitorPhone;
    private String visitorCompany;
    private String carNumber;
    private VisitPurpose purpose;

    private LocalDateTime visitStartDate;
    private LocalDateTime visitEndDate;

    private Long hostUserId;
    private String hostName;
    private String hostDepartment;

    private boolean visited;
    private boolean verified;

    private List<CompanionResponseDto> companions;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CompanionResponseDto {

        private String name;
        private String phoneNumber;
    }
}
