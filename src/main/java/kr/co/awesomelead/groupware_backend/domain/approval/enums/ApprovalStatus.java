package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalStatus {
    WAITING("대기"), // 앞 순서 결재자가 아직 승인을 안 해서 대기 중인 상태
    PENDING("승인 대기"), // 내 차례가 되어서 결재를 기다리는 상태 (알림 발송 대상)
    APPROVED("승인"), // 결재자가 승인함
    REJECTED("반려"), // 결재자가 반려함
    CANCELED("취소"); // 기안자가 상신을 취소함 (선택 사항)

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
