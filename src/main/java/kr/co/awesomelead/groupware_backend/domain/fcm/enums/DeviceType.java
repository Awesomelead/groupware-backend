package kr.co.awesomelead.groupware_backend.domain.fcm.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeviceType {
    ANDROID,
    IOS,
    WEB;

    @JsonValue
    public String getValue() {
        return this.name();
    }
}
