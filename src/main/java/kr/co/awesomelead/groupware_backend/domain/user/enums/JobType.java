package kr.co.awesomelead.groupware_backend.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobType {

    // 현장직
    FIELD("현장직"),

    // 관리직
    MANAGEMENT("관리직");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
