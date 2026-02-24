package kr.co.awesomelead.groupware_backend.domain.notice.controller;

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
import java.io.IOException;
import java.net.URI;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeSearchConditionDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeDetailDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.service.NoticeService;
import kr.co.awesomelead.groupware_backend.domain.user.dto.CustomUserDetails;
import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(
    name = "Notice",
    description =
        """
            ## 공지사항 관리 API
            
            상시공지, 식단표, 기타 공지사항의 생성, 조회, 수정, 삭제 기능을 제공합니다.
            
            ### 사용되는 Enum 타입
            - **NoticeType**: 공지사항 유형 (REGULAR: 상시공지, MENU: 식단표, ETC: 기타)
            
            ### 권한
            - 공지사항 작성/수정/삭제: WRITE_NOTICE 권한 필요
            - 공지사항 조회: 모든 사용자 가능
            """)
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(
        summary = "공지 생성",
        description =
            """
                새로운 공지를 생성합니다. 첨부파일을 포함할 수 있습니다.
                
                **공지 대상 설정 로직**:
                1. **회사(targetCompanies)**: 선택된 회사 소속 전체 인원을 대상으로 합니다.
                2. **부서(targetDepartmentIds)**: 선택된 부서 및 그 하위 부서의 모든 인원을 대상으로 합니다.
                3. **개인(targetUserIds)**: 특정 유저를 직접 대상으로 지정합니다.
                
                *위 세 조건은 **합집합(OR)**으로 계산되어 최종 공지 대상자(NoticeTarget)가 결정됩니다.*
                """,
        requestBody =
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content =
            @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                encoding = {
                    @Encoding(
                        name = "requestDto",
                        contentType = MediaType.APPLICATION_JSON_VALUE),
                    @Encoding(name = "files", contentType = "image/*")
                })))
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "공지 생성 성공",
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
                    examples =
                    @ExampleObject(
                        name = "입력값 검증 실패",
                        value =
                            """
                                {
                                "isSuccess": false,
                                "code": "COMMON400",
                                "message": "입력값이 유효하지 않습니다.",
                                "result": { "title": "공지사항 제목은 필수입니다." }
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        name = "회사 미선택 오류",
                        value =
                            """
                                {
                                "isSuccess": false,
                                "code": "COMMON400",
                                "message": "공지 대상 회사는 최소 하나 이상 선택해야 합니다.",
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
                                "code": "NO_AUTHORITY_FOR_NOTICE",
                                "message": "공지사항 작성 권한이 없습니다.",
                                "result": null
                                }
                                """))),
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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createNotice(
        @Parameter(
            description = "공지사항 생성 정보 (JSON)",
            required = true,
            schema = @Schema(implementation = NoticeCreateRequestDto.class))
        @RequestPart("requestDto")
        @Valid
        NoticeCreateRequestDto requestDto,
        @Parameter(description = "첨부 파일 목록 (여러 파일 선택 가능)")
        @RequestPart(value = "files", required = false)
        List<MultipartFile> files,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
        throws IOException {

        Long noticeId = noticeService.createNotice(requestDto, files, userDetails.getId());

        URI location =
            ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(noticeId)
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(noticeId));
    }


    @Operation(
        summary = "공지 목록 조회",
        description =
            """
                사용자가 열람 가능한 공지 목록을 조회합니다.
                
                **필터링 원칙**:
                - **일반 유저**: 공지 생성 시점에 대상자(NoticeTarget)로 포함된 공지만 노출됩니다.
                - **관리자(ACCESS_NOTICE)**: 모든 공지를 제약 없이 조회할 수 있습니다.
                - 페이징 및 조건 검색(제목, 내용 등)을 지원합니다.
                """)
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
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
                                    "content": [
                                      {
                                        "id": 1,
                                        "type": "REGULAR",
                                        "title": "2026년 신년 휴무 안내",
                                        "isPinned": true,
                                        "updatedDate": "2026-01-01T09:00:00"
                                      },
                                      {
                                        "id": 5,
                                        "type": "MENU",
                                        "title": "1월 식단표 안내",
                                        "isPinned": false,
                                        "updatedDate": "2026-01-10T14:30:00"
                                      }
                                    ],
                                    "pageable": {
                                      "pageNumber": 0,
                                      "pageSize": 10,
                                      "sort": { "empty": false, "sorted": true, "unsorted": false },
                                      "offset": 0,
                                      "paged": true,
                                      "unpaged": false
                                    },
                                    "totalElements": 15,
                                    "totalPages": 2,
                                    "last": false,
                                    "size": 10,
                                    "number": 0,
                                    "sort": { "empty": false, "sorted": true, "unsorted": false },
                                    "numberOfElements": 10,
                                    "first": true,
                                    "empty": false
                                  }
                                }
                                """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content =
                @Content(
                    mediaType = "application/json",
                    examples =
                    @ExampleObject(
                        value =
                            """
                                {
                                  "isSuccess": false,
                                  "code": "INTERNAL_SERVER_ERROR",
                                  "message": "서버 내부 오류가 발생했습니다.",
                                  "result": null
                                }
                                """)))
        })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NoticeSummaryDto>>> getNotices(
        @ParameterObject NoticeSearchConditionDto condition,
        @ParameterObject @PageableDefault(page = 0, size = 10) Pageable pageable,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<NoticeSummaryDto> notices =
            noticeService.getNoticesByType(condition, userDetails.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.onSuccess(notices));
    }

    @Operation(summary = "공지 상세 조회", description = "특정 공지의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
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
                                "title": "2025년 1월 전체 회의 안내",
                                "content": "오는 1월 15일 오후 2시에 전체 회의가 있습니다.",
                                "authorName": "홍길동",
                                "updatedDate": "2025-01-10T14:30:00",
                                "viewCount": 43,
                                "attachments": [
                                  {
                                    "id": 1,
                                    "originalFileName": "회의자료.pdf",
                                    "fileSize": 1048576,
                                    "viewUrl": "https://s3.../uuid_file.pdf"
                                  }
                                ]
                              }
                            }
                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "공지사항 없음",
            content =
            @Content(
                mediaType = "application/json",
                examples =
                @ExampleObject(
                    value =
                        """
                            {
                              "isSuccess": false,
                              "code": "NOTICE_NOT_FOUND",
                              "message": "해당 공지사항을 찾을 수 없습니다.",
                              "result": null
                            }
                            """)))
    })
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailDto>> getNotice(
        @Parameter(description = "조회할 공지사항 ID", example = "1", required = true) @PathVariable
        Long noticeId) {
        NoticeDetailDto dto = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.onSuccess(dto));
    }

    @Operation(summary = "공지 삭제", description = "특정 공지를 삭제합니다. 첨부파일도 함께 S3에서 삭제됩니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "삭제 성공",
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
                              "code": "COMMON204",
                              "message": "성공적으로 처리되었습니다.",
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
                    value =
                        """
                            {
                              "isSuccess": false,
                              "code": "NO_AUTHORITY_FOR_NOTICE",
                              "message": "공지사항 작성 권한이 없습니다.",
                              "result": null
                            }
                            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "공지사항 없음",
            content =
            @Content(
                mediaType = "application/json",
                examples =
                @ExampleObject(
                    value =
                        """
                            {
                              "isSuccess": false,
                              "code": "NOTICE_NOT_FOUND",
                              "message": "해당 공지사항을 찾을 수 없습니다.",
                              "result": null
                            }
                            """)))
    })
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
        @Parameter(description = "삭제할 공지사항 ID", example = "1", required = true) @PathVariable
        Long noticeId,
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        noticeService.deleteNotice(userDetails.getId(), noticeId);
        return ResponseEntity.ok().body(ApiResponse.onNoContent());
    }

    @Operation(
        summary = "공지 수정",
        description = "특정 공지를 수정합니다. JSON(내용만) 또는 multipart(내용+파일) 수정을 지원합니다.",
        requestBody =
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = {
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = NoticeUpdateRequestDto.class),
                    examples = {
                        @ExampleObject(
                            name = "JSON 요청 예시",
                            value =
                                """
                                    {
                                      "title": "2025년 2월 전체 회의 안내 (수정)",
                                      "content": "회의 시간이 오후 3시로 변경되었습니다.",
                                      "pinned": true,
                                      "attachmentsIdsToRemove": [1]
                                    }
                                    """)
                    }),
                @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = NoticeUpdateMultipartRequestDoc.class),
                    encoding = {
                        @Encoding(name = "notice", contentType = MediaType.APPLICATION_JSON_VALUE),
                        @Encoding(name = "files", contentType = "image/*,application/pdf")
                    },
                    examples = {
                        @ExampleObject(
                            name = "Multipart 요청 예시",
                            value =
                                """
                                    {
                                      "notice": {
                                        "title": "2025년 2월 전체 회의 안내 (수정)",
                                        "content": "회의 시간이 오후 3시로 변경되었습니다.",
                                        "pinned": true,
                                        "attachmentsIdsToRemove": [1]
                                      },
                                      "files": ["(binary)"]
                                    }
                                    """)
                    })
            }))
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 찾을 수 없음")
        })
    @PatchMapping(value = "/{noticeId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateNoticeJson(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
        @Parameter(description = "수정할 공지사항 ID", example = "1", required = true) @PathVariable Long noticeId,
        @Valid @RequestBody NoticeUpdateRequestDto dto)
        throws IOException {

        Long updatedId = noticeService.updateNotice(userDetails.getId(), noticeId, dto, null);
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }

    @Operation(hidden = true) // 중복 문서 노출 방지
    @PatchMapping(value = "/{noticeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> updateNoticeMultipart(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long noticeId,
        @Valid @RequestPart("notice") NoticeUpdateRequestDto dto,
        @RequestPart(value = "files", required = false) List<MultipartFile> files)
        throws IOException {

        Long updatedId = noticeService.updateNotice(userDetails.getId(), noticeId, dto, files);
        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }


    @Operation(
        summary = "홈 화면용 상위 공지 조회",
        description = "홈 화면에 노출할 상위 3개의 공지를 조회합니다. (상단 고정 우선, 최신순 정렬)")
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
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
                                  "result": [
                                    {
                                      "id": 10,
                                      "type": "상시공지",
                                      "title": "전사 신년회 안내",
                                      "isPinned": true,
                                      "updatedDate": "2026-01-25T10:00:00"
                                    },
                                    {
                                      "id": 9,
                                      "type": "식단표",
                                      "title": "1월 마지막 주 식단표",
                                      "isPinned": false,
                                      "updatedDate": "2026-01-26T14:00:00"
                                    },
                                    {
                                      "id": 8,
                                      "type": "상시공지",
                                      "title": "사내 도서관 신간 안내",
                                      "isPinned": false,
                                      "updatedDate": "2026-01-27T09:30:00"
                                    }
                                  ]
                                }
                                """)))
        })
    @GetMapping("/home")
    public ResponseEntity<ApiResponse<List<NoticeSummaryDto>>> getHomeNotices(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<NoticeSummaryDto> notices = noticeService.getTop3NoticesForHome(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.onSuccess(notices));
    }
}

@Schema(name = "NoticeUpdateMultipartRequest")
class NoticeUpdateMultipartRequestDoc {

    @Schema(description = "공지 수정 정보(JSON 파트)")
    public NoticeUpdateRequestDto notice;

    @ArraySchema(schema = @Schema(type = "string", format = "binary"))
    public List<String> files;
}

