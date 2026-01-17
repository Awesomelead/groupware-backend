package kr.co.awesomelead.groupware_backend.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Role {
    ADMIN("관리자"),
    USER("일반 사용자"),
    MASTER_ADMIN("마스터 관리자");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }

    Role(String description) {
        this.description = description;
    }
}
