package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalLinePolicy {
    FIXED("고정 결재선"),
    FLEXIBLE("가변 결재선");

    private final String description;
}
