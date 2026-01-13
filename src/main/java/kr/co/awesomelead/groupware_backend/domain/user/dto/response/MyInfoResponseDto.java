package kr.co.awesomelead.groupware_backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    @Schema(description = "직종")
    private JobType jobType;

    @Schema(description = "직급", example = "대리")
    private String position;

    public static MyInfoResponseDto from(User user) {
        return MyInfoResponseDto.builder()
                .id(user.getId())
                .nameKor(user.getNameKor())
                .nameEng(user.getNameEng())
                .birthDate(user.getBirthDate())
                .nationality(user.getNationality())
                .registrationNumberFront(formatRegistrationNumber(user.getRegistrationNumber()))
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .workLocation(user.getWorkLocation())
                .departmentName(
                        user.getDepartment() != null ? user.getDepartment().getName() : null)
                .jobType(user.getJobType())
                .position(user.getPosition())
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
