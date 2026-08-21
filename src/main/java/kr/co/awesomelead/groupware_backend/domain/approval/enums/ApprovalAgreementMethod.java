package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalAgreementMethod {
    SEQUENTIAL("순차합의"),
    PARALLEL("병렬합의");

    private final String description;
}
