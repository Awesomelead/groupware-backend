package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;

@Entity
@DiscriminatorValue("BASIC")
public class BasicApproval extends Approval {

}
