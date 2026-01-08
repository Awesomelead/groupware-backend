package kr.co.awesomelead.groupware_backend.domain.annualleave.controller;

import io.swagger.v3.oas.annotations.Operation;

import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AnnualLeaveResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.service.AnnualLeaveService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/annualleaves")
@RequiredArgsConstructor
public class AnnualLeaveController {

    private final AnnualLeaveService annualLeaveService;

    @Operation(summary = "연차 발송", description = "엑셀 파일과 시트명을 입력하여 연차를 일괄적으로 발송합니다.")
    @PatchMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExcelUploadResponseDto>> uploadAnnualLeaveFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("sheetName") String sheetName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ExcelUploadResponseDto responseDto =
                annualLeaveService.uploadAnnualLeaveFile(file, sheetName, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "연차 조회", description = "연차 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<AnnualLeaveResponseDto>> getAnnualLeave(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AnnualLeaveResponseDto responseDto = annualLeaveService.getAnnualLeave(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }
}
