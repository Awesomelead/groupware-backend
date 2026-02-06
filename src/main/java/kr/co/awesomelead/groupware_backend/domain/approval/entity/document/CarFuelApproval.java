package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.detail.CarFuelDetail;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import java.util.ArrayList;
import java.util.List;

@Entity
@DiscriminatorValue("CAR_FUEL")
public class CarFuelApproval extends Approval {

    private String agreementDepartment;
    private String carTypeNumber; // 차종/차량번호
    private String fuelType; // 연료 구분

    private Double totalDistanceKm; // 총 합계 운영거리
    private Long fuelClaimAmount; // 유류대 청구 금액
    private Long totalAmount; // 총 합계 금액

    private String bankName;
    private String accountNumber;
    private String accountHolder;

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL)
    private List<CarFuelDetail> details = new ArrayList<>();

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.CAR_FUEL;
    }
}
