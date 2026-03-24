package kr.co.awesomelead.groupware_backend.domain.safetytraining.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SafetyEducationType {
    REGULAR("정기교육"),
    HIRING("채용시"),
    JOB_CHANGE("작업내용 변경시"),
    SPECIAL("특별교육"),
    MSDS("MSDS교육");

    private final String description;
}
