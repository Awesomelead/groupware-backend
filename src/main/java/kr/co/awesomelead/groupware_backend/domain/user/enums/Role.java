package kr.co.awesomelead.groupware_backend.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
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

    public static Role from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("역할 값은 null일 수 없습니다.");
        }

        String normalized = value.trim();

        return Arrays.stream(values())
            .filter(
                role ->
                    role.name().equalsIgnoreCase(normalized)
                        || role.getDescription().equals(normalized))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("알 수 없는 역할: " + value));
    }
}
