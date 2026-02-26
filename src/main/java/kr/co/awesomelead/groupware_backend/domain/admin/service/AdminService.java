package kr.co.awesomelead.groupware_backend.domain.admin.service;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.enums.AuthorityAction;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.MyInfoUpdateRequestRepository;
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
    private final MyInfoUpdateRequestRepository myInfoUpdateRequestRepository;

    @Transactional
    public void approveUserRegistration(
        Long userId, UserApprovalRequestDto requestDto, Long adminId) {
        //  관리자 권한 확인
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (admin.getRole() != Role.MASTER_ADMIN && admin.getRole() != Role.ADMIN) {
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
        user.setWorkLocation(requestDto.getWorkLocation());
        user.setDepartment(department);
        user.setJobType(requestDto.getJobType());

        if (requestDto.getJobType() == JobType.FIELD && requestDto.getRole() == Role.ADMIN) {
            throw new CustomException(ErrorCode.INVALID_JOB_TYPE_FOR_ADMIN_ROLE);
        }
        if (requestDto.getRole() != null) {
            user.setRole(requestDto.getRole());
        }
        user.setPosition(requestDto.getPosition());
        user.setHireDate(requestDto.getHireDate());
        // 사용자의 상태를 AVAILABLE로 변경
        user.setStatus(Status.AVAILABLE);

        // 관리직의 경우 기본 권한 부여
        if (requestDto.getJobType() == JobType.MANAGEMENT) {
            user.addAuthority(Authority.ACCESS_MESSAGE);
            user.addAuthority(Authority.ACCESS_EDUCATION);
        }
        // ADMIN 역할인 경우 모든 권한 부여
        if (requestDto.getRole() == Role.ADMIN) {
            for (Authority authority : Authority.values()) {
                user.addAuthority(authority);
            }
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
        targetUser.getAuthorities().clear();

        if (role == Role.MASTER_ADMIN || role == Role.ADMIN) {
            for (Authority authority : Authority.values()) {
                targetUser.addAuthority(authority);
            }
        }

        userRepository.save(targetUser);
    }

    @Transactional
    public void updateUserAuthority(
        Long userId, List<Authority> authorities, AuthorityAction action, Long adminId) {
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

        // 3. 요청 유효성 검사
        if (authorities == null || authorities.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        // 4. 동작(Action)에 따른 권한 처리
        switch (action) {
            case ADD -> {
                for (Authority authority : authorities) {
                    if (targetUser.hasAuthority(authority)) {
                        throw new CustomException(ErrorCode.AUTHORITY_ALREADY_ASSIGNED);
                    }
                }
                authorities.forEach(targetUser::addAuthority);
            }
            case REMOVE -> {
                for (Authority authority : authorities) {
                    if (!targetUser.hasAuthority(authority)) {
                        throw new CustomException(ErrorCode.AUTHORITY_NOT_ASSIGNED);
                    }
                }
                authorities.forEach(targetUser::removeAuthority);
            }
        }

        userRepository.save(targetUser);
    }

    @Transactional
    public void approveMyInfoUpdate(Long userId, Long adminId) {
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateMyInfoApprovalAuthority(admin);

        User targetUser =
            userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        MyInfoUpdateRequest request =
            myInfoUpdateRequestRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, MyInfoUpdateRequestStatus.PENDING)
                .orElseThrow(() -> new CustomException(ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

        if (request.getRequestedNameEng() != null) {
            targetUser.setNameEng(request.getRequestedNameEng());
        }
        if (request.getRequestedPhoneNumber() != null) {
            String phoneHash = request.getRequestedPhoneNumberHash();
            if (!phoneHash.equals(targetUser.getPhoneNumberHash())
                && userRepository.existsByPhoneNumberHash(phoneHash)) {
                throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
            }
            targetUser.updatePhoneNumber(request.getRequestedPhoneNumber());
        }
        if (request.getRequestedZipcode() != null) {
            targetUser.setZipcode(request.getRequestedZipcode());
        }
        if (request.getRequestedAddress1() != null) {
            targetUser.setAddress1(request.getRequestedAddress1());
        }
        if (request.getRequestedAddress2() != null) {
            targetUser.setAddress2(request.getRequestedAddress2());
        }

        request.approve(admin);
        userRepository.save(targetUser);
        myInfoUpdateRequestRepository.save(request);
    }

    @Transactional
    public void rejectMyInfoUpdate(Long userId, String reason, Long adminId) {
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateMyInfoApprovalAuthority(admin);

        if (reason == null || reason.isBlank()) {
            throw new CustomException(ErrorCode.MY_INFO_UPDATE_REJECT_REASON_REQUIRED);
        }

        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        MyInfoUpdateRequest request =
            myInfoUpdateRequestRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                    userId, MyInfoUpdateRequestStatus.PENDING)
                .orElseThrow(() -> new CustomException(ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

        request.reject(admin, reason.trim());
        myInfoUpdateRequestRepository.save(request);
    }

    private void validateMyInfoApprovalAuthority(User admin) {
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_MY_INFO_UPDATE_APPROVAL);
        }
    }
}
