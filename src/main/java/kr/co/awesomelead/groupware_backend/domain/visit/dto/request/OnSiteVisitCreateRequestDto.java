package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OnSiteVisitCreateRequestDto {

    @NotNull
    private Long hostUserId;

    @NotNull
    @Valid
    private VisitorRequestDto visitor;

    @NotBlank
    private String hostCompany;
    @NotNull
    private VisitPurpose purpose;
    private String carNumber;

    @NotNull
    private LocalDateTime visitStartDate;
    @NotNull
    private LocalDateTime visitEndDate;

    @Valid
    private List<CompanionRequestDto> companions;
}
