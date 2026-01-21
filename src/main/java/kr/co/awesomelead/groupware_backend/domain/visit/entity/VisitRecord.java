package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VisitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate visitDate; // 방문일

    private LocalDateTime entryTime; // 실제 입실 시간
    private LocalDateTime exitTime;  // 실제 퇴실 시간

    private String signatureKey; // 입실 시 수행하는 서명 (S3)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false)
    @JsonBackReference // 무한 참조 방지
    private Visit visit;

    // 상태 확인용 로직
    public boolean isCheckedIn() {
        return entryTime != null;
    }
}
