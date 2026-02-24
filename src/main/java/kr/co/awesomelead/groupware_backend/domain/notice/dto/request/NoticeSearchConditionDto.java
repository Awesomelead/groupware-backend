package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeSearchType;
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

    @Schema(description = "공지 유형 (상시공지, 식단표, 기타)", example = "상시공지")
    private NoticeType type;

    @Schema(description = "검색 키워드", example = "회의")
    private String keyword;

    @Schema(description = "검색 유형 (제목, 내용, 작성자, 전체(제목+내용))", example = "제목")
    private NoticeSearchType searchType;
}
