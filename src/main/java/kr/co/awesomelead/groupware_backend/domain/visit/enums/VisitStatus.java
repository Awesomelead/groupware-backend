package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VisitStatus {
    PENDING("승인 대기"),      // 장기 방문 신청 직후
    NOT_VISITED("방문 전"),    // 사전 하루 방문 신청 직후 또는 승인된 장기 방문의 대기 상태
    COMPLETED("방문 완료");    // 입실 처리가 완료되었거나 전체 기간이 종료됨

    private final String description; // 한글 설명

    @JsonValue
    public String getDescription() {
        return description;
    }
}
