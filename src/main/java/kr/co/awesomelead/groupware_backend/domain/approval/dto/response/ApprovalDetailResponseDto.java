package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "전자결재 단건 상세 조회 응답 DTO")
public class ApprovalDetailResponseDto {

    @Schema(description = "문서 식별자", example = "1")
    private Long id;

    @Schema(description = "문서 구분", example = "LEAVE")
    private DocumentType documentType;

    @Schema(description = "문서 기본 번호", example = "휴가신청서 개발부 20250407-1")
    private String documentNumber;

    @Schema(description = "문서 상태", example = "PENDING")
    private ApprovalStatus status;

    @Schema(description = "제목", example = "여름 정기 휴가 신청")
    private String title;

    @Schema(description = "본문 (HTML 가능)", example = "수고 많으십니다. 8월 1일~3일 휴가 신청합니다.")
    private String content;

    @Schema(description = "기안자 ID", example = "42")
    private Long drafterId;

    @Schema(description = "기안자 이름", example = "홍길동")
    private String drafterName;

    @Schema(description = "기안 부서명", example = "개발부")
    private String draftDepartmentName;

    @Schema(description = "기안 일시", example = "2025-04-07T10:00:00")
    private LocalDateTime draftDate;

    // 문서 양식에 따른 상세 추가 정보 (예: 휴가 시작일, 끝일, 복지대장 항목 등)
    @Schema(description = "결재 양식별 부가 정보 (JSON/Object 등 자유형식)")
    private Object documentDetails;

    // 결재선 이력 정보
    @Schema(description = "결재 단계 이력 정보 리스트")
    private List<ApprovalStepDetailDto> approvalSteps;

    // 참가자(참조/수신) 이력 정보
    @Schema(description = "참조자 정보 리스트")
    private List<ApprovalParticipantDetailDto> participants;

    // 첨부파일
    @Schema(description = "첨부파일 리스트")
    private List<ApprovalAttachmentDetailDto> attachments;

    @Getter
    @Builder
    public static class ApprovalStepDetailDto {
        private Long id;
        private int sequence;
        private Long approverId;
        private String approverName;
        private String approverDepartmentName;
        private ApprovalStatus status;
        private String comment;
        private LocalDateTime processedAt;
    }

    @Getter
    @Builder
    public static class ApprovalParticipantDetailDto {
        private Long id;
        private Long userId;
        private String userName;
        private String departmentName;
        private String participantType;
    }

    @Getter
    @Builder
    public static class ApprovalAttachmentDetailDto {
        private Long id;
        private String originalFileName;
        private String fileUrl; // S3 Presigned URL 등
        private Long fileSize;
    }
}
