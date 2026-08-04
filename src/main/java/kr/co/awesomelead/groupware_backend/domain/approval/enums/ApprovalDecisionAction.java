package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalDecisionAction {
    APPROVE("승인"),
    REJECT("반려"),
    HOLD("보류");

    private final String description;
}
