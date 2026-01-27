package kr.co.awesomelead.groupware_backend.domain.notice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "공지사항 생성 요청")
public class NoticeCreateRequestDto {

    @NotBlank(message = "공지사항 제목은 필수입니다.")
    @Schema(description = "공지사항 제목", example = "2025년 1월 전체 회의 안내", required = true)
    private String title;

    @Schema(description = "공지사항 내용 (식단표의 경우 null 가능)", example = "오는 1월 15일 오후 2시에 전체 회의가 있습니다.")
    private String content;

    @NotNull(message = "공지 유형은 필수입니다.")
    @Schema(
        description = "공지사항 유형",
        example = "REGULAR",
        required = true,
        allowableValues = {"상시공지", "식단표", "기타"})
    private NoticeType type;

    @Builder.Default
    @Schema(description = "상단 고정 여부", example = "false", defaultValue = "false")
    private Boolean pinned = false;

    @NotNull(message = "공지 대상 회사는 최소 하나 이상 선택해야 합니다.")
    @Schema(
        description = "공지 대상 회사 목록 (중복 선택 가능)",
        example = "[\"AWESOME\", \"MARUI\"]",
        required = true)
    private List<Company> targetCompanies;
}
