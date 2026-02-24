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

    public static NoticeSearchType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("searchType is null");
        }
        String v = value.trim();
        for (NoticeSearchType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.getDescription().equals(v)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown NoticeSearchType: " + value);
    }
}
