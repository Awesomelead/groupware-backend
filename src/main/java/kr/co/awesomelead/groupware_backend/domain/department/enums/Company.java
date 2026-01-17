package kr.co.awesomelead.groupware_backend.domain.department.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Company {
    AWESOME("어썸리드"),
    MARUI("마루이");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
