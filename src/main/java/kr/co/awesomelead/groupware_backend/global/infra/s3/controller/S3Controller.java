package kr.co.awesomelead.groupware_backend.global.infra.s3.controller;

import io.swagger.v3.oas.annotations.Operation;

import kr.co.awesomelead.groupware_backend.global.common.response.ApiResponse;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @Operation(summary = "파일 단일 업로드", description = "파일을 S3에 업로드하고 접근 URL을 반환합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestPart("file") MultipartFile file)
            throws IOException {

        // S3에 저장 후 URL 반환
        String fileUrl = s3Service.uploadFile(file);

        return ResponseEntity.ok(ApiResponse.onSuccess(fileUrl));
    }

    @Operation(summary = "파일 단일 삭제", description = "파일 키(fileKey)를 입력하여 S3 파일을 삭제합니다.")
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestParam("fileKey") String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        s3Service.deleteFile(fileKey.trim());
        return ResponseEntity.ok(ApiResponse.onNoContent("파일이 성공적으로 삭제되었습니다."));
    }
}
