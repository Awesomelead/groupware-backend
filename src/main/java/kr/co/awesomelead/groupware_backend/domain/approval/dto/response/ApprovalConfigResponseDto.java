package kr.co.awesomelead.groupware_backend.domain.approval.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결재선 설정 응답 DTO")
public class ApprovalConfigResponseDto {

    @Schema(description = "문서 양식 타입", example = "BASIC")
    private DocumentType documentType;

    @Schema(description = "결재자 ID 목록 (순서 보장)", example = "[1, 2, 3]")
    private List<Long> approverIds;

    @Schema(description = "참조자 ID 목록", example = "[4, 5]")
    private List<Long> referrerIds;

    public static ApprovalConfigResponseDto from(ApprovalLineConfig config) {
        return ApprovalConfigResponseDto.builder()
                .documentType(config.getDocumentType())
                .approverIds(config.getApproverIds())
                .referrerIds(config.getReferrerIds())
                .build();
    }
}
