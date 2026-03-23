package kr.co.awesomelead.groupware_backend.domain.safetytraining.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SafetyTrainingAttendeeStatus {
    PENDING("서명 대기"),
    SIGNED("서명 완료"),
    ABSENT("불참");

    private final String description;
}
