package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserSummaryResponseDto {

    private Long id;
    private String name;

}
