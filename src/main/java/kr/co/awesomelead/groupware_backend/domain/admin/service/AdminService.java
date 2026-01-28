package kr.co.awesomelead.groupware_backend.domain.admin.service;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public void approveUserRegistration(
        Long userId, UserApprovalRequestDto requestDto, Long adminId) {
        //  관리자 권한 확인
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
        }

        // userId로 PENDING 상태의 사용자를 조회
        User user =
            userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.DUPLICATED_SIGNUP_REQUEST);
        }

        // 유저가 작성한 값들 중 관리자가 수정하는 값
        if (requestDto.getNameKor() != null) {
            user.setNameKor(requestDto.getNameKor());
        }
        if (requestDto.getNameEng() != null) {
            user.setNameEng(requestDto.getNameEng());
        }

        if (requestDto.getPhoneNumber() != null) {
            user.updatePhoneNumber(requestDto.getPhoneNumber());
        }

        if (requestDto.getNationality() != null) {
            user.setNationality(requestDto.getNationality());
        }

        Department department =
            departmentRepository
                .findById(requestDto.getDepartmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // DTO의 정보로 사용자 엔티티를 설정
        user.setDepartment(department);
        user.setJobType(requestDto.getJobType());
        user.setPosition(requestDto.getPosition());
        user.setHireDate(requestDto.getHireDate());
        user.setWorkLocation(requestDto.getWorkLocation());
        user.setRole(Role.USER); // 승인 시 기본 역할을 USER로 설정

        // 사용자의 상태를 AVAILABLE로 변경
        user.setStatus(Status.AVAILABLE);

        // 현장직의 경우 기본 권한 부여
        if (requestDto.getJobType() == JobType.MANAGEMENT) {
            user.addAuthority(Authority.ACCESS_MESSAGE);
            user.addAuthority(Authority.ACCESS_EDUCATION);
        }

        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(Long userId, Role role, Long adminId) {

        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_ROLE_UPDATE);
        }
        // 1. 대상 사용자 조회
        User targetUser =
            userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 역할 업데이트
        targetUser.setRole(role);

        if (role == Role.ADMIN) {
            targetUser.addAuthority(Authority.ACCESS_NOTICE);
            targetUser.addAuthority(Authority.MANAGE_EMPLOYEE_DATA);
        } else {
            targetUser.removeAuthority(Authority.ACCESS_NOTICE);
            targetUser.removeAuthority(Authority.MANAGE_EMPLOYEE_DATA);
        }

        userRepository.save(targetUser);
    }

    @Transactional
    public void updateUserAuthority(Long userId, Authority authority, String action, Long adminId) {
        // 1. 관리자 권한 확인 (ADMIN 또는 MASTER_ADMIN만 가능)
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_ROLE_UPDATE);
        }

        // 2. 대상 사용자 조회
        User targetUser =
            userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 동작(Action)에 따른 권한 처리
        if ("ADD".equalsIgnoreCase(action)) {
            targetUser.addAuthority(authority);
        } else if ("REMOVE".equalsIgnoreCase(action)) {
            targetUser.removeAuthority(authority);
        } else {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT); // 잘못된 액션 요청
        }

        userRepository.save(targetUser);
    }
}
