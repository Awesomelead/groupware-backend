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

    @Schema(description = "결재자 ID 목록 (순서 보장)", example = "[1, 2, 3]")
    private List<Long> approverIds = new ArrayList<>();

    @Schema(description = "참조자 ID 목록", example = "[4, 5]")
    private List<Long> referrerIds = new ArrayList<>();
}
