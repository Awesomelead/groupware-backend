package kr.co.awesomelead.groupware_backend.domain.safetytraining.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SafetyEducationMethod {
    LECTURE("강의"),
    AUDIOVISUAL("시청각"),
    FIELD_TRAINING("현장 교육"),
    DEMONSTRATION("시범 실습"),
    TOUR("견학"),
    ROLE_PLAY("역할연기");

    private final String description;
}
