package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전자결재 문서 목록 요약 응답 DTO")
public class ApprovalSummaryResponseDto {

    @Schema(description = "결재 식별자", example = "1")
    private Long id;

    @Schema(description = "문서 번호", example = "기본양식 개발부 20250407-123")
    private String documentNumber;

    @Schema(description = "기안자 이름", example = "홍길동")
    private String drafterName;

    @Schema(description = "문서 제목", example = "휴가 신청서")
    private String title;

    @Schema(description = "결재 상태", example = "PENDING")
    private ApprovalStatus status;

    @Schema(description = "결재 라인 요약", example = "[홍길동 > 김팀장 > 이본부장]")
    private String approvalLine;

    @Schema(description = "상신일", example = "2025-04-07T10:00:00")
    private LocalDateTime draftDate;

    @Schema(description = "완료일(최종 승인/반려일)", example = "2025-04-08T15:30:00")
    private LocalDateTime completedDate;
}
