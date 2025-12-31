package kr.co.awesomelead.groupware_backend.domain.education.dto.response;

import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EduReportSummaryDto {

    private Long id;
    private String title;
    private EduType eduType;
    private LocalDate eduDate;
    private boolean attendance;
    private boolean pinned;

}
