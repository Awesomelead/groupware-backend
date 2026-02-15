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
@Schema(description = "기안 및 지출결의 생성 요청 DTO")
public class ExpenseDraftApprovalCreateRequestDto extends ApprovalCreateRequestDto {

    @Schema(description = "상세 지출 내역 리스트")
    @NotEmpty(message = "지출 상세 내역은 최소 1건 이상이어야 합니다.")
    @Valid // 리스트 내부의 ExpenseDraftDetailRequestDto 검증 전파
    private List<ExpenseDraftDetailRequestDto> details;

    @Getter
    @Setter
    @Schema(description = "지출결의 상세 내역 DTO")
    public static class ExpenseDraftDetailRequestDto {

        @Schema(description = "증빙 일자", example = "2026-02-15")
        @NotNull(message = "증빙 일자는 필수입니다.")
        private LocalDate evidenceDate;

        @Schema(description = "거래처 명", example = "(주)어썸테크")
        @NotBlank(message = "거래처 명은 필수입니다.")
        private String clientName;

        @Schema(description = "내용 (PJT Code 포함 가능)", example = "[PJT-001] 서버 유지보수 비용")
        @NotBlank(message = "지출 내용은 필수입니다.")
        private String content;

        @Schema(description = "공급가액", example = "100000")
        @NotNull(message = "공급가액은 필수입니다.")
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        private Long supplyAmount;

        @Schema(description = "부가세", example = "10000")
        @NotNull(message = "부가세는 필수입니다.")
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        private Long vatAmount;

        @Schema(description = "합계 금액", example = "110000")
        @NotNull(message = "합계 금액은 필수입니다.")
        @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
        private Long totalAmount;

        @Schema(description = "지급 요청일 (문자열 형식)", example = "2026-02-28 또는 익월 말일")
        @NotBlank(message = "지급 요청일 정보를 입력해주세요.")
        private String paymentRequestDate;

        @Schema(description = "비용 구분 (거래처, 개인경비, 법인카드 등)", example = "법인카드")
        @NotBlank(message = "비용 구분을 입력해주세요.")
        private String expenseType;
    }
}
