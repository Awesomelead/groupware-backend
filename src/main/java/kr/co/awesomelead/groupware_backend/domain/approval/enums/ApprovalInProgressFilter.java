package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalInProgressFilter {
    ALL("전체"),
    TO_APPROVE("결재하기"),
    BEFORE_MY_TURN("결재 전단계"),
    PROCESSED_BY_ME("기결"),
    REJECTED_OR_RECALLED("반려/회수"),
    DRAFT_BOX("임시저장함");

    private final String description;
}
