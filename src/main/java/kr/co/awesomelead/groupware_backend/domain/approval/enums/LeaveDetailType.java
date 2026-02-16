package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 소분류 (상세 유형)
@Getter
@AllArgsConstructor
public enum LeaveDetailType {
    // 휴가(LEAVE) 관련
    ANNUAL("연차"),
    FAMILY_EVENT("경조"),
    MENSTRUAL("생리"),
    PAID("유급"),
    UNPAID("무급"),

    // 반차(HALF_OFF) 관련
    AM("오전"),
    PM("오후");

    private final String description;

    @JsonValue
    public String getDescription() {
        return description;
    }
}
