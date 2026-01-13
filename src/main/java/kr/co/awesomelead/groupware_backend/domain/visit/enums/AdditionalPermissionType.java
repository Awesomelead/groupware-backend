package kr.co.awesomelead.groupware_backend.domain.visit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdditionalPermissionType {
    NONE("해당 없음"),
    CONFINED_SPACE_ENTRY("밀폐공간 출입"),
    HIGH_ALTITUDE_WORK("고소 작업"),
    OTHER_PERMISSION("기타 허가");

    private final String description;
}