package kr.co.awesomelead.groupware_backend.domain.approval.entity.detail;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.OverseasTripApproval;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "overseas_trip_details")
public class OverseasTripExpenseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overseas_trip_approval_id")
    private OverseasTripApproval approval; // 연관된 국외출장정산서

    private String evidenceNumber; // 증빙 번호
    private LocalDate evidenceDate; // 일자
    private String usageType;      // 사용 구분
    private String description;    // 상세 내용

    private Double foreignCurrency; // 외화
    private Double exchangeRate;    // 환율

    private Long cashAmount;       // 현금/개인 카드 (원화 환산액)
    private Long cardAmount;       // 법인 카드 (원화 환산액)
    private Long totalAmount;      // 합계
}
