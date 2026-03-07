package kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "관리자용 제증명 발급 신청 목록 응답")
public class AdminRequestHistorySummaryResponseDto {

    @Schema(description = "신청 ID", example = "101")
    private Long requestId;

    @Schema(description = "신청자 ID", example = "17")
    private Long userId;

    @Schema(description = "신청자 한글명", example = "홍길동")
    private String nameKor;

    @Schema(description = "부서명", example = "경영지원부", nullable = true)
    private String departmentName;

    @Schema(description = "직급", example = "사원")
    private String position;

    @Schema(description = "증명서 구분", example = "재직증명서")
    private RequestType requestType;

    @Schema(description = "용도", example = "은행 제출용")
    private String purpose;

    @Schema(description = "발급 부수", example = "2")
    private Integer copies;

    @Schema(description = "발급 희망일", example = "2026-03-10")
    private LocalDate wishDate;

    @Schema(description = "신청일", example = "2026-03-07")
    private LocalDate requestDate;

    @Schema(description = "처리 상태", example = "대기")
    private ApprovalStatus approvalStatus;

    public static AdminRequestHistorySummaryResponseDto from(RequestHistory requestHistory) {
        User requester = requestHistory.getUser();

        return AdminRequestHistorySummaryResponseDto.builder()
                .requestId(requestHistory.getId())
                .userId(requester != null ? requester.getId() : null)
                .nameKor(requestHistory.getName())
                .departmentName(
                        requester != null && requester.getDepartment() != null
                                ? requester.getDepartment().getName().getDescription()
                                : null)
                .position(requestHistory.getPosition())
                .requestType(requestHistory.getRequestType())
                .purpose(requestHistory.getPurpose())
                .copies(requestHistory.getCopies())
                .wishDate(requestHistory.getWishDate())
                .requestDate(requestHistory.getRequestDate())
                .approvalStatus(requestHistory.getApprovalStatus())
                .build();
    }
}
