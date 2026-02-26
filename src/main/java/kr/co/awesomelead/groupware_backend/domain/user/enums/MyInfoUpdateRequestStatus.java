package kr.co.awesomelead.groupware_backend.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MyInfoUpdateRequestStatus {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    CANCELED("취소");

    private final String description;
}
