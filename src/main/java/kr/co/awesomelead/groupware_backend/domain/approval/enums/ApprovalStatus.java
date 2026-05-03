package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalStatus {
    DRAFT("임시저장"),
    IN_PROGRESS("결재진행"),
    APPROVED("완결"),
    REJECTED("반려"),
    RECALLED("회수");

    private final String description;
}
