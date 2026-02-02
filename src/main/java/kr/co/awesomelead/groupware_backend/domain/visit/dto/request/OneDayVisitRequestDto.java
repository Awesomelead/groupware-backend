package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "사전 하루 방문 신청 요청 DTO")
public class OneDayVisitRequestDto implements VisitRequest {

    @NotBlank(message = "내방객 이름은 필수입니다.")
    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
    @Schema(description = "내방객 전화번호 (하이픈 제외)", example = "01012345678")
    private String visitorPhoneNumber;

    @NotBlank(message = "내방객 소속 회사명은 필수입니다.")
    @Schema(description = "내방객 소속 회사명", example = "어썸테크")
    private String visitorCompany;

    @Schema(description = "차량 번호", example = "12가 3456")
    private String carNumber;

    @NotNull(message = "방문 목적은 필수입니다.")
    @Schema(description = "방문 목적", example = "고객 검수")
    private VisitPurpose purpose;

    @NotNull(message = "방문일은 필수입니다.")
    @Schema(description = "방문 날짜", example = "2026-02-01")
    private LocalDate visitDate;

    @NotNull(message = "입실 예정 시간은 필수입니다.")
    @Schema(description = "입실 예정 시간", example = "10:00")
    private LocalTime entryTime;

    @NotNull(message = "퇴실 예정 시간은 필수입니다.")
    @Schema(description = "퇴실 예정 시간", example = "18:00")
    private LocalTime exitTime;

    // 보충적 허가 관련
    @Schema(description = "보충적 허가 타입 (없을 시 NONE)", example = "해당 없음")
    @Builder.Default
    private AdditionalPermissionType permissionType = AdditionalPermissionType.NONE;

    @Schema(description = "기타 허가 요구사항 (필요 시 작성)", example = "화기 사용 허가 필요")
    private String permissionDetail;

    @NotNull(message = "담당자 선택은 필수입니다.")
    @Schema(description = "담당 직원 ID", example = "1")
    private Long hostId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 4, message = "비밀번호는 숫자 4자리여야 합니다.")
    @Schema(description = "조회용 비밀번호 (4자리)", example = "1234")
    private String password;

    @AssertTrue(message = "퇴실 예정 시간은 입실 예정 시간보다 빨라야 합니다.")
    @Schema(hidden = true)
    public boolean isValidTimeRange() {
        if (entryTime == null || exitTime == null) {
            return true;
        }
        return exitTime.isAfter(entryTime);
    }
}
