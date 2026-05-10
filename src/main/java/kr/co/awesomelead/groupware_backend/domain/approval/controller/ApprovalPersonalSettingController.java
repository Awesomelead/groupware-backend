package kr.co.awesomelead.groupware_backend.domain.approval.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalPersonalSettingUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalPersonalSettingResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.service.ApprovalPersonalSettingService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/approval-settings")
@Tag(
        name = "전자결재 개인설정",
        description =
                """
        전자결재 > 환경설정 > 개인설정 API
        - 대결지정(기간 포함)
        - 열람권자 기본설정
        - 서명이미지

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
        - ApprovalTargetType
          - USER (사용자)
          - DEPARTMENT (부서)
        """)
public class ApprovalPersonalSettingController {

    private final ApprovalPersonalSettingService approvalPersonalSettingService;

    @Operation(
            summary = "전자결재 개인설정 조회",
            description =
                    """
            현재 로그인 사용자의 개인설정을 조회합니다.

            ### 응답 필드
            - 대결지정: delegateEnabled, delegateUser, delegateStartDate, delegateEndDate
            - 열람권자 기본설정: defaultViewerTargets
            - 서명이미지: signatureImageUrl
            """)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ApprovalPersonalSettingResponseDto>> getMySetting(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalPersonalSettingService.getMySetting(userDetails.getId())));
    }

    @Operation(
            summary = "전자결재 개인설정 저장",
            description =
                    """
            개인설정의 대결지정/열람권자 기본설정을 저장합니다.

            ### 대결지정 규칙
            - delegateEnabled=true 인 경우 delegateUserId, delegateStartDate, delegateEndDate 필수
            - delegateStartDate <= delegateEndDate 여야 함
            - 본인을 대결자로 지정할 수 없음

            ### 열람권자 기본설정 규칙
            - targetType=USER -> targetUserId 필수
            - targetType=DEPARTMENT -> targetDepartmentId 필수
            """)
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ApprovalPersonalSettingResponseDto>> saveMySetting(
            @Valid @RequestBody ApprovalPersonalSettingUpdateRequestDto request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalPersonalSettingService.saveMySetting(
                                userDetails.getId(), request)));
    }

    @Operation(
            summary = "서명이미지 업로드",
            description =
                    """
            전자결재 서명이미지를 업로드합니다.

            ### 업로드 규칙
            - multipart 필드명: signatureImage
            - 허용 확장자: gif, jpg, jpeg, png
            - 최대 파일 크기: 100KB

            ### 응답
            - 저장된 서명이미지 URL
            """)
    @PostMapping(value = "/me/signature-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadSignatureImage(
            @RequestPart("signatureImage") MultipartFile signatureImage,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.onSuccess(
                        approvalPersonalSettingService.uploadSignatureImage(
                                userDetails.getId(), signatureImage)));
    }

    @Operation(
            summary = "서명이미지 삭제",
            description =
                    """
            현재 로그인 사용자의 전자결재 서명이미지를 삭제합니다.
            - 삭제 후 조회 시 signatureImageUrl은 null로 반환됩니다.
            """)
    @DeleteMapping("/me/signature-image")
    public ResponseEntity<ApiResponse<Void>> deleteSignatureImage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        approvalPersonalSettingService.deleteSignatureImage(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onNoContent("서명이미지가 삭제되었습니다."));
    }
}
