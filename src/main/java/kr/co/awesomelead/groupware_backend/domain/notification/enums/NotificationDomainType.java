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
    MY_INFO_UPDATE,
    CHECK_SHEET,
    SAFETY_TRAINING;

    @JsonValue
    public String getValue() {
        return this.name();
    }
}
