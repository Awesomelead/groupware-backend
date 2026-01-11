package kr.co.awesomelead.groupware_backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserApprovalRequestDto {

    @Schema(description = "사용자 이름", example = "홍길동")
    private LocalDate hireDate;

    @Schema(description = "직무 유형", example = "관리직")
    private String jobType;

    @Schema(description = "직급", example = "대리")
    private String position;

    @Schema(description = "근무지", example = "AWESOME")
    private Company workLocation;

    @Schema(description = "사용자 역할", example = "USER")
    private Role role;
}
