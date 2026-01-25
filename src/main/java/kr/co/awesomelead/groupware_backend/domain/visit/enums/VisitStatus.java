package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VisitStatus {
    PENDING("승인 대기"), // 장기 방문 신청 직후
    APPROVED("승인 완료"), // 승인이 완료되어 언제든 입실 가능한 상태 (장기 방문 전용)
    NOT_VISITED("방문 전"), // 사전 하루 방문 신청 직후
    IN_PROGRESS("방문 중"), // 현재 방문 기간 내에 있으며, 아직 퇴실하지 않은 상태
    COMPLETED("방문 완료"); // 입실 처리가 완료되었거나 전체 기간이 종료됨

    private final String description; // 한글 설명

    @JsonValue
    public String getDescription() {
        return description;
    }
}
