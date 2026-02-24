package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.detail.ExpenseDraftDetail;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("EXPENSE_DRAFT")
@Getter
@Setter
public class ExpenseDraftApproval extends Approval {

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL)
    private List<ExpenseDraftDetail> details = new ArrayList<>();

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.EXPENSE_DRAFT;
    }
}
