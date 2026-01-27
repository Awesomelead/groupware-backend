package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeSearchConditionDto {

    @Schema(description = "공지 유형", example = "REGULAR")
    private NoticeType type;

    @Schema(description = "검색 키워드", example = "회의")
    private String keyword;

    @Schema(description = "검색 유형", example = "TITLE")
    private String searchType;
}
