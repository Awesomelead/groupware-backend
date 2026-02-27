package kr.co.awesomelead.groupware_backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 직원 정보 수정 요청 (이메일 제외)")
public class AdminUserUpdateRequestDto {

    @Schema(description = "한글 이름", example = "홍길동")
    private String nameKor;

    @Schema(description = "영문 이름", example = "HONG GILDONG")
    private String nameEng;

    @Schema(description = "생년월일", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "국적", example = "대한민국")
    private String nationality;

    @Schema(description = "우편번호", example = "06234")
    private String zipcode;

    @Schema(description = "주소1", example = "서울특별시 강남구 테헤란로 123")
    private String address1;

    @Schema(description = "주소2", example = "어썸리드빌딩 5층")
    private String address2;

    @Schema(description = "주민등록번호/외국인등록번호", example = "9001011234567")
    private String registrationNumber;

    @Schema(description = "전화번호 (변경 시 인증 필요)", example = "01099998888")
    private String phoneNumber;

    @Schema(description = "근무사업장", example = "어썸리드")
    private Company workLocation;

    @Schema(description = "부서 ID", example = "11")
    private Long departmentId;

    @Schema(description = "직급", example = "사원")
    private Position position;

    @Schema(description = "직무 유형", example = "관리직")
    private JobType jobType;

    @Schema(description = "역할", example = "일반 사용자")
    private Role role;

    @ArraySchema(
            schema = @Schema(implementation = Authority.class),
            arraySchema = @Schema(description = "권한부여 목록 (배열 형태)"))
    private List<Authority> authorities;

    @Schema(description = "입사일", example = "2025-09-22")
    private LocalDate hireDate;

    @Schema(description = "퇴사일", example = "2026-12-31")
    private LocalDate resignationDate;
}
