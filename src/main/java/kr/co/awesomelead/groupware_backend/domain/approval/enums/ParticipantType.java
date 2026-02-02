package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParticipantType {
    REFERRER("참조자"),
    VIEWER("열람권자");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
