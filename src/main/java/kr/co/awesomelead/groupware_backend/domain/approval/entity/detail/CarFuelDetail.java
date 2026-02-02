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
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.CarFuelApproval;
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
@Table(name = "car_fuel_details")
public class CarFuelDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_fuel_approval_id")
    private CarFuelApproval approval; // 연관된 차량유류정산서

    private LocalDate driveDate; // 날짜
    private String purpose;    // 운행 목적
    private String route;      // 출발 → 경유지 → 도착

    private Double distanceKm; // 주행거리 Km
    private Long tollParkingFee; // 통행료/주차비
}
