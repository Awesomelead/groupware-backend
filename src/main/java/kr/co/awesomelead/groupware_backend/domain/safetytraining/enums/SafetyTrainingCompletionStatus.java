package kr.co.awesomelead.groupware_backend.domain.safetytraining.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SafetyTrainingCompletionStatus {
    COMPLETED("수료"),
    INCOMPLETE("미수료");

    private final String description;
}
