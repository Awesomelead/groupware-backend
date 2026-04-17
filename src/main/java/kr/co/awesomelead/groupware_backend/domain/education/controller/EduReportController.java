package kr.co.awesomelead.groupware_backend.domain.education.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.DepartmentEduReportCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportStatusUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.EduReportUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.PsmEduReportCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.request.SafetyEduReportCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportDetailDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSignatureStatusDto;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.service.EduReportService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/educations")
public class EduReportController {

    private final EduReportService eduReportService;

    @Operation(
            tags = {"부서교육"},
            summary = "부서 교육 게시물 생성",
            description =
                    """
                `multipart/form-data`로 부서 교육 게시물을 생성합니다.

                - `requestDto`(JSON 파트)는 필수입니다.
                - `files`(파일 파트)는 선택입니다.
                - 부서 교육 관리 권한(`MANAGE_DEPARTMENT_EDUCATION`)이 있어야 생성할 수 있습니다.
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
                                                                    DepartmentEduReportCreateMultipartRequestDoc
                                                                            .class),
                                            encoding = {
                                                @Encoding(
                                                        name = "requestDto",
                                                        contentType =
                                                                MediaType.APPLICATION_JSON_VALUE),
                                                @Encoding(name = "files", contentType = "*/*")
                                            })))
    @ApiResponses({
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
                              "message": "교육 게시물 관리 권한이 없습니다.",
                              "result": null
                            }
                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                            {
                              "isSuccess": false,
                              "code": "DEPARTMENT_NOT_FOUND",
                              "message": "해당 부서를 찾을 수 없습니다.",
                              "result": null
                            }
                            """)))
    })
    @PostMapping(value = "/department", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createDepartmentEduReport(
            @Parameter(description = "부서 교육 게시물 생성 정보(JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    DepartmentEduReportCreateRequestDto requestDto,
            @Parameter(description = "첨부 파일 목록(선택)") @RequestPart(value = "files", required = false)
                    List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {

        Long reportId =
                eduReportService.createDepartmentEduReport(requestDto, files, userDetails.getId());

        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/educations/department/{id}")
                        .buildAndExpand(reportId)
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(reportId));
    }

    @Operation(
            tags = {"PSM"},
            summary = "PSM 게시물 생성",
            description =
                    """
                `multipart/form-data`로 PSM 게시물을 생성합니다.

                - `requestDto`(JSON 파트)는 필수입니다.
                - `files`(파일 파트)는 선택입니다.
                - PSM 관리 권한(`MANAGE_PSM`)이 있어야 생성할 수 있습니다.
                - `companyScope`를 지정하면 해당 회사 게시물, `null`이면 모든 회사 공통 게시물로 생성됩니다.
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
                                                                    PsmEduReportCreateMultipartRequestDoc
                                                                            .class),
                                            encoding = {
                                                @Encoding(
                                                        name = "requestDto",
                                                        contentType =
                                                                MediaType.APPLICATION_JSON_VALUE),
                                                @Encoding(name = "files", contentType = "*/*")
                                            })))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                            {
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "성공적으로 생성되었습니다.",
                              "result": 2
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
                              "code": "NO_AUTHORITY_FOR_PSM_MANAGE",
                              "message": "PSM 관리 권한이 없습니다.",
                              "result": null
                            }
                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                            {
                              "isSuccess": false,
                              "code": "EDUCATION_CATEGORY_NOT_FOUND",
                              "message": "해당 교육 카테고리를 찾을 수 없습니다.",
                              "result": null
                            }
                            """)))
    })
    @PostMapping(value = "/psm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createPsmEduReport(
            @Parameter(description = "PSM 게시물 생성 정보(JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    PsmEduReportCreateRequestDto requestDto,
            @Parameter(description = "첨부 파일 목록(선택)") @RequestPart(value = "files", required = false)
                    List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {

        Long reportId = eduReportService.createPsmEduReport(requestDto, files, userDetails.getId());

        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/educations/psm/{id}")
                        .buildAndExpand(reportId)
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(reportId));
    }

    @Operation(
            tags = {"안전보건"},
            summary = "안전 보건 게시물 생성",
            description =
                    """
                `multipart/form-data`로 안전 보건 게시물을 생성합니다.

                - `requestDto`(JSON 파트)는 필수입니다.
                - `files`(파일 파트)는 선택입니다.
                - 안전 보건 관리 권한(`MANAGE_SAFETY`)이 있어야 생성할 수 있습니다.
                - `companyScope`를 지정하면 해당 회사 게시물, `null`이면 모든 회사 공통 게시물로 생성됩니다.
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
                                                                    SafetyEduReportCreateMultipartRequestDoc
                                                                            .class),
                                            encoding = {
                                                @Encoding(
                                                        name = "requestDto",
                                                        contentType =
                                                                MediaType.APPLICATION_JSON_VALUE),
                                                @Encoding(name = "files", contentType = "*/*")
                                            })))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                            {
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "성공적으로 생성되었습니다.",
                              "result": 3
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
                              "code": "NO_AUTHORITY_FOR_SAFETY_WRITE",
                              "message": "안전 보건 관리 권한이 없습니다.",
                              "result": null
                            }
                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                            {
                              "isSuccess": false,
                              "code": "EDUCATION_CATEGORY_NOT_FOUND",
                              "message": "해당 교육 카테고리를 찾을 수 없습니다.",
                              "result": null
                            }
                            """)))
    })
    @PostMapping(value = "/safety", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createSafetyEduReport(
            @Parameter(description = "안전 보건 게시물 생성 정보(JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    SafetyEduReportCreateRequestDto requestDto,
            @Parameter(description = "첨부 파일 목록(선택)") @RequestPart(value = "files", required = false)
                    List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {

        Long reportId =
                eduReportService.createSafetyEduReport(requestDto, files, userDetails.getId());

        URI location =
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/educations/safety/{id}")
                        .buildAndExpand(reportId)
                        .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(reportId));
    }

    @Schema(
            name = "DepartmentEduReportCreateMultipartRequestDoc",
            description = "부서 교육 게시물 생성 multipart 요청")
    static class DepartmentEduReportCreateMultipartRequestDoc {

        @Schema(
                description = "부서 교육 게시물 생성 정보(JSON 파트)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        public DepartmentEduReportCreateRequestDto requestDto;

        @ArraySchema(schema = @Schema(type = "string", format = "binary"))
        public List<String> files;
    }

    @Schema(name = "PsmEduReportCreateMultipartRequestDoc", description = "PSM 게시물 생성 multipart 요청")
    static class PsmEduReportCreateMultipartRequestDoc {

        @Schema(description = "PSM 게시물 생성 정보(JSON 파트)", requiredMode = Schema.RequiredMode.REQUIRED)
        public PsmEduReportCreateRequestDto requestDto;

        @ArraySchema(schema = @Schema(type = "string", format = "binary"))
        public List<String> files;
    }

    @Schema(
            name = "SafetyEduReportCreateMultipartRequestDoc",
            description = "안전 보건 게시물 생성 multipart 요청")
    static class SafetyEduReportCreateMultipartRequestDoc {

        @Schema(
                description = "안전 보건 게시물 생성 정보(JSON 파트)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        public SafetyEduReportCreateRequestDto requestDto;

        @ArraySchema(schema = @Schema(type = "string", format = "binary"))
        public List<String> files;
    }

    @Schema(name = "EduReportUpdateMultipartRequestDoc", description = "교육 수정 multipart 요청")
    static class EduReportUpdateMultipartRequestDoc {

        @Schema(description = "교육 수정 정보(JSON 파트)", requiredMode = Schema.RequiredMode.REQUIRED)
        public EduReportUpdateRequestDto requestDto;

        @ArraySchema(schema = @Schema(type = "string", format = "binary"))
        public List<String> files;
    }

    @Operation(
            tags = {"부서교육"},
            summary = "부서 교육 게시물 목록 조회",
            description =
                    """
                부서 교육 게시물 목록을 조회합니다.

                - `MANAGE_DEPARTMENT_EDUCATION` 권한 사용자는 `departmentName`으로 전체 부서 대상 필터 조회가 가능합니다.
                - `MANAGE_DEPARTMENT_EDUCATION` 권한이 없는 사용자는 본인 소속 부서 게시물만 조회됩니다.
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
    @GetMapping("/department")
    public ResponseEntity<ApiResponse<List<EduReportSummaryDto>>> getDepartmentEduReports(
            @Parameter(description = "부서명 필터(권한 사용자 전용)", example = "SALES_DEPT")
                    @RequestParam(required = false)
                    DepartmentName departmentName,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EduReportSummaryDto> reports =
                eduReportService.getDepartmentEduReports(departmentName, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(reports));
    }

    @Operation(
            tags = {"PSM"},
            summary = "PSM 게시물 목록 조회",
            description =
                    """
                PSM 게시물 목록을 조회합니다.

                - `MANAGE_PSM` 권한 사용자는 모든 회사의 PSM 게시물을 조회할 수 있습니다.
                - `MANAGE_PSM` 권한이 없는 사용자는 본인 소속 회사의 PSM 게시물만 조회됩니다.
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
                                  "id": 301,
                                  "title": "PSM 변경관리 교육",
                                  "eduType": "PSM",
                                  "eduDate": "2026-04-14",
                                  "content": "PSM 교육 게시글입니다.",
                                  "attendance": false,
                                  "pinned": false,
                                  "signatureRequired": false,
                                  "status": "OPEN",
                                  "categoryId": 2,
                                  "categoryName": "변경관리"
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
                                """)
                                }))
    })
    @GetMapping("/psm")
    public ResponseEntity<ApiResponse<List<EduReportSummaryDto>>> getPsmEduReports(
            @Parameter(description = "카테고리 ID 필터(PSM)", example = "1")
                    @RequestParam(required = false)
                    Long categoryId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EduReportSummaryDto> reports =
                eduReportService.getPsmEduReports(categoryId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(reports));
    }

    @Operation(
            tags = {"안전보건"},
            summary = "안전 보건 게시물 목록 조회",
            description =
                    """
                안전 보건 게시물 목록을 조회합니다.

                - `MANAGE_SAFETY` 권한 사용자는 모든 회사의 안전 보건 게시물을 조회할 수 있습니다.
                - `MANAGE_SAFETY` 권한이 없는 사용자는 본인 소속 회사의 안전 보건 게시물만 조회됩니다.
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
                                  "id": 401,
                                  "title": "안전 보건 정기교육",
                                  "eduType": "안전 보건",
                                  "eduDate": "2026-04-14",
                                  "content": "안전 보건 교육 게시글입니다.",
                                  "attendance": false,
                                  "pinned": false,
                                  "signatureRequired": true,
                                  "status": "OPEN",
                                  "categoryId": 3,
                                  "categoryName": "정기교육"
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
                                """)
                                }))
    })
    @GetMapping("/safety")
    public ResponseEntity<ApiResponse<List<EduReportSummaryDto>>> getSafetyEduReports(
            @Parameter(description = "카테고리 ID 필터(안전 보건)", example = "1")
                    @RequestParam(required = false)
                    Long categoryId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EduReportSummaryDto> reports =
                eduReportService.getSafetyEduReports(categoryId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(reports));
    }

    @Operation(
            tags = {"부서교육"},
            summary = "부서 교육 게시물 상세 조회",
            description =
                    """
                부서 교육 게시물 상세 정보를 조회합니다.

                - `MANAGE_DEPARTMENT_EDUCATION` 권한 사용자는 전체 부서의 부서 교육 게시물을 조회할 수 있습니다.
                - 권한이 없는 사용자는 본인 소속 부서의 게시물만 조회할 수 있습니다.
                - 권한이 없는 사용자가 타 부서 게시물을 조회하면 `EDU_REPORT_NOT_FOUND(404)`가 반환됩니다.
                - `MANAGE_DEPARTMENT_EDUCATION` 권한 사용자는 `attendees`, `numberOfPeople`, `numberOfAttendees`를 조회할 수 있으며, 권한이 없는 사용자는 위 필드가 `null`로 반환됩니다.
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
                                            name = "게시물 없음",
                                            value =
                                                    """
                                {
                                  "isSuccess": false,
                                  "code": "EDU_REPORT_NOT_FOUND",
                                  "message": "해당 교육 게시물을 찾을 수 없습니다.",
                                  "result": null
                                }
                                """),
                                    @ExampleObject(
                                            name = "타 부서 접근",
                                            value =
                                                    """
                                {
                                  "isSuccess": false,
                                  "code": "EDU_REPORT_NOT_FOUND",
                                  "message": "해당 교육 게시물을 찾을 수 없습니다.",
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
    @GetMapping("/department/{educationId}")
    public ResponseEntity<ApiResponse<EduReportDetailDto>> getDepartmentEduReport(
            @Parameter(description = "조회할 부서 교육 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        EduReportDetailDto report =
                eduReportService.getDepartmentEduReport(educationId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(report));
    }

    @Operation(
            tags = {"PSM"},
            summary = "PSM 게시물 상세 조회",
            description =
                    """
                PSM 게시물 상세 정보를 조회합니다.

                - `MANAGE_PSM` 권한 사용자는 모든 회사의 PSM 게시물을 조회할 수 있습니다.
                - 권한이 없는 사용자는 본인 소속 회사 게시물과 공통 게시물(`companyScope=null`)만 조회할 수 있습니다.
                - 권한이 없는 사용자가 타 회사 게시물을 조회하면 `EDU_REPORT_NOT_FOUND(404)`가 반환됩니다.
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
                                            name = "게시물 없음",
                                            value =
                                                    """
                                {
                                  "isSuccess": false,
                                  "code": "EDU_REPORT_NOT_FOUND",
                                  "message": "해당 교육 게시물을 찾을 수 없습니다.",
                                  "result": null
                                }
                                """),
                                    @ExampleObject(
                                            name = "타 회사 접근",
                                            value =
                                                    """
                                {
                                  "isSuccess": false,
                                  "code": "EDU_REPORT_NOT_FOUND",
                                  "message": "해당 교육 게시물을 찾을 수 없습니다.",
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
    @GetMapping("/psm/{educationId}")
    public ResponseEntity<ApiResponse<EduReportDetailDto>> getPsmEduReport(
            @Parameter(description = "조회할 PSM 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        EduReportDetailDto report =
                eduReportService.getPsmEduReport(educationId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(report));
    }

    @Operation(
            tags = {"안전보건"},
            summary = "안전 보건 게시물 상세 조회",
            description =
                    """
                안전 보건 게시물 상세 정보를 조회합니다.

                - `MANAGE_SAFETY` 권한 사용자는 모든 회사의 안전 보건 게시물을 조회할 수 있습니다.
                - 권한이 없는 사용자는 본인 소속 회사 게시물과 공통 게시물(`companyScope=null`)만 조회할 수 있습니다.
                - 권한이 없는 사용자가 타 회사 게시물을 조회하면 `EDU_REPORT_NOT_FOUND(404)`가 반환됩니다.
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
                                            name = "게시물 없음",
                                            value =
                                                    """
                                {
                                  "isSuccess": false,
                                  "code": "EDU_REPORT_NOT_FOUND",
                                  "message": "해당 교육 게시물을 찾을 수 없습니다.",
                                  "result": null
                                }
                                """),
                                    @ExampleObject(
                                            name = "타 회사 접근",
                                            value =
                                                    """
                                {
                                  "isSuccess": false,
                                  "code": "EDU_REPORT_NOT_FOUND",
                                  "message": "해당 교육 게시물을 찾을 수 없습니다.",
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
    @GetMapping("/safety/{educationId}")
    public ResponseEntity<ApiResponse<EduReportDetailDto>> getSafetyEduReport(
            @Parameter(description = "조회할 안전 보건 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        EduReportDetailDto report =
                eduReportService.getSafetyEduReport(educationId, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(report));
    }

    @Operation(
            tags = {"부서교육"},
            summary = "부서 교육 게시물 수정",
            description =
                    """
                `multipart/form-data`로 부서 교육 게시물을 수정합니다.

                - 부서 교육 관리 권한(`MANAGE_DEPARTMENT_EDUCATION`)이 필요합니다.
                - `OPEN` 상태에서만 수정 가능합니다.
                - 출석 완료자가 1명이라도 있으면 수정할 수 없습니다.
                - `requestDto`(JSON 파트)는 필수이며, `files`(파일 파트)는 선택입니다.
                - `requestDto.deleteAttachmentIds`로 기존 첨부파일 삭제가 가능합니다.
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
                                                                    EduReportUpdateMultipartRequestDoc
                                                                            .class),
                                            encoding = {
                                                @Encoding(
                                                        name = "requestDto",
                                                        contentType =
                                                                MediaType.APPLICATION_JSON_VALUE),
                                                @Encoding(name = "files", contentType = "*/*")
                                            })))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @PatchMapping(
            value = "/department/{educationId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateDepartmentEducation(
            @Parameter(description = "수정할 부서 교육 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(description = "부서 교육 수정 정보(JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    EduReportUpdateRequestDto requestDto,
            @Parameter(description = "추가할 첨부 파일 목록(선택)")
                    @RequestPart(value = "files", required = false)
                    List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateDepartmentEduReport(
                        educationId, requestDto, files, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Hidden
    @PatchMapping(value = "/department/{educationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateDepartmentEducationJsonFallback(
            @PathVariable Long educationId,
            @Valid @RequestBody EduReportUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateDepartmentEduReport(
                        educationId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            tags = {"PSM"},
            summary = "PSM 게시물 수정",
            description =
                    """
                `multipart/form-data`로 PSM 게시물을 수정합니다.

                - PSM 관리 권한(`MANAGE_PSM`)이 필요합니다.
                - `OPEN` 상태에서만 수정 가능합니다.
                - 출석 완료자가 1명이라도 있으면 수정할 수 없습니다.
                - `requestDto`(JSON 파트)는 필수이며, `files`(파일 파트)는 선택입니다.
                - `requestDto.deleteAttachmentIds`로 기존 첨부파일 삭제가 가능합니다.
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
                                                                    EduReportUpdateMultipartRequestDoc
                                                                            .class),
                                            encoding = {
                                                @Encoding(
                                                        name = "requestDto",
                                                        contentType =
                                                                MediaType.APPLICATION_JSON_VALUE),
                                                @Encoding(name = "files", contentType = "*/*")
                                            })))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @PatchMapping(value = "/psm/{educationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updatePsmEducation(
            @Parameter(description = "수정할 PSM 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(description = "PSM 수정 정보(JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    EduReportUpdateRequestDto requestDto,
            @Parameter(description = "추가할 첨부 파일 목록(선택)")
                    @RequestPart(value = "files", required = false)
                    List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updatePsmEduReport(
                        educationId, requestDto, files, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Hidden
    @PatchMapping(value = "/psm/{educationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Long>> updatePsmEducationJsonFallback(
            @PathVariable Long educationId,
            @Valid @RequestBody EduReportUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updatePsmEduReport(educationId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            tags = {"안전보건"},
            summary = "안전 보건 게시물 수정",
            description =
                    """
                `multipart/form-data`로 안전 보건 게시물을 수정합니다.

                - 안전 보건 관리 권한(`MANAGE_SAFETY`)이 필요합니다.
                - `OPEN` 상태에서만 수정 가능합니다.
                - 출석 완료자가 1명이라도 있으면 수정할 수 없습니다.
                - `requestDto`(JSON 파트)는 필수이며, `files`(파일 파트)는 선택입니다.
                - `requestDto.deleteAttachmentIds`로 기존 첨부파일 삭제가 가능합니다.
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
                                                                    EduReportUpdateMultipartRequestDoc
                                                                            .class),
                                            encoding = {
                                                @Encoding(
                                                        name = "requestDto",
                                                        contentType =
                                                                MediaType.APPLICATION_JSON_VALUE),
                                                @Encoding(name = "files", contentType = "*/*")
                                            })))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @PatchMapping(value = "/safety/{educationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateSafetyEducation(
            @Parameter(description = "수정할 안전 보건 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(description = "안전 보건 수정 정보(JSON)", required = true)
                    @RequestPart("requestDto")
                    @Valid
                    EduReportUpdateRequestDto requestDto,
            @Parameter(description = "추가할 첨부 파일 목록(선택)")
                    @RequestPart(value = "files", required = false)
                    List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateSafetyEduReport(
                        educationId, requestDto, files, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Hidden
    @PatchMapping(value = "/safety/{educationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateSafetyEducationJsonFallback(
            @PathVariable Long educationId,
            @Valid @RequestBody EduReportUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateSafetyEduReport(
                        educationId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            tags = {"부서교육"},
            summary = "부서 교육 게시물 상태 변경",
            description =
                    """
                부서 교육 게시물 상태를 `OPEN`/`CLOSED`로 변경합니다.

                - 부서 교육 관리 권한(`MANAGE_DEPARTMENT_EDUCATION`)이 필요합니다.
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상태 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @PatchMapping("/department/{educationId}/status")
    public ResponseEntity<ApiResponse<Long>> updateDepartmentEducationStatus(
            @Parameter(description = "상태 변경할 부서 교육 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Valid @RequestBody EduReportStatusUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateDepartmentEduReportStatus(
                        educationId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            tags = {"PSM"},
            summary = "PSM 게시물 상태 변경",
            description =
                    """
                PSM 게시물 상태를 `OPEN`/`CLOSED`로 변경합니다.

                - PSM 관리 권한(`MANAGE_PSM`)이 필요합니다.
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상태 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @PatchMapping("/psm/{educationId}/status")
    public ResponseEntity<ApiResponse<Long>> updatePsmEducationStatus(
            @Parameter(description = "상태 변경할 PSM 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Valid @RequestBody EduReportStatusUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updatePsmEduReportStatus(
                        educationId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            tags = {"안전보건"},
            summary = "안전 보건 게시물 상태 변경",
            description =
                    """
                안전 보건 게시물 상태를 `OPEN`/`CLOSED`로 변경합니다.

                - 안전 보건 관리 권한(`MANAGE_SAFETY`)이 필요합니다.
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "상태 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @PatchMapping("/safety/{educationId}/status")
    public ResponseEntity<ApiResponse<Long>> updateSafetyEducationStatus(
            @Parameter(description = "상태 변경할 안전 보건 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Valid @RequestBody EduReportStatusUpdateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long updatedId =
                eduReportService.updateSafetyEduReportStatus(
                        educationId, requestDto, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(
            tags = {"부서교육"},
            summary = "부서 교육 게시물 삭제",
            description = "부서 교육 게시물을 삭제합니다. 부서 교육 관리 권한(`MANAGE_DEPARTMENT_EDUCATION`)이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @DeleteMapping("/department/{educationId}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartmentEduReport(
            @Parameter(description = "삭제할 부서 교육 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        eduReportService.deleteDepartmentEduReport(educationId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.onNoContent());
    }

    @Operation(
            tags = {"PSM"},
            summary = "PSM 게시물 삭제",
            description = "PSM 게시물을 삭제합니다. PSM 관리 권한(`MANAGE_PSM`)이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @DeleteMapping("/psm/{educationId}")
    public ResponseEntity<ApiResponse<Void>> deletePsmEduReport(
            @Parameter(description = "삭제할 PSM 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        eduReportService.deletePsmEduReport(educationId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.onNoContent());
    }

    @Operation(
            tags = {"안전보건"},
            summary = "안전 보건 게시물 삭제",
            description = "안전 보건 게시물을 삭제합니다. 안전 보건 관리 권한(`MANAGE_SAFETY`)이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @DeleteMapping("/safety/{educationId}")
    public ResponseEntity<ApiResponse<Void>> deleteSafetyEduReport(
            @Parameter(description = "삭제할 안전 보건 게시물 ID", example = "1") @PathVariable
                    Long educationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        eduReportService.deleteSafetyEduReport(educationId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.onNoContent());
    }

    @Operation(
            tags = {"부서교육"},
            summary = "부서 교육 게시물 출석(서명) 처리",
            description =
                    """
                부서 교육 게시물 출석을 처리합니다.

                - `signatureRequired=true` 게시물은 PNG 서명 파일(`signature`)이 필수입니다.
                - `signatureRequired=false` 게시물은 파일 없이도 출석 처리됩니다.
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
                        description = "잘못된 요청"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "404",
                        description = "리소스 없음")
            })
    @PostMapping(
            value = "/department/{educationId}/attendance",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> markDepartmentAttendance(
            @Parameter(description = "부서 교육 게시물 ID", example = "1") @PathVariable Long educationId,
            @Parameter(description = "서명 PNG 파일(signatureRequired=true인 경우 필수)")
                    @RequestPart(value = "signature", required = false)
                    MultipartFile signature,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
            throws IOException {
        eduReportService.markDepartmentAttendance(educationId, signature, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onNoContent());
    }

    @Operation(
            tags = {"부서교육", "PSM", "안전보건"},
            summary = "서명 현황 목록 조회",
            description =
                    """
                교육 보고서의 대상 직원별 서명 현황을 조회합니다.

                - 교육 유형에 따라 권한 검증이 이루어집니다.
                  - 부서교육: `MANAGE_DEPARTMENT_EDUCATION`
                  - PSM: `MANAGE_PSM`
                  - 안전보건: `MANAGE_SAFETY`
                - `name` 파라미터로 한글명(nameKor) 또는 영문명(nameEng)에 대한 부분 일치 검색이 가능합니다.
                """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "리소스 없음")
    })
    @GetMapping("/{educationId}/signatures")
    public ResponseEntity<ApiResponse<List<EduReportSignatureStatusDto>>> getSignatureStatuses(
            @Parameter(description = "교육 보고서 ID", example = "1") @PathVariable Long educationId,
            @Parameter(description = "직원명 검색 필터 (한글명/영문명 부분 일치)", example = "홍")
                    @RequestParam(required = false)
                    String name,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<EduReportSignatureStatusDto> result =
                eduReportService.getSignatureStatuses(educationId, name, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
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
