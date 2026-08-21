package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryResponseDto {

    @Schema(description = "사용자 고유 ID", example = "1", required = true)
    private Long id;

    @Schema(description = "사용자 이름", example = "홍길동", required = true)
    private String name;

    @Schema(description = "근무 직종", example = "관리직")
    private JobType jobType;
}
