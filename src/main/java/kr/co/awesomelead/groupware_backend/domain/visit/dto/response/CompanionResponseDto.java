package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "동행자 정보 응답")
public class CompanionResponseDto {

    @Schema(description = "동행자 ID", example = "1")
    private Long id;

    @Schema(description = "동행자 이름", example = "김철수")
    private String name;

    @Schema(description = "동행자 전화번호", example = "01087654321")
    private String phoneNumber;

    @Schema(description = "동행자 회사명", example = "테스트회사")
    private String visitorCompany;
}
