package kr.co.awesomelead.groupware_backend.domain.annualleave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AdminAnnualLeaveDispatchGroupResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.AnnualLeaveResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.dto.response.ExcelUploadResponseDto;
import kr.co.awesomelead.groupware_backend.domain.annualleave.service.AnnualLeaveService;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/annualleaves")
@RequiredArgsConstructor
public class AnnualLeaveController {

    private final AnnualLeaveService annualLeaveService;

    @Operation(summary = "연차 발송", description = "엑셀 파일과 시트명을 입력하여 연차를 일괄적으로 발송합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "업로드 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                ExcelUploadResponseDto.class)))
            })
    @PatchMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExcelUploadResponseDto>> uploadAnnualLeaveFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam("sheetName") String sheetName,
            @RequestParam("company") Company company,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ExcelUploadResponseDto responseDto =
                annualLeaveService.uploadAnnualLeaveFile(
                        file, sheetName, userDetails.getId(), company);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "연차 조회", description = "연차 정보를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                AnnualLeaveResponseDto.class)))
            })
    @GetMapping
    public ResponseEntity<ApiResponse<AnnualLeaveResponseDto>> getAnnualLeave(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AnnualLeaveResponseDto responseDto = annualLeaveService.getAnnualLeave(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "보낸 연차 목록 조회 (관리자)", description = "관리자가 보낸 연차 이력을 연도 기준 그룹으로 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                AdminAnnualLeaveDispatchGroupResponseDto
                                                                        .class)))
            })
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<AdminAnnualLeaveDispatchGroupResponseDto>>>
            getAnnualLeavesForAdmin(
                    @RequestParam(value = "company", required = false) Company company,
                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AdminAnnualLeaveDispatchGroupResponseDto> responseDto =
                annualLeaveService.getAnnualLeavesForAdmin(userDetails.getId(), company);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "보낸 연차 상세 조회 (관리자)", description = "관리자가 특정 연차 발송 이력을 상세 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                AdminAnnualLeaveDispatchDetailResponseDto
                                                                        .class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": true,
                                "code": "COMMON200",
                                "message": "성공입니다.",
                                "result": {
                                    "dispatchId": 10,
                                    "originalFileName": "2026_연차현황.xlsx",
                                    "sheetName": "2026-06",
                                    "senderName": "김관리",
                                    "senderPosition": "과장",
                                    "createdAt": "2026-05-31T15:29:04",
                                    "fileUrl": "https://...presigned-url",
                                    "title": "6월",
                                    "company": "AWESOME"
                                }
                                }
                                """)))
            })
    @GetMapping("/admin/{dispatchId}")
    public ResponseEntity<ApiResponse<AdminAnnualLeaveDispatchDetailResponseDto>>
            getAnnualLeaveDispatchDetailForAdmin(
                    @PathVariable Long dispatchId,
                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminAnnualLeaveDispatchDetailResponseDto responseDto =
                annualLeaveService.getAnnualLeaveDispatchDetailForAdmin(
                        userDetails.getId(), dispatchId);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(summary = "보낸 연차 수정 (관리자)", description = "관리자가 특정 연차 발송 이력을 새 파일로 수정합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "수정 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                ExcelUploadResponseDto.class)))
            })
    @PutMapping(value = "/admin/{dispatchId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExcelUploadResponseDto>> updateAnnualLeaveDispatch(
            @PathVariable Long dispatchId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("sheetName") String sheetName,
            @RequestParam("company") Company company,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ExcelUploadResponseDto responseDto =
                annualLeaveService.updateAnnualLeaveDispatchForAdmin(
                        userDetails.getId(), dispatchId, file, sheetName, company);
        return ResponseEntity.ok(ApiResponse.onSuccess(responseDto));
    }

    @Operation(
            summary = "연차 정보 재계산 (관리자)",
            description = "남아 있는 연차 발송 이력의 원본 파일 기준으로 사용자 연차 정보를 다시 계산합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "처리 성공")
            })
    @PostMapping("/admin/rebuild")
    public ResponseEntity<ApiResponse<Void>> rebuildAnnualLeavesForAdmin(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        annualLeaveService.rebuildAnnualLeavesForAdmin(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onNoContent("연차 정보가 성공적으로 재계산되었습니다."));
    }

    @Operation(summary = "보낸 연차 삭제 (관리자)", description = "관리자가 특정 연차 발송 이력을 삭제합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "처리 성공")
            })
    @DeleteMapping("/admin/{dispatchId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnualLeaveDispatch(
            @PathVariable Long dispatchId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        annualLeaveService.deleteAnnualLeaveDispatchForAdmin(userDetails.getId(), dispatchId);
        return ResponseEntity.ok(ApiResponse.onNoContent("연차 발송 이력이 성공적으로 삭제되었습니다."));
    }

    @Operation(
            summary = "회사별 당월 연차 발송 현황 조회",
            description = "당월 회사별 연차 발송 여부를 조회합니다. EDIT_EMPLOYEE_INFO 권한 필요.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        example =
                                                                "{\"isSuccess\":true,\"code\":\"COMMON200\",\"message\":\"요청에"
                                                                    + " 성공하였습니다.\",\"result\":{\"어썸리드\":true,\"한국마루이\":false}}")))
            })
    @GetMapping("/admin/dispatch-status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getMonthlyDispatchStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Boolean> result =
                annualLeaveService.getMonthlyDispatchStatus(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}
