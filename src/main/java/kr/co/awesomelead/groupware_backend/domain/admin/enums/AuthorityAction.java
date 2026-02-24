package kr.co.awesomelead.groupware_backend.domain.admin.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthorityAction {
    ADD("추가"),
    REMOVE("제거");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}