package kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestType;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "제증명 발급 신청 목록 응답")
public class RequestHistorySummaryResponseDto {

    @Schema(description = "신청 ID", example = "1")
    private Long id;

    @Schema(description = "증명서 구분", example = "재직증명서")
    private RequestType requestType;

    @Schema(description = "용도", example = "은행 제출용")
    private String purpose;

    @Schema(description = "발급 부수", example = "1")
    private Integer copies;

    @Schema(description = "발급 희망일", example = "2026-03-10")
    private LocalDate wishDate;

    @Schema(description = "신청일", example = "2026-03-07")
    private LocalDate requestDate;

    @Schema(description = "처리 상태", example = "대기")
    private ApprovalStatus approvalStatus;

    public static RequestHistorySummaryResponseDto from(RequestHistory requestHistory) {
        return RequestHistorySummaryResponseDto.builder()
                .id(requestHistory.getId())
                .requestType(requestHistory.getRequestType())
                .purpose(requestHistory.getPurpose())
                .copies(requestHistory.getCopies())
                .wishDate(requestHistory.getWishDate())
                .requestDate(requestHistory.getRequestDate())
                .approvalStatus(requestHistory.getApprovalStatus())
                .build();
    }
}
