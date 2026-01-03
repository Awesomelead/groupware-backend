package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NoticeCreateRequestDto {

    @NotBlank(message = "공지사항 제목은 필수입니다.")
    private String title;
    private String content;
    @NotNull(message = "공지 유형은 필수입니다.")
    private NoticeType type;
    @Builder.Default
    private Boolean pinned = false;
}
