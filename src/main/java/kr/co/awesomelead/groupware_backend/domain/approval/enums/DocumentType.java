package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentType {
    LEAVE_REQUEST("휴가신청서"),
    MESSAGE("메시지문서");

    private final String description;
}
