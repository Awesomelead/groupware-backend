package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VisitType {
    PRE_REGISTRATION("사전 예약"), // 사전 예약
    ON_SITE("현장 방문"); // 현장 방문

    private final String description; // 한글 설명

    @JsonValue
    public String getDescription() {
        return description;
    }
}
