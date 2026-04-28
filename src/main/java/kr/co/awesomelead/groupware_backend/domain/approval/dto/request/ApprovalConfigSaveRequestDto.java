package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "결재선 설정 저장 요청 DTO")
public class ApprovalConfigSaveRequestDto {

    @NotNull
    @Schema(description = "문서 양식 타입", example = "BASIC")
    private DocumentType documentType;

    @Schema(description = "승인자 타겟(부서/사용자). targetUserIds는 순서를 보장합니다.")
    private ApprovalTargetRequestDto approvers = new ApprovalTargetRequestDto();

    @Schema(description = "열람권자 타겟(부서/사용자). 결재 완료 후 열람 가능합니다.")
    private ApprovalTargetRequestDto viewers = new ApprovalTargetRequestDto();

    @Schema(description = "참조자 타겟(부서/사용자). 결재 진행 전 과정에서 열람 가능합니다.")
    private ApprovalTargetRequestDto referrers = new ApprovalTargetRequestDto();

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "결재선 역할별 타겟 요청 DTO")
    public static class ApprovalTargetRequestDto {
        @Schema(description = "대상 부서 ID 목록", example = "[1, 2]")
        private List<Long> targetDepartmentIds = new ArrayList<>();

        @Schema(description = "대상 사용자 ID 목록", example = "[14, 15]")
        private List<Long> targetUserIds = new ArrayList<>();
    }
}
