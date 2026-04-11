package kr.co.awesomelead.groupware_backend.domain.education.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportStatusUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService.FileDownloadDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    ## 교육 보고서 API

    교육 보고서 생성/조회/수정/상태변경/삭제, 첨부파일 다운로드, 출석(서명) 처리 API입니다.

    ### API별 권한
    - 생성
      - PSM/안전보건: `WRITE_SAFETY`
      - 부서교육: `WRITE_DEPARTMENT_EDUCATION`
    - 목록 조회/상세 조회/출석(서명): 로그인 사용자
    - 부서교육 수정/상태변경/삭제: `WRITE_DEPARTMENT_EDUCATION`
    - 첨부파일 다운로드: 인증 불필요(공개)

    ### 교육 유형(EduType)
    - `PSM`
    - `안전 보건`
    - `부서 교육`
    """)
public class EduReportController {

    private final EduReportService eduReportService;

    @Operation(
            summary = "교육 보고서 생성",
            description =
                    """
            `multipart/form-data`로 교육 보고서를 생성합니다.

            - `requestDto`(JSON 파트)는 필수입니다.
            - `files`(파일 파트)는 선택입니다.
            - PSM/안전보건(`PSM`, `안전 보건`) 생성 시 `categoryId`는 필수입니다.
            - 부서교육(`부서 교육`) 생성 시 `departmentId`를 전달해야 대상 부서가 지정됩니다.
            """,
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    EduReportCreateMultipartRequestDoc
                                                                            .class),
                                            encoding = {
                                                @Encoding(
                                                        name = "requestDto",
                                                        contentType =
                                                                MediaType.APPLICATION_JSON_VALUE),
                                                @Encoding(name = "files", contentType = "*/*")
                                            })))
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
                        responseCode = "400",
                        description = "잘못된 요청",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "카테고리 누락",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "EDUCATION_CATEGORY_REQUIRED",
                "message": "PSM/안전보건 교육 등록 시 카테고리는 필수입니다.",
                "result": null
              }
              """),
                                            @ExampleObject(
                                                    name = "카테고리 타입 불일치",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "INVALID_ARGUMENT",
                "message": "유효하지 않은 ARGUMENT입니다.",
                "result": null
              }
              """),
                                            @ExampleObject(
                                                    name = "파일 크기 초과",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "COMMON400",
                "message": "파일 용량이 제한을 초과했습니다. (서버 설정값 확인 필요)",
                "result": null
              }
              """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "403",
                        description = "권한 없음",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "PSM/안전보건 작성 권한 없음",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
                "message": "PSM/안전보건 작성 권한이 없습니다.",
                "result": null
              }
              """),
                                            @ExampleObject(
                                                    name = "부서교육 관리 권한 없음",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "NO_AUTHORITY_FOR_EDU_REPORT",
                "message": "교육 보고서 관리 권한이 없습니다.",
                "result": null
              }
              """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "리소스 없음",
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
              """),
                                            @ExampleObject(
                                                    name = "카테고리 없음",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "EDUCATION_CATEGORY_NOT_FOUND",
                "message": "해당 교육 카테고리를 찾을 수 없습니다.",
                "result": null
              }
              """)
                                        }))
            })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createEduReport(
            @Parameter(description = "교육 보고서 생성 정보(JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    EduReportRequestDto requestDto,
            @Parameter(description = "첨부 파일 목록(선택)") @RequestPart(value = "files", required = false)
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

    @Schema(name = "EduReportCreateMultipartRequestDoc", description = "교육 보고서 생성 multipart 요청")
    static class EduReportCreateMultipartRequestDoc {

        @Schema(description = "교육 보고서 생성 정보(JSON 파트)", requiredMode = Schema.RequiredMode.REQUIRED)
        public EduReportRequestDto requestDto;

        @ArraySchema(schema = @Schema(type = "string", format = "binary"))
        public List<String> files;
    }

    @Operation(
            summary = "교육 보고서 목록 조회",
            description =
                    """
            교육 보고서 목록을 조회합니다.

            - `type` 미지정 시 전체 유형 조회
            - `categoryId`는 PSM/안전보건 카테고리 필터 용도
            - `departmentName`은 `type=DEPARTMENT` + `WRITE_DEPARTMENT_EDUCATION` 권한 사용자일 때만 유효
            - `WRITE_DEPARTMENT_EDUCATION` 권한이 없는 사용자는 부서교육의 경우 본인 부서 데이터만 조회
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
          {
            "isSuccess": true,
            "code": "COMMON200",
            "message": "요청에 성공했습니다.",
            "result": [
              {
                "id": 202,
                "title": "경영지원부 교육",
                "eduType": "부서 교육",
                "eduDate": "2026-03-16",
                "content": "부서교육 게시글입니다.",
                "attendance": false,
                "pinned": false,
                "signatureRequired": true,
                "status": "OPEN",
                "categoryId": null,
                "categoryName": null
              }
            ]
          }
          """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음",
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
    @GetMapping
    public ResponseEntity<ApiResponse<List<EduReportSummaryDto>>> getEduReports(
            @Parameter(description = "교육 유형 필터", example = "DEPARTMENT")
                    @RequestParam(required = false)
                    EduType type,
            @Parameter(
                            description = "부서명 필터(`type=DEPARTMENT`에서만 사용, 그 외 타입에서는 무시)",
                            example = "SALES_DEPT")
                    @RequestParam(required = false)
                    DepartmentName departmentName,
            @Parameter(description = "카테고리 ID 필터(PSM/안전보건)", example = "1")
                    @RequestParam(required = false)
                    Long categoryId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EduReportSummaryDto> reports =
                eduReportService.getEduReports(
                        type, departmentName, categoryId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(reports));
    }

    @Operation(
            summary = "교육 보고서 상세 조회",
            description =
                    """
            교육 보고서 상세 정보를 조회합니다.

            - `WRITE_DEPARTMENT_EDUCATION` 권한 사용자는 `attendees`, `numberOfPeople`, `numberOfAttendees`를 조회할 수 있습니다.
            - 일반 사용자는 위 필드가 `null`로 반환됩니다.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음",
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

    @Operation(
            summary = "부서교육 수정",
            description =
                    """
            부서교육(`eduType=부서 교육`)을 수정합니다.

            - `WRITE_DEPARTMENT_EDUCATION` 권한 필요
            - `OPEN` 상태에서만 수정 가능
            - 서명(출석) 완료자가 1명이라도 있으면 수정 불가
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples = {
                                    @ExampleObject(
                                            name = "부서교육이 아님",
                                            value =
                                                    """
              {
                "isSuccess": false,
                "code": "INVALID_ARGUMENT",
                "message": "유효하지 않은 ARGUMENT입니다.",
                "result": null
              }
              """),
                                    @ExampleObject(
                                            name = "마감 상태",
                                            value =
                                                    """
              {
                "isSuccess": false,
                "code": "EDU_REPORT_CLOSED",
                "message": "마감된 부서교육입니다.",
                "result": null
              }
              """),
                                    @ExampleObject(
                                            name = "서명 완료자 존재",
                                            value =
                                                    """
              {
                "isSuccess": false,
                "code": "EDU_REPORT_HAS_SIGNED_ATTENDEE",
                "message": "서명 완료자가 존재하여 부서교육을 수정할 수 없습니다.",
                "result": null
              }
              """)
                                })),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
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
            "code": "NO_AUTHORITY_FOR_EDU_REPORT",
            "message": "교육 보고서 관리 권한이 없습니다.",
            "result": null
          }
          """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음",
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
    @PatchMapping("/{eduReportId}")
    public ResponseEntity<ApiResponse<Long>> updateDepartmentEduReport(
            @Parameter(description = "수정할 부서교육 보고서 ID", example = "1") @PathVariable
                    Long eduReportId,
            @Valid @RequestBody EduReportUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateDepartmentEduReport(
                        eduReportId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            summary = "부서교육 상태 변경",
            description =
                    """
            부서교육(`eduType=부서 교육`) 상태를 `OPEN`/`CLOSED`로 변경합니다.

            - `WRITE_DEPARTMENT_EDUCATION` 권한 필요
            - 상태 변경 후 `CLOSED`인 부서교육은 출석(서명)할 수 없습니다.
            """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상태 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                name = "부서교육이 아님",
                                                value =
                                                        """
          {
            "isSuccess": false,
            "code": "INVALID_ARGUMENT",
            "message": "유효하지 않은 ARGUMENT입니다.",
            "result": null
          }
          """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
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
            "code": "NO_AUTHORITY_FOR_EDU_REPORT",
            "message": "교육 보고서 관리 권한이 없습니다.",
            "result": null
          }
          """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @PatchMapping("/{eduReportId}/status")
    public ResponseEntity<ApiResponse<Long>> updateDepartmentEduReportStatus(
            @Parameter(description = "상태 변경할 부서교육 보고서 ID", example = "1") @PathVariable
                    Long eduReportId,
            @Valid @RequestBody EduReportStatusUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateDepartmentEduReportStatus(
                        eduReportId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            summary = "교육 보고서 삭제",
            description = "교육 보고서를 삭제합니다. `WRITE_DEPARTMENT_EDUCATION` 권한이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
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
            "code": "NO_AUTHORITY_FOR_EDU_REPORT",
            "message": "교육 보고서 관리 권한이 없습니다.",
            "result": null
          }
          """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음",
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

    @Operation(summary = "교육 첨부파일 다운로드", description = "교육 보고서 첨부파일을 다운로드합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "다운로드 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                schema = @Schema(type = "string", format = "binary"))),
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

    @Operation(
            summary = "교육 출석(서명) 처리",
            description =
                    """
            교육 보고서 출석을 처리합니다.

            - `signatureRequired=true` 보고서는 PNG 서명 파일(`signature`)이 필수입니다.
            - `signatureRequired=false` 보고서는 파일 없이도 출석 처리됩니다.
            - 동일 사용자 중복 출석은 허용되지 않습니다.
            """,
            requestBody =
                    @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = false,
                            content =
                                    @Content(
                                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    EduReportAttendanceMultipartRequestDoc
                                                                            .class),
                                            encoding =
                                                    @Encoding(
                                                            name = "signature",
                                                            contentType = "image/png"))))
    @ApiResponses(
            value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "출석 처리 성공"),
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
                                                    name = "서명 누락",
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
                                                    name = "서명 포맷 오류",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "INVALID_SIGNATURE_FORMAT",
                "message": "서명은 PNG 파일 형식만 지원합니다.",
                "result": null
              }
              """),
                                            @ExampleObject(
                                                    name = "부서교육 마감 상태",
                                                    value =
                                                            """
              {
                "isSuccess": false,
                "code": "EDU_REPORT_CLOSED",
                "message": "마감된 부서교육입니다.",
                "result": null
              }
              """)
                                        })),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "리소스 없음",
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
    @PostMapping(value = "/{id}/attendance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> markAttendance(
            @Parameter(description = "교육 보고서 ID", example = "1") @PathVariable Long id,
            @Parameter(description = "서명 PNG 파일(signatureRequired=true인 경우 필수)")
                    @RequestPart(value = "signature", required = false)
                    MultipartFile signature,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {

        eduReportService.markAttendance(id, signature, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onNoContent());
    }

    @Schema(name = "EduReportAttendanceMultipartRequestDoc", description = "출석(서명) multipart 요청")
    static class EduReportAttendanceMultipartRequestDoc {

        @Schema(
                description = "서명 PNG 파일(signatureRequired=true인 경우 필수)",
                type = "string",
                format = "binary")
        public String signature;
    }
}
