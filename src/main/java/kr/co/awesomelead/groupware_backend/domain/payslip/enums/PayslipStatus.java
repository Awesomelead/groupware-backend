package kr.co.awesomelead.groupware_backend.domain.payslip.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayslipStatus {
    PENDING("확인 대기"),
    APPROVED("확인 완료"),
    REJECTED("거절됨");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
