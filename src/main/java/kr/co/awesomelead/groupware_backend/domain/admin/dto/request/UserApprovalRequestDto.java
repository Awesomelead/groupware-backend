package kr.co.awesomelead.groupware_backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserApprovalRequestDto {

    @Schema(description = "한글 이름 (수정 필요 시)", example = "홍길동")
    private String nameKor;

    @Schema(description = "영문 이름 (수정 필요 시)", example = "HONG GILDONG")
    private String nameEng;

    @Schema(description = "국적 (수정 필요 시)", example = "대한민국")
    private String nationality;

    @Schema(description = "전화번호 (수정 필요 시)", example = "01099998888")
    private String phoneNumber;

    @Schema(description = "부서 ID", example = "1")
    private Long departmentId;

    @Schema(description = "직무 유형", example = "관리직")
    private JobType jobType;

    @Schema(description = "직급", example = "대리")
    private String position;

    @Schema(description = "입사일", example = "2025-09-22")
    private LocalDate hireDate;

    @Schema(description = "근무지", example = "AWESOME")
    private Company workLocation;
}
