package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("WELFARE_EXPENSE")
@Getter
@Setter
public class WelfareExpenseApproval extends ExpenseDraftApproval {

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.WELFARE_EXPENSE;
    }
}
