package kr.co.awesomelead.groupware_backend.domain.notice.controller;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;

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

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 생성", description = "새로운 공지를 생성합니다.")
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

    @Operation(summary = "공지 목록 조회", description = "특정 유형의 공지 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeSummaryDto>>> getNotices(
            @RequestParam NoticeType type) {
        List<NoticeSummaryDto> notices = noticeService.getNoticesByType(type);
        return ResponseEntity.ok(ApiResponse.onSuccess(notices));
    }

    @Operation(summary = "공지 상세 조회", description = "특정 공지의 상세 정보를 조회합니다.")
    @GetMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<NoticeDetailDto>> getNotice(@PathVariable Long noticeId) {
        NoticeDetailDto dto = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.onSuccess(dto));
    }

    @Operation(summary = "공지 삭제", description = "특정 공지를 삭제합니다.")
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @PathVariable Long noticeId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        noticeService.deleteNotice(userDetails.getId(), noticeId);
        return ResponseEntity.ok(ApiResponse.onNoContent());
    }

    @Operation(summary = "공지 수정", description = "특정 공지를 수정합니다.")
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
