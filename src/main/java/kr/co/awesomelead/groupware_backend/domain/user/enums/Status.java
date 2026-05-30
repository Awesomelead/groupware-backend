package kr.co.awesomelead.groupware_backend.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    PENDING("승인 대기"),
    AVAILABLE("활성"),
    SUSPENDED("비활성");

    private final String description;
}
