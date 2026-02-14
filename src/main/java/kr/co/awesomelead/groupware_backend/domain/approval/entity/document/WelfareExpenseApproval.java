package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("WELFARE_EXPENSE")
public class WelfareExpenseApproval extends ExpenseDraftApproval {

    private String agreementDepartment; // 합의부서/수신부서
}
