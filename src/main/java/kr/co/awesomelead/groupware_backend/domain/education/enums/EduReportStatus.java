package kr.co.awesomelead.groupware_backend.domain.education.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EduReportStatus {
    OPEN("진행중"),
    CLOSED("마감");

    private final String description;
}
