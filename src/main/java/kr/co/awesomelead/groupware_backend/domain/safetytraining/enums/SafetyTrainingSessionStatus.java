package kr.co.awesomelead.groupware_backend.domain.safetytraining.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SafetyTrainingSessionStatus {
    OPEN("진행중"),
    CLOSED("마감");

    private final String description;
}
