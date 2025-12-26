package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class VisitorResponseDto {

    private Long id;
    private String name;
    private String phoneNumber;
    private String visitorCompany;
}
