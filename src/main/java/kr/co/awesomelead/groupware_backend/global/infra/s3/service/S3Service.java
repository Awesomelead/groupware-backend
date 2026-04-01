package kr.co.awesomelead.groupware_backend.global.infra.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public String getPresignedViewUrl(String fileKey) {
        return generatePresignedUrl(fileKey, Duration.ofMinutes(30)); // 30분짜리 권한
    }

    public String getPresignedDownloadUrl(String fileKey, String downloadFileName) {
        return generatePresignedUrl(
                fileKey,
                Duration.ofMinutes(30),
                buildAttachmentContentDisposition(downloadFileName));
    }

    public String generatePresignedUrl(String fileKey, Duration duration) {
        return generatePresignedUrl(fileKey, duration, null);
    }

    public String generatePresignedUrl(
            String fileKey, Duration duration, String responseContentDisposition) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }

        try {
            GetObjectRequest.Builder requestBuilder =
                    GetObjectRequest.builder().bucket(bucketName).key(fileKey);
            if (responseContentDisposition != null && !responseContentDisposition.isBlank()) {
                requestBuilder.responseContentDisposition(responseContentDisposition);
            }
            GetObjectRequest getObjectRequest =
                    requestBuilder.build();

            GetObjectPresignRequest presignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(duration)
                            .getObjectRequest(getObjectRequest)
                            .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (Exception e) {
            log.error("Presigned URL 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    private String buildAttachmentContentDisposition(String fileName) {
        String safeFileName = fileName == null || fileName.isBlank() ? "download.xlsx" : fileName;
        String asciiFallback = safeFileName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
        String encoded =
                URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        String fileName = generateFileName(file.getOriginalFilename());

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(file.getContentType())
                        .contentDisposition("inline")
                        .build();

        RequestBody requestBody =
                RequestBody.fromInputStream(file.getInputStream(), file.getSize());
        s3Client.putObject(putObjectRequest, requestBody);

        return fileName;
    }

    public String uploadBytes(byte[] fileData, String originalFileName, String contentType) {
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("파일 데이터가 비어있습니다.");
        }

        String fileName = generateFileName(originalFileName);

        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .contentDisposition("inline")
                        .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));
        return fileName;
    }

    public void deleteFile(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return;
        }

        try {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(fileKey));
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
            throw new RuntimeException("S3 파일 삭제 중 오류가 발생했습니다.");
        }
    }

    private String generateFileName(String originalFileName) {
        return UUID.randomUUID().toString()
                + "_"
                + (originalFileName != null ? originalFileName : "file");
    }

    public String getFileUrl(String fileName) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);
    }

    public byte[] downloadFile(String fileKey) {
        try {
            return s3Client.getObjectAsBytes(builder -> builder.bucket(bucketName).key(fileKey))
                    .asByteArray();
        } catch (Exception e) {
            log.error("S3 파일 다운로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 다운로드 중 오류가 발생했습니다.");
        }
    }
}
