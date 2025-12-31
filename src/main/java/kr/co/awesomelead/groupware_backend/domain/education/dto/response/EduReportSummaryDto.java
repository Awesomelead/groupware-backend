package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class EduReportSummaryDto {

    private Long id;
    private String title;
    private EduType eduType;
    private LocalDate eduDate;
    private boolean attendance;
    private boolean pinned;
}
