package kr.co.awesomelead.groupware_backend.global.infra.s3.controller;

import io.swagger.v3.oas.annotations.Operation;

import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.dto.response.FileUploadResponseDto;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service.S3File;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @Operation(summary = "파일 단일 업로드", description = "파일을 S3에 업로드하고 파일 키, 원본 파일명, 접근 URL을 반환합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResponseDto>> uploadFile(
            @RequestPart("file") MultipartFile file) throws IOException {

        FileUploadResponseDto uploaded = s3Service.uploadEditorFile(file);
        FileUploadResponseDto response =
                FileUploadResponseDto.builder()
                        .fileKey(uploaded.getFileKey())
                        .fileName(uploaded.getFileName())
                        .imageUrl(s3Service.getProxyViewUrl(uploaded.getFileKey()))
                        .build();

        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @Operation(summary = "파일 보기", description = "S3 파일 키(fileKey)를 입력하여 파일 바이트를 바로 조회합니다.")
    @GetMapping("/view")
    public ResponseEntity<byte[]> viewFile(@RequestParam("fileKey") String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        S3File file = s3Service.downloadFileWithMetadata(fileKey.trim());
        String contentType =
                StringUtils.hasText(file.contentType())
                        ? file.contentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Cache-Control", "private, max-age=300")
                .body(file.bytes());
    }

    @Operation(summary = "파일 단일 삭제", description = "파일 키(fileKey)를 입력하여 S3 파일을 삭제합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam("fileKey") String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        s3Service.deleteFile(fileKey.trim());
        return ResponseEntity.ok(ApiResponse.onNoContent("파일이 성공적으로 삭제되었습니다."));
    }
}
