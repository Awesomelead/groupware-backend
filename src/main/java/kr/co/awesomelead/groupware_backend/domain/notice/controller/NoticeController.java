package kr.co.awesomelead.groupware_backend.domain.notice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeDetailDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import kr.co.awesomelead.groupware_backend.domain.notice.service.NoticeService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(
    name = "Notice",
    description = """
        ## 공지사항 관리 API
        
        상시공지, 식단표, 기타 공지사항의 생성, 조회, 수정, 삭제 기능을 제공합니다.
        
        ### 사용되는 Enum 타입
        - **NoticeType**: 공지사항 유형 (REGULAR: 상시공지, MENU: 식단표, ETC: 기타)
        
        ### 권한
        - 공지사항 작성/수정/삭제: WRITE_NOTICE 권한 필요
        - 공지사항 조회: 모든 사용자 가능
        """
)
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 생성", description = "새로운 공지를 생성합니다. 첨부파일(선택)을 포함할 수 있습니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "공지 생성 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": true,
                          "code": "COMMON201",
                          "message": "성공적으로 생성되었습니다.",
                          "result": 1
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "COMMON400",
                          "message": "입력값이 유효하지 않습니다.",
                          "result": {
                            "title": "공지사항 제목은 필수입니다.",
                            "type": "공지 유형은 필수입니다."
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "공지 작성 권한 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "NO_AUTHORITY_FOR_NOTICE",
                          "message": "공지사항 작성 권한이 없습니다.",
                          "result": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "사용자를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "USER_NOT_FOUND",
                          "message": "해당 사용자를 찾을 수 없습니다.",
                          "result": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "서버 에러 (S3 업로드 실패 등)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "COMMON500",
                          "message": "서버 내부 오류가 발생했습니다.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createNotice(
        @RequestPart("requestDto") @Valid NoticeCreateRequestDto requestDto,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        @AuthenticationPrincipal CustomUserDetails userDetails)
        throws IOException {

        Long noticeId = noticeService.createNotice(requestDto, files, userDetails.getId());

        URI location =
            ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(noticeId)
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.onCreated(noticeId));
    }

    @Operation(summary = "공지 목록 조회", description = "특정 유형의 공지 목록을 조회합니다. type을 지정하지 않으면 전체 공지를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": true,
                          "code": "COMMON200",
                          "message": "요청에 성공했습니다.",
                          "result": [
                            {
                              "id": 1,
                              "title": "2025년 1월 전체 회의 안내",
                              "updatedDate": "2025-01-10T14:30:00"
                            },
                            {
                              "id": 2,
                              "title": "1월 둘째 주 식단표",
                              "updatedDate": "2025-01-09T09:00:00"
                            }
                          ]
                        }
                        """
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeSummaryDto>>> getNotices(
        @RequestParam(required = false) NoticeType type) {
        List<NoticeSummaryDto> notices = noticeService.getNoticesByType(type);
        return ResponseEntity.ok(ApiResponse.onSuccess(notices));
    }

    @Operation(summary = "공지 상세 조회", description = "특정 공지의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
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
                                "viewUrl": "https://bucket.s3.amazonaws.com/notices/uuid_file.pdf"
                              }
                            ]
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "공지사항을 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "NOTICE_NOT_FOUND",
                          "message": "해당 공지사항을 찾을 수 없습니다.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailDto>> getNotice(@PathVariable Long noticeId) {
        NoticeDetailDto dto = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.onSuccess(dto));
    }

    @Operation(summary = "공지 삭제", description = "특정 공지를 삭제합니다. 첨부파일도 함께 S3에서 삭제됩니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "삭제 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": true,
                          "code": "COMMON204",
                          "message": "성공적으로 처리되었습니다.",
                          "result": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "공지 삭제 권한 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "NO_AUTHORITY_FOR_NOTICE",
                          "message": "공지사항 작성 권한이 없습니다.",
                          "result": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "공지사항 또는 사용자를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "공지사항 없음",
                        value = """
                            {
                              "isSuccess": false,
                              "code": "NOTICE_NOT_FOUND",
                              "message": "해당 공지사항을 찾을 수 없습니다.",
                              "result": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "사용자 없음",
                        value = """
                            {
                              "isSuccess": false,
                              "code": "USER_NOT_FOUND",
                              "message": "해당 사용자를 찾을 수 없습니다.",
                              "result": null
                            }
                            """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "서버 에러 (S3 삭제 실패 등)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "COMMON500",
                          "message": "서버 내부 오류가 발생했습니다.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
        @PathVariable Long noticeId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        noticeService.deleteNotice(userDetails.getId(), noticeId);
        return ResponseEntity.ok().body(ApiResponse.onNoContent());
    }

    @Operation(summary = "공지 수정", description = "특정 공지를 수정합니다. 새로운 첨부파일 추가 및 기존 첨부파일 삭제가 가능합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "수정 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": true,
                          "code": "COMMON200",
                          "message": "요청에 성공했습니다.",
                          "result": 1
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "공지 수정 권한 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "NO_AUTHORITY_FOR_NOTICE",
                          "message": "공지사항 작성 권한이 없습니다.",
                          "result": null
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "공지사항, 첨부파일 또는 사용자를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "공지사항 없음",
                        value = """
                            {
                              "isSuccess": false,
                              "code": "NOTICE_NOT_FOUND",
                              "message": "해당 공지사항을 찾을 수 없습니다.",
                              "result": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "첨부파일 없음",
                        value = """
                            {
                              "isSuccess": false,
                              "code": "NOTICE_ATTACHMENT_NOT_FOUND",
                              "message": "해당 공지사항 첨부파일을 찾을 수 없습니다.",
                              "result": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "사용자 없음",
                        value = """
                            {
                              "isSuccess": false,
                              "code": "USER_NOT_FOUND",
                              "message": "해당 사용자를 찾을 수 없습니다.",
                              "result": null
                            }
                            """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "서버 에러 (S3 업로드/삭제 실패 등)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                        {
                          "isSuccess": false,
                          "code": "COMMON500",
                          "message": "서버 내부 오류가 발생했습니다.",
                          "result": null
                        }
                        """
                )
            )
        )
    })
    @PatchMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Long>> updateNotice(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long noticeId,
        @RequestPart(value = "notice") @Valid NoticeUpdateRequestDto dto,
        @RequestPart(value = "files", required = false) List<MultipartFile> files)
        throws IOException {

        Long updatedId = noticeService.updateNotice(userDetails.getId(), noticeId, dto, files);

        return ResponseEntity.ok(ApiResponse.onSuccess(updatedId));
    }
}