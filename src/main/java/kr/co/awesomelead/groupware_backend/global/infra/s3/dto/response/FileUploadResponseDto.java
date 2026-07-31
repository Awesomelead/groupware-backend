package kr.co.awesomelead.groupware_backend.global.infra.s3.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "파일 단일 업로드 응답")
public class FileUploadResponseDto {

    @Schema(description = "S3 파일 키", example = "editor/2026/07/uuid-dummy1.png")
    private String fileKey;

    @Schema(description = "원본 파일명", example = "dummy1.png")
    private String fileName;

    @Schema(
            description = "에디터에 삽입할 이미지 URL",
            example =
                    "https://bucket.s3.ap-northeast-2.amazonaws.com/editor/2026/07/uuid-dummy1.png?X-Amz-Signature=...")
    private String imageUrl;
}
