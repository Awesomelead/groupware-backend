package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RetentionPeriod {

    FIVE_YEAR("5년"),
    PERMANENT("영구");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
