package kr.co.awesomelead.groupware_backend.domain.payslip.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayslipStatus {
    SENT("발송 완료"),
    READ("열람 완료");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
