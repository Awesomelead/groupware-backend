package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "전자결재 첨부파일 응답")
public class ApprovalAttachmentResponseDto {

    @Schema(description = "첨부파일 ID", example = "1")
    private Long attachmentId;

    @Schema(description = "문서 ID", example = "10")
    private Long documentId;

    @Schema(description = "원본 파일명", example = "출장정산서.pdf")
    private String originalFileName;

    @Schema(description = "S3 파일 키")
    private String fileKey;

    @Schema(description = "파일 크기(byte)", example = "102400")
    private Long fileSize;

    @Schema(description = "브라우저 보기 URL")
    private String viewUrl;

    @Schema(description = "다운로드 URL")
    private String downloadUrl;

    @Schema(description = "업로드 사용자 ID", example = "14")
    private Long uploadedByUserId;

    @Schema(description = "업로드 사용자명", example = "고영민")
    private String uploadedByUserName;

    @Schema(description = "업로드 일시")
    private LocalDateTime uploadedAt;
}
