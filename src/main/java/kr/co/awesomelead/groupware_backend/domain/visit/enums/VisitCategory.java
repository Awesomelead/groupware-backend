package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VisitCategory {
    PRE_ONE_DAY("사전 하루"),
    PRE_LONG_TERM("사전 장기"),
    ON_SITE("현장 입실");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
