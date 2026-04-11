package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalConfigSaveRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalConfigResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalConfigService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approval-configs")
@RequiredArgsConstructor
@Tag(name = "ApprovalConfig", description = "문서 양식별 기본 결재선 설정 API")
public class ApprovalConfigController {

    private final ApprovalConfigService approvalConfigService;

    @Operation(
            summary = "결재선 설정 저장",
            description =
                    "문서 양식(DocumentType)별 기본 결재자/참조자 ID 목록을 저장합니다. 기존 설정이 있으면 덮어씁니다."
                            + " `MANAGE_APPROVAL_LINE` 권한이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "결재선 설정 관리 권한 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ApprovalConfigResponseDto>> saveConfig(
            @Valid @RequestBody ApprovalConfigSaveRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApprovalConfigResponseDto response =
                approvalConfigService.saveConfig(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "전체 결재선 설정 조회", description = "모든 문서 양식에 대한 기본 결재선 설정을 목록으로 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApprovalConfigResponseDto>>> getAllConfigs() {
        return ResponseEntity.ok(ApiResponse.onSuccess(approvalConfigService.getAllConfigs()));
    }

    @Operation(
            summary = "결재선 설정 조회",
            description =
                    "문서 양식(DocumentType)에 해당하는 기본 결재선 설정을 조회합니다."
                            + " 설정이 없는 경우 빈 목록을 반환합니다. 인증된 사용자라면 누구나 조회할 수 있습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공")
    })
    @GetMapping("/{documentType}")
    public ResponseEntity<ApiResponse<ApprovalConfigResponseDto>> getConfig(
            @Parameter(description = "문서 양식 타입", example = "BASIC") @PathVariable
                    DocumentType documentType) {
        ApprovalConfigResponseDto response = approvalConfigService.getConfig(documentType);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
