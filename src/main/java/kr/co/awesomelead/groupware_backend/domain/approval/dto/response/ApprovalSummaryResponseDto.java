package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalStep;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
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

    public ApprovalSummaryResponseDto(Approval approval, Long viewerId) {
        this.id = approval.getId();
        this.documentNumber = approval.getDocumentNumber();
        this.drafterName = approval.getDrafter().getDisplayName();
        this.title = approval.getTitle();
        this.status = approval.getDisplayStatus(viewerId);

        // [작성자 > 승인자1 > 승인자2 ...] 형태로 결재라인 문자열 조합
        StringBuilder sb = new StringBuilder("[");
        sb.append(this.drafterName);

        List<ApprovalStep> sortedSteps =
                approval.getSteps().stream()
                        .sorted((s1, s2) -> Integer.compare(s1.getSequence(), s2.getSequence()))
                        .collect(Collectors.toList());

        for (ApprovalStep step : sortedSteps) {
            sb.append(" > ").append(step.getApprover().getDisplayName());
        }
        sb.append("]");
        this.approvalLine = sb.toString();

        // 시간 정보 가공 (임시로 생성일=draftDate 로 간주)
        this.draftDate = approval.getCreatedAt();

        // 최종 승인 또는 반려인 경우 마지막 단계의 처리일을 완료일로 세팅
        if (this.status == ApprovalStatus.APPROVED || this.status == ApprovalStatus.REJECTED) {
            this.completedDate =
                    sortedSteps.isEmpty()
                            ? null
                            : sortedSteps.get(sortedSteps.size() - 1).getProcessedAt();
        } else {
            this.completedDate = null;
        }
    }
}
