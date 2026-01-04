package kr.co.awesomelead.groupware_backend.domain.notice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeSummaryDto {

    private Long id;
    private String title;
    private LocalDateTime updatedDate;
}
