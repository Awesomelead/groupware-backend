package kr.co.awesomelead.groupware_backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "직원 관리 목록 응답")
public class AdminUserSummaryResponseDto {

    @Schema(description = "사용자 ID", example = "17")
    private Long userId;

    @Schema(description = "한글 이름", example = "홍길동")
    private String nameKor;

    @Schema(description = "직급")
    private Position position;

    @Schema(description = "근무직종")
    private JobType jobType;

    @Schema(description = "부서명")
    private DepartmentName departmentName;

    @Schema(description = "회원가입 상태", example = "AVAILABLE")
    private Status signupStatus;

    @Schema(description = "개인정보 수정 요청 대기 여부", example = "false")
    private boolean hasPendingMyInfoRequest;

    public static AdminUserSummaryResponseDto from(User user, boolean hasPendingMyInfoRequest) {
        return AdminUserSummaryResponseDto.builder()
                .userId(user.getId())
                .nameKor(user.getNameKor())
                .position(user.getPosition())
                .jobType(user.getJobType())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .signupStatus(user.getStatus())
                .hasPendingMyInfoRequest(hasPendingMyInfoRequest)
                .build();
    }
}
