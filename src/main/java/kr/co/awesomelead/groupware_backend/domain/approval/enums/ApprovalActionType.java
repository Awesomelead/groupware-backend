package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalActionType {
    SAVE_DRAFT("임시저장"),
    SUBMIT("상신"),
    APPROVE("승인"),
    REJECT("반려"),
    RECALL("회수"),
    RESUBMIT("재상신");

    private final String description;
}
