package kr.co.awesomelead.groupware_backend.domain.notice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NoticeType {
    GENERAL("일반 공지"),
    URGENT("긴급 공지"),
    EVENT("경조사/이벤트");

    private final String description;
}