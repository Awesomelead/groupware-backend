package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "방문 접수 요청")
public class VisitCreateRequestDto {

    @NotBlank
    @Schema(description = "내방객 이름", example = "홍길동", required = true)
    private String visitorName;

    @NotBlank
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    @Schema(description = "내방객 전화번호 (하이픈 제외)", example = "01012345678", required = true)
    private String visitorPhone;

    @NotBlank
    @Schema(description = "내방객 회사명", example = "어썸리드", required = true)
    private String visitorCompany;

    @Schema(description = "차량 번호", example = "12가3456")
    private String carNumber;

    @NotNull
    @Schema(
        description = "방문 목적",
        example = "MEETING",
        required = true,
        allowableValues = {
            "CUSTOMER_INSPECTION",
            "GOODS_DELIVERY",
            "FACILITY_CONSTRUCTION",
            "LOGISTICS",
            "MEETING",
            "OTHER"
        })
    private VisitPurpose purpose;

    @NotNull
    @Schema(description = "방문 시작 일시", example = "2025-01-15T14:00:00", required = true)
    private LocalDateTime visitStartDate;

    @NotNull
    @Schema(description = "담당자 ID", example = "1", required = true)
    private Long hostUserId;

    @NotNull
    @Schema(description = "담당자 회사명 (클라이언트에서 자동 주입)", example = "어썸리드", required = true)
    private Company hostCompany;

    @Valid
    @Schema(description = "동행자 목록")
    private List<CompanionRequestDto> companions;

    @Size(min = 4, max = 4, message = "비밀번호는 4자리여야 합니다.")
    @Schema(
        description = "내방객 비밀번호 (4자리, 사전 예약 시 필수)",
        example = "1234",
        minLength = 4,
        maxLength = 4)
    private String visitorPassword;
}
