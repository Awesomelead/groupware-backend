package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeUpdateRequestDto {

    private String title;
    private String content;
    private Boolean pinned;
    private List<Long> attachmentsIdsToRemove;
}
