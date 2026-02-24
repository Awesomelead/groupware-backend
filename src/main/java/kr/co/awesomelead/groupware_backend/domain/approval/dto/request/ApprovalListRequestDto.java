package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalCategory;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ParticipantType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "전자결재 문서 목록 조회 파라미터 DTO")
public class ApprovalListRequestDto {

    @Schema(description = "조회 대분류 (전체, 결재진행, 참조문서, 내 작성)", example = "IN_PROGRESS")
    private ApprovalCategory category;

    @Schema(description = "상태 필터 (ALL 필터링은 null 또는 생략)", example = "WAITING")
    private ApprovalStatus status;

    @Schema(
            description = "참여자 유형 필터 (REFERENCE 카테고리에서 REFERRER 또는 VIEWER 구분용)",
            example = "REFERRER")
    private ParticipantType participantType;

    @Schema(description = "결재 양식 서브 필터", example = "BASIC")
    private DocumentType documentType;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "10")
    private int size = 10;
}
