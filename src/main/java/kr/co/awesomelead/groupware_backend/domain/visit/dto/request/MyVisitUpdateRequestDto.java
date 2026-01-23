package kr.co.awesomelead.groupware_backend.domain.visit.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalTime;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyVisitUpdateRequestDto implements VisitRequest {

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "방문 비밀번호", example = "1234")
    private String password;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "내방객 전화번호 (하이픈 제외)", example = "01012345678")
    private String visitorCompany;

    @Schema(description = "방문 목적", example = "고객 검수")
    private VisitPurpose purpose;

    @Schema(description = "보충적 허가 타입", example = "해당 없음")
    private AdditionalPermissionType permissionType;

    @Schema(description = "기타 허가 요구사항", example = "화기 사용 허가 필요")
    private String permissionDetail;

    // 날짜 및 시간 수정
    @Schema(description = "방문 시작일", example = "2024-07-01")
    private LocalDate startDate;

    @Schema(description = "방문 종료일", example = "2024-07-01")
    private LocalDate endDate;

    @Schema(description = "입실 예정 시간", example = "10:00")
    private LocalTime plannedEntryTime;

    @Schema(description = "퇴실 예정 시간", example = "18:00")
    private LocalTime plannedExitTime;

    @Override
    public String getVisitorPhoneNumber() {
        return null;
    }

    @Override
    public String getCarNumber() {
        return null;
    }

    @Override
    public Long getHostId() {
        return null;
    }
}
