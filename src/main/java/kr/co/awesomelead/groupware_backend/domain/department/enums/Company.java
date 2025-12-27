package kr.co.awesomelead.groupware_backend.domain.department.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Company {
    AWESOME("어썸리드"),
    MARUI("마루이");

    private final String description;
}
