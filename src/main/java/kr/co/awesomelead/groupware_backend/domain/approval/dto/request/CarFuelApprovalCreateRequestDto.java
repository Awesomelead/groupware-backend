package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "차량유류정산지출결의 생성 요청 DTO")
public class CarFuelApprovalCreateRequestDto extends ApprovalCreateRequestDto {

    @Schema(description = "합의부서", example = "관리본부")
    @NotBlank(message = "합의부서는 필수입니다.")
    private String agreementDepartment;

    @Schema(description = "차종/차량번호", example = "카니발 / 12가 3456")
    @NotBlank(message = "차량 정보는 필수입니다.")
    private String carTypeNumber;

    @Schema(description = "연료 구분", example = "휘발유")
    @NotBlank(message = "연료 구분은 필수입니다.")
    private String fuelType;

    @Schema(description = "총 합계 운영거리 (Km)", example = "150.5")
    @NotNull(message = "총 운영거리는 필수입니다.")
    @PositiveOrZero(message = "운영거리는 0 이상이어야 합니다.")
    private Double totalDistanceKm;

    @Schema(description = "유류대 청구 금액 (원)", example = "25000")
    @NotNull(message = "유류대 청구 금액은 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long fuelClaimAmount;

    @Schema(description = "총 합계 금액 (유류대 + 기타비용)", example = "30000")
    @NotNull(message = "총 합계 금액은 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long totalAmount;

    @Schema(description = "은행명", example = "국민은행")
    @NotBlank(message = "은행명은 필수입니다.")
    private String bankName;

    @Schema(description = "계좌번호", example = "123-456-7890")
    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNumber;

    @Schema(description = "예금주", example = "홍길동")
    @NotBlank(message = "예금주는 필수입니다.")
    private String accountHolder;

    @Schema(description = "상세 운행 명세 리스트")
    @NotEmpty(message = "상세 운행 명세는 최소 1건 이상이어야 합니다.")
    @Valid // 리스트 내부의 CarFuelDetailRequestDto 검증을 위해 필수
    private List<CarFuelDetailRequestDto> details;

    @Getter
    @Setter
    @Schema(description = "차량유류정산 상세 내역 DTO")
    public static class CarFuelDetailRequestDto {

        @Schema(description = "운행 날짜", example = "2026-03-02")
        @NotNull(message = "운행 날짜는 필수입니다.")
        private LocalDate driveDate;

        @Schema(description = "운행 목적", example = "A업체 기술 지원 외근")
        @NotBlank(message = "운행 목적은 필수입니다.")
        private String purpose;

        @Schema(description = "운행 경로 (출발 → 경유 → 도착)", example = "본사 → 화성 공장 → 본사")
        @NotBlank(message = "운행 경로는 필수입니다.")
        private String route;

        @Schema(description = "주행거리 (Km)", example = "50.2")
        @NotNull(message = "주행거리는 필수입니다.")
        @PositiveOrZero(message = "주행거리는 0 이상이어야 합니다.")
        private Double distanceKm;

        @Schema(description = "통행료/주차비 (원)", example = "5000")
        @NotNull(message = "비용 정보는 필수입니다.")
        @PositiveOrZero(message = "비용은 0 이상이어야 합니다.")
        private Long tollParkingFee;
    }
}
