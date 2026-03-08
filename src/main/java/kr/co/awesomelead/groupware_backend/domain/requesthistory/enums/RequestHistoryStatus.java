package kr.co.awesomelead.groupware_backend.domain.requesthistory.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RequestHistoryStatus {
    PENDING("발급 대기"),
    ISSUED("발급 완료"),
    REJECTED("반려"),
    CANCELED("취소");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
