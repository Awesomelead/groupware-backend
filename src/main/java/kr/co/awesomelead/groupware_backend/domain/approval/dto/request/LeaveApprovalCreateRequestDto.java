package kr.co.awesomelead.groupware_backend.domain.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveDetailType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "근태신청서(휴가) 생성 요청 DTO")
public class LeaveApprovalCreateRequestDto extends ApprovalCreateRequestDto {

    @Schema(description = "휴가 시작 일시", example = "2026-03-02T09:00:00")
    @NotNull(message = "휴가 시작 일시는 필수입니다.")
    private LocalDateTime startDate;

    @Schema(description = "휴가 종료 일시", example = "2026-03-03T18:00:00")
    @NotNull(message = "휴가 종료 일시는 필수입니다.")
    private LocalDateTime endDate;

    @Schema(description = "휴가 종류", example = "LEAVE")
    @NotNull(message = "휴가 종류를 선택해주세요.")
    private LeaveType leaveType;

    @Schema(description = "휴가 상세 구분", example = "ANNUAL")
    @NotNull(message = "휴가 상세 구분을 선택해주세요.")
    private LeaveDetailType leaveDetailType;

    @Schema(description = "신청 사유", example = "개인 사정으로 인한 연차 사용")
    @NotBlank(message = "신청 사유를 입력해주세요.")
    private String reason;

    @Schema(description = "비상 연락처", example = "010-1234-5678")
    @NotBlank(message = "비상 연락처를 입력해주세요.")
    private String emergencyContact;
}
