package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

import java.util.Set;

@Getter
public enum LeaveType {
    LEAVE(
            "휴가",
            Set.of(
                    LeaveDetailType.ANNUAL,
                    LeaveDetailType.FAMILY_EVENT,
                    LeaveDetailType.MENSTRUAL,
                    LeaveDetailType.PAID,
                    LeaveDetailType.UNPAID)),
    HALF_OFF("반차", Set.of(LeaveDetailType.AM, LeaveDetailType.PM)),
    EDUCATION("교육", Set.of()),
    TRAINING("훈련(예비군)", Set.of()),
    OTHER("기타", Set.of());

    private final String description;
    private final Set<LeaveDetailType> allowedDetailTypes;

    LeaveType(String description, Set<LeaveDetailType> allowedDetailTypes) {
        this.description = description;
        this.allowedDetailTypes = allowedDetailTypes;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    /**
     * LeaveType에 따른 LeaveDetailType 유효성 검증 - LEAVE: ANNUAL, FAMILY_EVENT, MENSTRUAL, PAID, UNPAID 중
     * 하나 필수 - HALF_OFF: AM, PM 중 하나 필수 - EDUCATION, TRAINING, OTHER: LeaveDetailType은 null이어야 함
     */
    public void validateDetailType(LeaveDetailType detailType) {
        if (allowedDetailTypes.isEmpty()) {
            // 소분류가 불필요한 타입 (EDUCATION, TRAINING, OTHER)
            if (detailType != null) {
                throw new IllegalArgumentException(
                        String.format(
                                "[%s] 유형은 소분류(LeaveDetailType)를 지정할 수 없습니다.", this.description));
            }
        } else {
            // 소분류가 필수인 타입 (LEAVE, HALF_OFF)
            if (detailType == null) {
                throw new IllegalArgumentException(
                        String.format(
                                "[%s] 유형은 소분류(LeaveDetailType)가 필수입니다. 허용: %s",
                                this.description, allowedDetailTypes));
            }
            if (!allowedDetailTypes.contains(detailType)) {
                throw new IllegalArgumentException(
                        String.format(
                                "[%s] 유형에 [%s]는 허용되지 않습니다. 허용: %s",
                                this.description, detailType.getDescription(), allowedDetailTypes));
            }
        }
    }
}
