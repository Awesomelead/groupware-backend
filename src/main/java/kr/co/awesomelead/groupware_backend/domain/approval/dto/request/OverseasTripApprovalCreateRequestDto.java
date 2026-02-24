package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Schema(description = "국외출장여비정산서 생성 요청 DTO")
public class OverseasTripApprovalCreateRequestDto extends ApprovalCreateRequestDto {

    @Schema(description = "동행자", example = "박지민 팀장, 김철수 대리")
    private String companion; // 동행자는 없을 수 있으므로 검증 생략 가능

    @Schema(description = "출장지", example = "미국 샌프란시스코 (Google 본사)")
    @NotBlank(message = "출장지는 필수입니다.")
    private String destination;

    @Schema(description = "출장 기간", example = "2026-03-01 ~ 2026-03-10")
    @NotBlank(message = "출장 기간은 필수입니다.")
    private String tripPeriod;

    @Schema(description = "출장 목적", example = "글로벌 기술 컨퍼런스 참가 및 파트너사 미팅")
    @NotBlank(message = "출장 목적은 필수입니다.")
    private String purpose;

    @Schema(description = "국가/통화 단위", example = "USD")
    @NotBlank(message = "통화 단위는 필수입니다.")
    private String currencyUnit;

    @Schema(description = "적용 환율 (정산 시점 기준)", example = "1350.5")
    @NotNull(message = "환율 정보는 필수입니다.")
    @Positive(message = "환율은 0보다 커야 합니다.")
    private Double exchangeRate;

    @Schema(description = "현금/개인카드 가지급액 (원)", example = "500000")
    @NotNull(message = "가지급액 정보는 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long advanceCash;

    @Schema(description = "법인카드 가지급액 (원)", example = "1000000")
    @NotNull(message = "가지급액 정보는 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long advanceCard;

    @Schema(description = "가지급금 총 합계 (원)", example = "1500000")
    @NotNull(message = "가지급금 합계는 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long advanceTotal;

    @Schema(description = "가지급금 반납액 (남은 돈)", example = "200000")
    @NotNull(message = "반납액 정보는 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long advanceReturn;

    @Schema(description = "추가 사용분 신청액 (모자란 돈)", example = "0")
    @NotNull(message = "추가 신청액 정보는 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long additionalClaim;

    @Schema(description = "상세 지출 내역 리스트 (영수증별)")
    @NotEmpty(message = "지출 상세 내역은 최소 1건 이상이어야 합니다.")
    @Valid // 리스트 내부 객체 검증 전파
    private List<OverseasTripExpenseDetailRequestDto> details;

    @Getter
    @Setter
    @Schema(description = "국외출장 상세 지출 내역 DTO")
    public static class OverseasTripExpenseDetailRequestDto {

        @Schema(description = "증빙 번호 (영수증 번호 등)", example = "REC-20260301-001")
        @NotBlank(message = "증빙 번호는 필수입니다.")
        private String evidenceNumber;

        @Schema(description = "지출 일자", example = "2026-03-01")
        @NotNull(message = "지출 일자는 필수입니다.")
        private LocalDate evidenceDate;

        @Schema(description = "사용 구분 (식비, 숙박비, 교통비 등)", example = "숙박비")
        @NotBlank(message = "사용 구분은 필수입니다.")
        private String usageType;

        @Schema(description = "상세 내용", example = "샌프란시스코 힐튼 호텔 1박")
        @NotBlank(message = "상세 내용은 필수입니다.")
        private String description;

        @Schema(description = "외화 금액 (현지 통화 기준)", example = "250.0")
        @NotNull(message = "외화 금액은 필수입니다.")
        @Positive(message = "금액은 0보다 커야 합니다.")
        private Double foreignCurrency;

        @Schema(description = "해당 지출 시점 환율", example = "1348.0")
        @NotNull(message = "환율 정보는 필수입니다.")
        @Positive(message = "환율은 0보다 커야 합니다.")
        private Double exchangeRate;

        @Schema(description = "현금/개인 카드 사용분 (원화 환산)", example = "337000")
        @NotNull(message = "원화 환산액은 필수입니다.")
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        private Long cashAmount;

        @Schema(description = "법인 카드 사용분 (원화 환산)", example = "0")
        @NotNull(message = "원화 환산액은 필수입니다.")
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        private Long cardAmount;

        @Schema(description = "합계 금액 (원화)", example = "337000")
        @NotNull(message = "합계 금액은 필수입니다.")
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        private Long totalAmount;
    }
}
