package kr.co.awesomelead.groupware_backend.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Position {
    CEO("대표이사"),
    VICE_PRESIDENT("부사장"),
    SENIOR_MANAGING_DIRECTOR("전무이사"),
    MANAGING_DIRECTOR("상무이사"),
    DIRECTOR("이사"),
    GENERAL_MANAGER("부장"),
    DEPUTY_GENERAL_MANAGER("차장"),
    MANAGER("과장"),
    ASSISTANT_MANAGER("대리"),
    SENIOR_STAFF("주임"),
    STAFF("사원"),

    // 특수 직급
    SECTION_HEAD("반장"),
    ADVISOR("전문위원"),
    SECURITY_GUARD("경비원");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
