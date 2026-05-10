package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalSavedLineType {
    PERSONAL("개인 결재선"),
    DEPARTMENT("부서 결재선");

    private final String description;
}
