package kr.co.awesomelead.groupware_backend.domain.notice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoticeType {
    REGULAR("상시공지"),
    MENU("식단표"),
    ETC("기타");

    private final String description;
}
