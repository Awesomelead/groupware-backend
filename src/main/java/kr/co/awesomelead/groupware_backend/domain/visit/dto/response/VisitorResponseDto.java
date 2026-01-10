package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "내방객 정보 응답")
public class VisitorResponseDto {

    @Schema(description = "내방객 ID", example = "1")
    private Long id;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String name;

    @Schema(description = "내방객 전화번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "내방객 회사명", example = "어썸리드")
    private String visitorCompany;
}
