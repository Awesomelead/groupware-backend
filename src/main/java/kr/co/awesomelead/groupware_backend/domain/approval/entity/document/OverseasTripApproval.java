package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.detail.OverseasTripExpenseDetail;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

@Entity
@DiscriminatorValue("OVERSEAS_TRIP")
public class OverseasTripApproval extends Approval {

    private String companion; // 동행자
    private String destination; // 출장지
    private String tripPeriod; // 출장기간 (문자열 혹은 날짜)
    private String purpose; // 출장 목적
    private String currencyUnit; // 국가/통화 단위
    private Double exchangeRate; // 적용 환율

    // 가지급금 관련 필드
    private Long advanceCash;   // 현금/개인카드
    private Long advanceCard;   // 법인카드
    private Long advanceTotal;  // 합계
    private Long advanceReturn; // 가지급금 반납
    private Long additionalClaim; // 추가 사용분 신청

    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL)
    private List<OverseasTripExpenseDetail> details = new ArrayList<>();

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.OVERSEAS_TRIP;
    }
}
