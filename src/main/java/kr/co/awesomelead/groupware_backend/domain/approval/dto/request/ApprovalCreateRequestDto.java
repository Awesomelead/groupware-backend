package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ParticipantType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.RetentionPeriod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "documentType", // 클라이언트가 보내는 JSON의 이 필드값으로 하위 클래스 판단
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = LeaveApprovalCreateRequestDto.class, name = "LEAVE"),
    @JsonSubTypes.Type(value = CarFuelApprovalCreateRequestDto.class, name = "CAR_FUEL"),
    @JsonSubTypes.Type(value = ExpenseDraftApprovalCreateRequestDto.class, name = "EXPENSE_DRAFT"),
    @JsonSubTypes.Type(value = OverseasTripApprovalCreateRequestDto.class, name = "OVERSEAS_TRIP"),
    @JsonSubTypes.Type(value = BasicApprovalCreateRequestDto.class, name = "BASIC")
})
@Schema(description = "전자결재 생성 요청 공통 DTO")
public abstract class ApprovalCreateRequestDto {

    @Schema(description = "결재 제목", example = "2026년 3월 하계 휴가 신청의 건")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200)
    private String title;          // 제목

    @Schema(description = "결재 본문 (HTML 형식)", example = "<p>2026년 3월 2일부터 3일까지 연차 휴가 신청드립니다.</p>")
    @NotBlank(message = "본문 내용은 필수입니다.")
    private String content;        // 에디터 본문 (HTML)

    @Schema(description = "문서 종류", example = "LEAVE")
    @NotNull(message = "문서 종류를 선택해주세요.")
    private DocumentType documentType; // 문서 종류 (Enum)

    @Schema(description = "보존년한", example = "FIVE_YEARS")
    @NotNull(message = "보존년한을 선택해주세요.")
    private RetentionPeriod retentionPeriod; // 보존년한

    @Schema(description = "결재선 리스트 (순서 중요)")
    @NotEmpty(message = "결재선은 최소 1명 이상 지정해야 합니다.")
    @Valid
    private List<StepRequestDto> approvalSteps;

    @Schema(description = "참조 및 열람자 리스트")
    @Valid
    private List<ParticipantRequestDto> participants;

    @Schema(description = "첨부파일 ID 리스트 (S3 업로드 후 반환된 ID)", example = "[12, 13]")
    private List<Long> attachmentIds;

    @Getter
    @Setter
    @Schema(description = "결재 단계 정보 DTO")
    public static class StepRequestDto {

        @Schema(description = "결재자 User ID", example = "5")
        @NotNull(message = "결재자 ID는 필수입니다.")
        private Long approverId; // 결재자 User ID

        @Schema(description = "결재 순서 (1부터 시작)", example = "1")
        @NotNull(message = "결재 순서는 필수입니다.")
        private Integer sequence; // 결재 순서 (1, 2, 3...)
    }

    @Getter
    @Setter
    @Schema(description = "참조/열람자 정보 DTO")
    public static class ParticipantRequestDto {

        @Schema(description = "대상자 User ID", example = "10")
        @NotNull(message = "대상자 ID는 필수입니다.")
        private Long userId; // 대상자 User ID

        @Schema(description = "참여 유형 (REFERRER: 참조자, VIEWER: 열람권자)", example = "REFERRER")
        @NotNull(message = "참여 유형을 선택해주세요.")
        private ParticipantType participantType; // REFERRER, VIEWER
    }

}
