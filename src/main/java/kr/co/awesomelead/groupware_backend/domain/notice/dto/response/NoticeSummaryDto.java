package kr.co.awesomelead.groupware_backend.domain.notice.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeSummaryDto {

    private Long id;
    private String title;
    private LocalDateTime updatedDate;

}
