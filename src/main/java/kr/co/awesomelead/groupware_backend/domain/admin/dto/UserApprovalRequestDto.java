package kr.co.awesomelead.groupware_backend.domain.admin.dto;

import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserApprovalRequestDto {
    private LocalDate hireDate;
    private String jobType;
    private String position;
    private String workLocation;
    private Role role;
}
