package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalLineStatus {
    WAITING("대기"),
    PENDING("결재 대기"),
    APPROVED("승인"),
    REJECTED("반려"),
    SKIPPED("건너뜀");

    private final String description;
}
