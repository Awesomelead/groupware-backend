package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.SavedDepartmentApprovalLineUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.SavedPersonalApprovalLineUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.SavedApprovalLineResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.service.SavedApprovalLineService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/approval-lines")
@Tag(
        name = "전자결재 결재선",
        description =
                """
        개인 결재선/부서 결재선 관리 API

        ### 권한 정보
        - 로그인 필요
        - 명시된 @PreAuthorize 없음 (서비스 레이어 권한 검증)

        ### 사용 Enum
        - Company
          - AWESOME (어썸리드)
          - MARUI (마루이)
        - JobType
          - FIELD (현장직)
          - MANAGEMENT (관리직)
        - Position
          - CEO (대표이사)
          - VICE_PRESIDENT (부사장)
          - SENIOR_MANAGING_DIRECTOR (전무이사)
          - MANAGING_DIRECTOR (상무이사)
          - DIRECTOR (이사)
          - GENERAL_MANAGER (부장)
          - DEPUTY_GENERAL_MANAGER (차장)
          - MANAGER (과장)
          - ASSISTANT_MANAGER (대리)
          - SENIOR_STAFF (주임)
          - STAFF (사원)
          - SECTION_HEAD (반장)
          - ADVISOR (전문위원)
          - SECURITY_GUARD (경비원)
        - Role
          - ADMIN (관리자)
          - USER (일반 사용자)
          - MASTER_ADMIN (마스터 관리자)
        - Status
          - PENDING
          - AVAILABLE
          - SUSPENDED
        - ApprovalSavedLineType
          - PERSONAL (개인 결재선)
          - DEPARTMENT (부서 결재선)
        - ApprovalRouteRole
          - APPROVAL_LINE (결재선)
          - AGREEMENT_REQUIRED (합의부서 필수)
          - AGREEMENT_OPTIONAL (합의부서 선택)
          - REFERENCE (참조자)
          - VIEWER (열람권자)
          - RECEIVER_DEPARTMENT (수신부서)
        - ApprovalTargetType
          - USER (사용자)
          - DEPARTMENT (부서)

        ### 타겟 지정 규칙
        - targetType=USER -> targetUserId 필수
        - targetType=DEPARTMENT -> targetDepartmentId 필수
        """)
public class SavedApprovalLineController {

    private final SavedApprovalLineService savedApprovalLineService;

    @Operation(summary = "개인 결재선 목록 조회")
    @GetMapping("/personal")
    public ResponseEntity<ApiResponse<List<SavedApprovalLineResponseDto>>> getPersonalLines(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(savedApprovalLineService.getPersonalLines(userDetails.getId())));
    }

    @Operation(summary = "개인 결재선 상세 조회")
    @GetMapping("/personal/{lineId}")
    public ResponseEntity<ApiResponse<SavedApprovalLineResponseDto>> getPersonalLine(
            @PathVariable Long lineId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        savedApprovalLineService.getPersonalLine(userDetails.getId(), lineId)));
    }

    @Operation(
            summary = "개인 결재선 생성",
            description =
                    """
            사용자가 자주 쓰는 결재선을 본인 계정에 저장합니다.
            - targetType=USER면 targetUserId, targetType=DEPARTMENT면 targetDepartmentId를 입력해야 합니다.
            """)
    @PostMapping("/personal")
    public ResponseEntity<ApiResponse<Long>> createPersonalLine(
            @Valid @RequestBody SavedPersonalApprovalLineUpsertRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long id = savedApprovalLineService.createPersonalLine(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(id));
    }

    @Operation(summary = "개인 결재선 수정")
    @PutMapping("/personal/{lineId}")
    public ResponseEntity<ApiResponse<Void>> updatePersonalLine(
            @PathVariable Long lineId,
            @Valid @RequestBody SavedPersonalApprovalLineUpsertRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        savedApprovalLineService.updatePersonalLine(userDetails.getId(), lineId, request);
        return ResponseEntity.ok(ApiResponse.onNoContent("개인 결재선이 수정되었습니다."));
    }

    @Operation(summary = "개인 결재선 삭제")
    @DeleteMapping("/personal/{lineId}")
    public ResponseEntity<ApiResponse<Void>> deletePersonalLine(
            @PathVariable Long lineId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        savedApprovalLineService.deletePersonalLine(userDetails.getId(), lineId);
        return ResponseEntity.ok(ApiResponse.onNoContent("개인 결재선이 삭제되었습니다."));
    }

    @Operation(summary = "부서 결재선 목록 조회")
    @GetMapping("/department")
    public ResponseEntity<ApiResponse<List<SavedApprovalLineResponseDto>>> getDepartmentLines(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(savedApprovalLineService.getDepartmentLines(userDetails.getId())));
    }

    @Operation(summary = "부서 결재선 상세 조회")
    @GetMapping("/department/{lineId}")
    public ResponseEntity<ApiResponse<SavedApprovalLineResponseDto>> getDepartmentLine(
            @PathVariable Long lineId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        savedApprovalLineService.getDepartmentLine(userDetails.getId(), lineId)));
    }

    @Operation(
            summary = "부서 결재선 생성",
            description =
                    """
            MANAGE_APPROVAL_LINE 권한 사용자가 부서 공용 결재선을 생성합니다.

            ### 권한
            - 생성/수정/삭제: MANAGE_APPROVAL_LINE
            - MASTER_ADMIN, CEO는 타 부서 결재선까지 관리 가능
            - 일반 권한자는 본인 부서 결재선만 관리 가능

            ### 사용 Enum
            - role: APPROVAL_LINE, AGREEMENT_REQUIRED, AGREEMENT_OPTIONAL, REFERENCE, VIEWER, RECEIVER_DEPARTMENT
            - targetType: USER, DEPARTMENT

            ### 타겟 지정 규칙
            - targetType=USER -> targetUserId 필수
            - targetType=DEPARTMENT -> targetDepartmentId 필수
            """)
    @PostMapping("/department")
    public ResponseEntity<ApiResponse<Long>> createDepartmentLine(
            @Valid @RequestBody SavedDepartmentApprovalLineUpsertRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long id = savedApprovalLineService.createDepartmentLine(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.onSuccess(id));
    }

    @Operation(
            summary = "부서 결재선 수정",
            description =
                    """
            부서 공용 결재선을 수정합니다.
            - targetType=USER -> targetUserId 필수
            - targetType=DEPARTMENT -> targetDepartmentId 필수
            """)
    @PutMapping("/department/{lineId}")
    public ResponseEntity<ApiResponse<Void>> updateDepartmentLine(
            @PathVariable Long lineId,
            @Valid @RequestBody SavedDepartmentApprovalLineUpsertRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        savedApprovalLineService.updateDepartmentLine(userDetails.getId(), lineId, request);
        return ResponseEntity.ok(ApiResponse.onNoContent("부서 결재선이 수정되었습니다."));
    }

    @Operation(summary = "부서 결재선 삭제")
    @DeleteMapping("/department/{lineId}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartmentLine(
            @PathVariable Long lineId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        savedApprovalLineService.deleteDepartmentLine(userDetails.getId(), lineId);
        return ResponseEntity.ok(ApiResponse.onNoContent("부서 결재선이 삭제되었습니다."));
    }
}
