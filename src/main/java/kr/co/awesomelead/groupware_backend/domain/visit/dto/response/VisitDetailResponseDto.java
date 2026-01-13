package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@Schema(description = "방문 상세 조회 응답 (직원용)")
public class VisitDetailResponseDto {

    @Schema(description = "방문 ID", example = "1")
    private Long visitId;

    @Schema(description = "내방객 회사명", example = "어썸리드")
    private String visitorCompany;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "방문 목적", example = "MEETING")
    private VisitPurpose purpose;

    @Schema(description = "보충적 허가 타입", example = "HIGH_ALTITUDE_WORK")
    private AdditionalPermissionType permissionType;

    @Schema(description = "기타 허가 상세 내용", example = "특수 장비 반입 허가 필요")
    private String permissionDetail;

    @Schema(description = "담당 부서", example = "개발팀")
    private String hostDepartment;

    @Schema(description = "담당자 이름", example = "이담당")
    private String hostName;

    @Schema(description = "내방객 전화번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "방문 시작 일시", example = "2025-01-15T14:00:00")
    private LocalDateTime visitStartDate;

    @Schema(description = "방문 종료 일시", example = "2025-01-15T18:00:00")
    private LocalDateTime visitEndDate;

    @Schema(
        description = "서명 이미지 URL",
        example = "https://bucket.s3.amazonaws.com/signatures/uuid_signature.png")
    private String signatureUrl;

    @Schema(description = "방문 여부", example = "false")
    private boolean visited;
}
