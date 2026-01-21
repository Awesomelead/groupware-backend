package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "직원용 방문 상세 응답 DTO")
public class VisitDetailResponseDto {

    @Schema(description = "방문 ID", example = "1")
    private Long id;

    @Schema(description = "내방객 소속 회사명", example = "어썸테크")
    private String visitorCompany;

    @Schema(description = "내방객 이름", example = "홍길동")
    private String visitorName;

    @Schema(description = "방문 목적", example = "시설공사")
    private VisitPurpose purpose;

    @Schema(description = "보충적 허가 타입", example = "기타 허가")
    private AdditionalPermissionType permissionType;

    @Schema(description = "기타 허가 요구사항", example = "특수 장비 반입 필요")
    private String permissionDetail;

    @Schema(description = "담당 부서", example = "경영지원부")
    private DepartmentName hostDepartmentName;

    @Schema(description = "담당자 이름", example = "이순신")
    private String hostName;

    @Schema(description = "내방객 전화번호", example = "01012345678")
    private String visitorPhoneNumber;

    @Schema(description = "방문 시작일", example = "2026-02-01")
    private LocalDate startDate;

    @Schema(description = "방문 종료일", example = "2026-02-01")
    private LocalDate endDate;

    @Schema(description = "방문 상태", example = "방문 중")
    private VisitStatus status;

    @Schema(description = "입퇴실 및 서명 기록 리스트")
    private List<VisitRecordResponseDto> records;
}
