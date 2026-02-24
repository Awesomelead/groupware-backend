package kr.co.awesomelead.groupware_backend.domain.notice.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoticeType {
    REGULAR("상시공지"),
    MENU("식단표"),
    ETC("기타");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }

    public static NoticeType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("type is null");
        }
        String v = value.trim();
        for (NoticeType t : values()) {
            if (t.name().equalsIgnoreCase(v) || t.getDescription().equals(v)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown NoticeType: " + value);
    }
}
