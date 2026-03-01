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
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.ExpenseDraftApproval;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "expense_draft_details")
public class ExpenseDraftDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_draft_approval_id")
    private ExpenseDraftApproval approval; // 연관된 지출결의서

    private LocalDate evidenceDate; // 증빙 일자
    private String clientName; // 거래처 명
    private String content; // 내용 (PJT Code 포함)

    private Long supplyAmount; // 공급가액
    private Long vatAmount; // 부가세
    private Long totalAmount; // 합계

    private String paymentRequestDate; // 지급 요청일 (문자열 처리 권장)
    private String expenseType; // 비용 구분 (거래처, 개인경비, 법인카드)
}
