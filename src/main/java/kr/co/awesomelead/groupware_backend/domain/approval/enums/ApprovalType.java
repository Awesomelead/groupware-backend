package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalType {
    INTERNAL("내부결재"),
    COOPERATIVE("협조결재");

    private final String description;
}
