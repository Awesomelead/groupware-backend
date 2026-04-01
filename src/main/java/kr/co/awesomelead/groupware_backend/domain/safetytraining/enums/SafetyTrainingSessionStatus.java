package kr.co.awesomelead.groupware_backend.domain.safetytraining.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SafetyTrainingSessionStatus {
    OPEN("진행중"),
    CLOSED("정상 마감"),
    CANCELED("오등록 종료");

    private final String description;
}
