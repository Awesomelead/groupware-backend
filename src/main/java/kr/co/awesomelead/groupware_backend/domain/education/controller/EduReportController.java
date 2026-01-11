package kr.co.awesomelead.groupware_backend.domain.education.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportAdminDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.education.repository.EduAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService.FileDownloadDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/edu-reports")
@Tag(
        name = "Education Report",
        description =
                """
            ## 교육 보고서 관리 API

            안전교육, 부서교육 등 사내 교육 보고서의 생성, 조회, 삭제 및 출석 체크 기능을 제공합니다.

            ### 사용되는 Enum 타입
            - **EduType**: PSM, SAFETY(안전 보건), DEPARTMENT(부서 교육)

            ### 권한 안내
            - **작성/삭제 권한**: `WRITE_EDUCATION` 권한이 필요합니다. (401 에러 발생 가능)
            - **관리자 조회**: `ADMIN` 역할(Role)이 필요합니다. (401 에러 발생 가능)
            """)
public class EduReportController {

    private final EduReportService eduReportService;
    private final EduAttachmentRepository eduAttachmentRepository;

    @Operation(summary = "교육 보고서 생성", description = "교육 보고서를 생성합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "201",
                        description = "생성 성공",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ApiResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                    {
                      "isSuccess": true,
                      "code": "COMMON201",
                      "message": "성공적으로 생성되었습니다.",
                      "result": 1
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
                                                        name = "작성 권한 없음",
                                                        value =
                                                                """
                    {
                      "isSuccess": false,
                      "code": "NO_AUTHORITY_FOR_EDU_REPORT",
                      "message": "교육 보고서 작성 권한이 없습니다.",
                      "result": null
                    }
                    """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 또는 부서 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "사용자 없음",
                                                    value =
                                                            """
                    {
                      "isSuccess": false,
                      "code": "USER_NOT_FOUND",
                      "message": "해당 사용자를 찾을 수 없습니다.",
                      "result": null
                    }
                    """),
                                            @ExampleObject(
                                                    name = "부서 없음",
                                                    value =
                                                            """
                    {
                      "isSuccess": false,
                      "code": "DEPARTMENT_NOT_FOUND",
                      "message": "해당 부서를 찾을 수 없습니다.",
                      "result": null
                    }
                    """)
                                        }))
            })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createEduReport(
            @Parameter(description = "교육 보고서 생성 정보 (JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    EduReportRequestDto requestDto,
            @Parameter(description = "첨부 파일 목록") @RequestPart(value = "files", required = false)
                    List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {

        Long reportId = eduReportService.createEduReport(requestDto, files, userDetails.getId());

        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(reportId)
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(reportId));
    }

    @Operation(summary = "교육 보고서 목록 조회", description = "교육 보고서 목록을 조회합니다. 교육 유형으로 필터링할 수 있습니다.")
    @ApiResponses({
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
    @GetMapping
    public ResponseEntity<ApiResponse<List<EduReportSummaryDto>>> getEduReports(
            @Parameter(description = "필터링할 교육 유형 (미지정 시 전체 조회)", example = "LEGAL")
                    @RequestParam(required = false)
                    EduType type,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EduReportSummaryDto> reports =
                eduReportService.getEduReports(type, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(reports));
    }

    @Operation(summary = "교육 보고서 상세 조회", description = "교육 보고서의 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "보고서 또는 사용자 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples = {
                                    @ExampleObject(
                                            name = "보고서 없음",
                                            value =
                                                    """
                    {
                      "isSuccess": false,
                      "code": "EDU_REPORT_NOT_FOUND",
                      "message": "해당 교육 보고서를 찾을 수 없습니다.",
                      "result": null
                    }
                    """),
                                    @ExampleObject(
                                            name = "사용자 없음",
                                            value =
                                                    """
                    {
                      "isSuccess": false,
                      "code": "USER_NOT_FOUND",
                      "message": "해당 사용자를 찾을 수 없습니다.",
                      "result": null
                    }
                    """)
                                }))
    })
    @GetMapping("/{eduReportId}")
    public ResponseEntity<ApiResponse<EduReportDetailDto>> getEduReport(
            @Parameter(description = "조회할 보고서 ID", example = "1") @PathVariable Long eduReportId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        EduReportDetailDto report = eduReportService.getEduReport(eduReportId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(report));
    }

    @Operation(summary = "교육 보고서 삭제", description = "교육 보고서를 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "권한 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                name = "삭제 권한 없음",
                                                value =
                                                        """
                    {
                      "isSuccess": false,
                      "code": "NO_AUTHORITY_FOR_EDU_REPORT",
                      "message": "교육 보고서 작성 권한이 없습니다.",
                      "result": null
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "보고서 또는 사용자 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples = {
                                    @ExampleObject(
                                            name = "보고서 없음",
                                            value =
                                                    """
                    {
                      "isSuccess": false,
                      "code": "EDU_REPORT_NOT_FOUND",
                      "message": "해당 교육 보고서를 찾을 수 없습니다.",
                      "result": null
                    }
                    """),
                                    @ExampleObject(
                                            name = "사용자 없음",
                                            value =
                                                    """
                    {
                      "isSuccess": false,
                      "code": "USER_NOT_FOUND",
                      "message": "해당 사용자를 찾을 수 없습니다.",
                      "result": null
                    }
                    """)
                                }))
    })
    @DeleteMapping("/{eduReportId}")
    public ResponseEntity<ApiResponse<Void>> deleteEduReport(
            @Parameter(description = "삭제할 보고서 ID", example = "1") @PathVariable Long eduReportId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {
        eduReportService.deleteEduReport(eduReportId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.onNoContent());
    }

    // 브라우저 자동 다운로드는 ApiResponse 미적용
    @Operation(summary = "첨부파일 다운로드", description = "교육 보고서 첨부파일을 다운로드합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "다운로드 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "파일 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                    {
                      "isSuccess": false,
                      "code": "EDU_ATTACHMENT_NOT_FOUND",
                      "message": "해당 교육 첨부파일을 찾을 수 없습니다.",
                      "result": null
                    }
                    """)))
    })
    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @Parameter(description = "다운로드할 파일 ID", example = "5") @PathVariable Long id) {
        FileDownloadDto downloadDto = eduReportService.getFileForDownload(id);

        String encodedFileName =
                UriUtils.encode(downloadDto.originalFileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(downloadDto.fileSize())
                .body(downloadDto.fileData());
    }

    //    @GetMapping("/attachments/{id}/download")
    //    public ResponseEntity<Void> downloadAttachment(@PathVariable Long id) {
    //        String downloadUrl = eduReportService.getDownloadUrl(id);
    //
    //        return ResponseEntity.status(HttpStatus.FOUND)
    //            .location(URI.create(downloadUrl))
    //            .build();
    //    }

    @Operation(summary = "출석 체크", description = "png 서명 이미지를 통해 교육 보고서에 대한 출석 체크를 수행합니다.")
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "출석 체크 성공"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "400",
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "이미 출석함",
                                                    value =
                                                            """
                    {
                      "isSuccess": false,
                      "code": "ALREADY_MARKED_ATTENDANCE",
                      "message": "이미 출석이 체크된 교육입니다.",
                      "result": null
                    }
                    """),
                                            @ExampleObject(
                                                    name = "서명 미제공",
                                                    value =
                                                            """
                    {
                      "isSuccess": false,
                      "code": "NO_SIGNATURE_PROVIDED",
                      "message": "서명이 제공되지 않았습니다.",
                      "result": null
                    }
                    """),
                                            @ExampleObject(
                                                    name = "잘못된 서명 형식",
                                                    value =
                                                            """
                    {
                      "isSuccess": false,
                      "code": "INVALID_SIGNATURE_FORMAT",
                      "message": "서명은 PNG 파일 형식만 지원합니다.",
                      "result": null
                    }
                    """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "사용자 또는 보고서 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                    {
                      "isSuccess": false,
                      "code": "EDU_REPORT_NOT_FOUND",
                      "message": "해당 교육 보고서를 찾을 수 없습니다.",
                      "result": null
                    }
                    """)))
            })
    @PostMapping("/{id}/attendance")
    public ResponseEntity<ApiResponse<Void>> markAttendance(
            @Parameter(description = "교육 보고서 ID", example = "1") @PathVariable Long id,
            @Parameter(description = "서명 이미지 파일")
                    @RequestPart(value = "signature", required = false)
                    MultipartFile signature,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {

        eduReportService.markAttendance(id, signature, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onNoContent());
    }

    @Operation(summary = "관리자용 교육 보고서 상세 조회", description = "관리자 권한으로 교육 보고서의 상세 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "관리자 조회 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                    {
                      "isSuccess": true,
                      "code": "COMMON200",
                      "message": "요청에 성공했습니다.",
                      "result": {
                        "id": 1,
                        "title": "2026 안전교육",
                        "attendanceList": [
                          {"userName": "홍길동", "attendedAt": "2026-01-11T16:00:00"}
                        ]
                      }
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
                                                name = "관리자 권한 없음",
                                                value =
                                                        """
                    {
                      "isSuccess": false,
                      "code": "NO_AUTHORITY_FOR_EDU_REPORT",
                      "message": "교육 보고서 작성 권한이 없습니다.",
                      "result": null
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "보고서 또는 사용자 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples = {
                                    @ExampleObject(
                                            name = "보고서 없음",
                                            value =
                                                    """
                    {
                      "isSuccess": false,
                      "code": "EDU_REPORT_NOT_FOUND",
                      "message": "해당 교육 보고서를 찾을 수 없습니다.",
                      "result": null
                    }
                    """),
                                    @ExampleObject(
                                            name = "사용자 없음",
                                            value =
                                                    """
                    {
                      "isSuccess": false,
                      "code": "USER_NOT_FOUND",
                      "message": "해당 사용자를 찾을 수 없습니다.",
                      "result": null
                    }
                    """)
                                }))
    })
    @GetMapping("/{reportId}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EduReportAdminDetailDto>> getEduReportForAdmin(
            @Parameter(description = "조회할 보고서 ID", example = "1") @PathVariable Long reportId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        EduReportAdminDetailDto response =
                eduReportService.getEduReportForAdmin(reportId, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
