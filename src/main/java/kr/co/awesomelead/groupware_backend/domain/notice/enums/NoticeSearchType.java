package kr.co.awesomelead.groupware_backend.domain.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NoticeSearchType {
    TITLE("제목"),
    CONTENT("내용"),
    AUTHOR("작성자"),
    ALL("전체(제목+내용)");

    private final String description;
}
