package kr.co.awesomelead.groupware_backend.domain.approval.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalRouteRole {
    APPROVAL_LINE("결재선"),
    AGREEMENT_REQUIRED("합의부서(필수)"),
    AGREEMENT_OPTIONAL("합의부서(선택)"),
    REFERENCE("참조자"),
    VIEWER("열람권자"),
    RECEIVER_DEPARTMENT("수신부서");

    private final String description;
}
