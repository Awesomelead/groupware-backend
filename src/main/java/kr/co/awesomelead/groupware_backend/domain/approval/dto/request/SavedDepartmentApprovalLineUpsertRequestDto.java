package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "부서 결재선 생성/수정 요청")
public class SavedDepartmentApprovalLineUpsertRequestDto {

    @NotBlank
    @Size(max = 150)
    @Schema(description = "결재선 이름", example = "환경안전부 기본 결재선", maxLength = 150)
    private String lineName;

    @Schema(description = "대상 부서 ID (미입력 시 요청자 소속 부서)", example = "3")
    private Long departmentId;

    @Valid
    @Schema(description = "결재선 상세 목록")
    private List<SavedApprovalLineDetailRequestDto> lines;
}
