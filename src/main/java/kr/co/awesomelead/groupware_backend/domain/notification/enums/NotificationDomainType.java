package kr.co.awesomelead.groupware_backend.domain.notification.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationDomainType {
    VISIT,
    APPROVAL,
    NOTICE,
    ANNUAL_LEAVE,
    GENERAL,
    AUTH,
    EDUCATION,
    PAYSLIP,
    REQUEST_HISTORY,
    CHECK_SHEET;

    @JsonValue
    public String getValue() {
        return this.name();
    }
}
