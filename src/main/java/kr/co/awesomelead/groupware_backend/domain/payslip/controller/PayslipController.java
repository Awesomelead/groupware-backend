package kr.co.awesomelead.groupware_backend.domain.payslip.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.payslip.dto.request.PayslipStatusRequestDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.AdminPayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipDetailDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.dto.response.EmployeePayslipSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.payslip.enums.PayslipStatus;
import kr.co.awesomelead.groupware_backend.domain.payslip.service.PayslipService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
@Tag(
        name = "Payslip",
        description =
                """
            ## 급여명세서 관리 API

            관리자의 급여명세서 일괄 발송, 목록 조회 및 직원의 명세서 확인/반려 기능을 수행합니다.

            ### 사용되는 Enum 타입
            - **PayslipStatus**: 명세서 상태 (PENDING: 확인 대기, APPROVED: 확인 완료, REJECTED: 반려됨)
            """)
public class PayslipController {

    private final PayslipService payslipService;

    @Operation(
            summary = "급여명세서 일괄 발송 (관리자)",
            description = "관리자가 다수의 PDF 파일을 업로드하여 발송합니다. 파일명 형식: '성명_입사일_급여명세서.pdf'")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "발송 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": true,
                                "code": "COMMON204",
                                "message": "급여명세서가 성공적으로 발송되었습니다.",
                                "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "잘못된 파일 형식",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "PDF 형식 위반",
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "ONLY_PDF_ALLOWED",
                                "message": "PDF 파일 형식만 업로드할 수 있습니다.",
                                "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "발송 권한 없음",
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "NO_AUTHORITY_FOR_PAYSLIP",
                                "message": "급여명세서 발송 권한이 없습니다.",
                                "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 찾을 수 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "대상자 없음",
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "USER_NOT_FOUND",
                                "message": "해당 사용자를 찾을 수 없습니다.",
                                "result": null
                                }
                                """)))
            })
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> sendPayslips(
            @Parameter(description = "급여명세서 PDF 파일들", required = true) @RequestPart("payslipFiles")
                    List<MultipartFile> payslipFiles,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {

        payslipService.sendPayslip(payslipFiles, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onNoContent("급여명세서가 성공적으로 발송되었습니다."));
    }

    @Operation(summary = "보낸 명세서 목록 조회 (관리자)", description = "관리자가 상태별로 발송한 명세서 목록을 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "NO_AUTHORITY_FOR_PAYSLIP",
                                "message": "급여명세서 발송 권한이 없습니다.",
                                "result": null
                                }
                                """)))
            })
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<AdminPayslipSummaryDto>>> getPayslipsForAdmin(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "명세서 상태 (생략 시 전체 조회)", example = "PENDING")
                    @RequestParam(required = false)
                    PayslipStatus status) {

        List<AdminPayslipSummaryDto> response =
                payslipService.getPayslipsForAdmin(userDetails.getId(), status);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "보낸 명세서 상세 조회 (관리자)", description = "관리자가 특정 명세서의 상세 정보 및 반려 사유를 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "명세서 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "PAYSLIP_NOT_FOUND",
                                "message": "해당 급여명세서를 찾을 수 없습니다.",
                                "result": null
                                }
                                """)))
            })
    @GetMapping("/admin/{payslipId}")
    public ResponseEntity<ApiResponse<AdminPayslipDetailDto>> getPayslipDetailForAdmin(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "명세서 ID", example = "1") @PathVariable Long payslipId) {

        AdminPayslipDetailDto response =
                payslipService.getPayslipForAdmin(userDetails.getId(), payslipId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "내 명세서 목록 조회 (직원)", description = "직원이 본인의 명세서 목록을 상태별로 조회합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "USER_NOT_FOUND",
                                "message": "해당 사용자를 찾을 수 없습니다.",
                                "result": null
                                }
                                """)))
            })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<EmployeePayslipSummaryDto>>> getMyPayslips(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "명세서 상태 (생략 시 전체 조회)", example = "PENDING")
                    @RequestParam(required = false)
                    PayslipStatus status) {

        List<EmployeePayslipSummaryDto> response =
                payslipService.getPayslips(userDetails.getId(), status);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "내 명세서 상세 조회 (직원)", description = "직원이 특정 급여명세서의 상세 내용을 확인합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "조회 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "접근 권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "NO_AUTHORITY_FOR_VIEW_PAYSLIP",
                                "message": "급여명세서 조회 권한이 없습니다.",
                                "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "명세서 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "PAYSLIP_NOT_FOUND",
                                "message": "해당 급여명세서를 찾을 수 없습니다.",
                                "result": null
                                }
                                """)))
            })
    @GetMapping("/me/{payslipId}")
    public ResponseEntity<ApiResponse<EmployeePayslipDetailDto>> getMyPayslipDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "명세서 ID", example = "1") @PathVariable Long payslipId) {

        EmployeePayslipDetailDto response =
                payslipService.getPayslip(userDetails.getId(), payslipId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "명세서 승인/반려 (직원)", description = "직원이 명세서를 확인 완료하거나 반려합니다. 반려 시 사유 필수.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "처리 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "사유 미입력",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "NO_REJECTION_REASON_PROVIDED",
                                "message": "반려 사유가 제공되지 않았습니다.",
                                "result": null
                                }
                                """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "401",
                        description = "권한 부족",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                {
                                "isSuccess": false,
                                "code": "NO_AUTHORITY_FOR_VIEW_PAYSLIP",
                                "message": "급여명세서 조회 권한이 없습니다.",
                                "result": null
                                }
                                """)))
            })
    @PatchMapping("/me/{payslipId}/response")
    public ResponseEntity<ApiResponse<Void>> respondToPayslip(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "명세서 ID", example = "1") @PathVariable Long payslipId,
            @RequestBody @Valid PayslipStatusRequestDto requestDto) {

        payslipService.respondToPayslip(userDetails.getId(), payslipId, requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
}
