package kr.co.awesomelead.groupware_backend.test.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "더미 유저 생성 요청")
public class DummyUsersCreateRequestDto {

    @Schema(description = "생성할 유저 수", example = "20", defaultValue = "20")
    @Min(1)
    @Max(500)
    private int count = 20;

    @Schema(description = "이메일/전화번호/사번 시퀀스 시작값", example = "1", defaultValue = "1")
    @Min(1)
    private int startIndex = 1;

    @Schema(
            description = "대상 부서 ID 목록 (비우면 전체 부서 순환 분배)",
            example = "[2,3,4,5,6]")
    private List<Long> departmentIds;

    @Schema(description = "더미 계정 공통 비밀번호", example = "test1234!", defaultValue = "test1234!")
    @NotBlank
    private String password = "test1234!";

    @Schema(description = "역할", defaultValue = "USER")
    private Role role = Role.USER;

    @Schema(description = "상태", defaultValue = "AVAILABLE")
    private Status status = Status.AVAILABLE;

    @Schema(description = "직종", defaultValue = "MANAGEMENT")
    private JobType jobType = JobType.MANAGEMENT;

    @Schema(description = "직급", defaultValue = "STAFF")
    private Position position = Position.STAFF;
}
