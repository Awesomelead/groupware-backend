package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdditionalPermissionType {
    NONE("해당없음"),
    CONFINED_SPACE_ENTRY("밀폐공간 작업"),
    HOT_WORK("화기 작업 (용접, 에어로졸 사용 포함)"),
    FIRE_PROTECTION_WORK("소방공사 작업"),
    ELECTRICAL_OUTAGE_WORK("정전(전기) 작업"),
    HIGH_ALTITUDE_WORK("고소 작업"),
    HEAVY_EQUIPMENT_WORK("중장비 작업"),
    OTHER_PERMISSION("기타 허가");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
