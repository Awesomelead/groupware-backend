package kr.co.awesomelead.groupware_backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 정보 조회 응답")
public class MyInfoResponseDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "한글 이름", example = "김철수")
    private String nameKor;

    @Schema(description = "영문 이름", example = "Kim Chulsoo")
    private String nameEng;

    @Schema(description = "생년월일", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "국적", example = "대한민국")
    private String nationality;

    @Schema(description = "우편번호", example = "03127")
    private String zipcode;

    @Schema(description = "주소1", example = "충남 아산시 둔포면 아산밸리로388번길 10")
    private String address1;

    @Schema(description = "주소2", example = "101호")
    private String address2;

    @Schema(description = "주민등록번호", example = "900101-1******")
    private String registrationNumberFront;

    @Schema(description = "전화번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "이메일", example = "timber@example.com")
    private String email;

    @Schema(description = "근무지")
    private Company workLocation;

    @Schema(description = "부서")
    private DepartmentName departmentName;

    @Schema(description = "직급", example = "대리")
    private Position position;

    @Schema(description = "직종")
    private JobType jobType;

    @Schema(description = "권한부여")
    private List<MyInfoAuthorityItemDto> authorities;

    @Schema(description = "입사일")
    private LocalDate hireDate;

    @Schema(description = "퇴사일")
    private LocalDate resignationDate;

    public static MyInfoResponseDto from(User user) {
        return MyInfoResponseDto.builder()
                .id(user.getId())
                .nameKor(user.getNameKor())
                .nameEng(user.getNameEng())
                .birthDate(user.getBirthDate())
                .nationality(user.getNationality())
                .zipcode(user.getZipcode())
                .address1(user.getAddress1())
                .address2(user.getAddress2())
                .registrationNumberFront(formatRegistrationNumber(user.getRegistrationNumber()))
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .workLocation(user.getWorkLocation())
                .departmentName(
                        user.getDepartment() != null ? user.getDepartment().getName() : null)
                .position(user.getPosition())
                .jobType(user.getJobType())
                .authorities(
                        java.util.Arrays.stream(
                                        kr.co.awesomelead.groupware_backend.domain.user.enums
                                                .Authority.values())
                                .map(
                                        a ->
                                                MyInfoAuthorityItemDto.builder()
                                                        .code(a.name())
                                                        .label(a.getDescription())
                                                        .enabled(
                                                                user.getAuthorities() != null
                                                                        && user.getAuthorities()
                                                                                .contains(a))
                                                        .build())
                                .toList())
                .hireDate(user.getHireDate())
                .resignationDate(user.getResignationDate())
                .build();
    }

    // 주민등록번호를 "900101-1******" 형식으로 포맷팅
    private static String formatRegistrationNumber(String registrationNumber) {
        if (registrationNumber == null || registrationNumber.length() < 7) {
            return null;
        }
        // 앞 6자리 + "-" + 뒷자리 첫번째 숫자 + "******"
        return registrationNumber.substring(0, 6) + "-" + registrationNumber.charAt(6) + "******";
    }
}
