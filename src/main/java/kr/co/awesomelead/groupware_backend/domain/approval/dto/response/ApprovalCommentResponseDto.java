package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "전자결재 의견글 응답")
public class ApprovalCommentResponseDto {

    @Schema(description = "의견글 ID", example = "1")
    private Long commentId;

    @Schema(description = "문서 ID", example = "10")
    private Long documentId;

    @Schema(description = "작성자 ID", example = "14")
    private Long writerUserId;

    @Schema(description = "작성자 이름", example = "고영민")
    private String writerUserName;

    @Schema(description = "작성자 부서명", example = "경영지원부")
    private String writerDepartmentName;

    @Schema(description = "작성자 직급", example = "사원")
    private String writerPositionName;

    @Schema(description = "의견 내용", example = "확인했습니다.")
    private String content;

    @Schema(description = "작성일시")
    private LocalDateTime createdAt;
}
