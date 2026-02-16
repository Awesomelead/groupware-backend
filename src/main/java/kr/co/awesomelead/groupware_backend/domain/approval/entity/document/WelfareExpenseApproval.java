package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("WELFARE_EXPENSE")
@Getter
@Setter
public class WelfareExpenseApproval extends ExpenseDraftApproval {

    private String agreementDepartment; // 합의부서/수신부서
}
