package kr.co.awesomelead.groupware_backend.domain.admin.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.MyInfoUpdateRequestSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.PendingUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.enums.AuthorityAction;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final MyInfoUpdateRequestRepository myInfoUpdateRequestRepository;
    private final PhoneAuthService phoneAuthService;

    @Transactional
    public void approveUserRegistration(
        Long userId, UserApprovalRequestDto requestDto, Long adminId) {
        //  관리자 권한 확인
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

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

        if (requestDto.getNationality() != null) {
            user.setNationality(requestDto.getNationality());
        }

        if (hasText(requestDto.getZipcode())) {
            user.setZipcode(requestDto.getZipcode().trim());
        }
        if (hasText(requestDto.getAddress1())) {
            user.setAddress1(requestDto.getAddress1().trim());
        }
        if (hasText(requestDto.getAddress2())) {
            user.setAddress2(requestDto.getAddress2().trim());
        }

        if (hasText(requestDto.getRegistrationNumber())) {
            String newRegNo = requestDto.getRegistrationNumber().trim();
            if (!newRegNo.equals(user.getRegistrationNumber())
                && userRepository.existsByRegistrationNumber(newRegNo)) {
                throw new CustomException(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
            }
            user.updateRegistrationNumber(newRegNo);
        }

        if (hasText(requestDto.getPhoneNumber())) {
            String newPhone = requestDto.getPhoneNumber().trim();
            String newPhoneHash = User.hashValue(newPhone);

            if (!newPhoneHash.equals(user.getPhoneNumberHash())) {
                if (!phoneAuthService.isPhoneVerified(newPhone)) {
                    throw new CustomException(ErrorCode.PHONE_NOT_VERIFIED);
                }
                if (userRepository.existsByPhoneNumberHash(newPhoneHash)) {
                    throw new CustomException(ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
                }
                user.updatePhoneNumber(newPhone);
                phoneAuthService.clearVerification(newPhone);
            }
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
        if (requestDto.getBirthDate() != null) {
            user.setBirthDate(requestDto.getBirthDate());
        }
        user.setResignationDate(requestDto.getResignationDate());
        // 사용자의 상태를 AVAILABLE로 변경
        user.setStatus(Status.AVAILABLE);

        if (requestDto.getAuthorities() != null) {
            user.getAuthorities().clear();
            requestDto.getAuthorities().forEach(user::addAuthority);
        } else {
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
        }
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<PendingUserSummaryResponseDto> getPendingSignupUsers(Long adminId) {
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        return userRepository.findAllByStatusWithDepartment(Status.PENDING).stream()
            .map(PendingUserSummaryResponseDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminUserSummaryResponseDto> getUsers(
            Long adminId, String keyword, Pageable pageable) {
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        Set<Long> pendingMyInfoUserIds =
            myInfoUpdateRequestRepository
                .findDistinctUserIdsByStatus(MyInfoUpdateRequestStatus.PENDING)
                .stream()
                .collect(Collectors.toSet());

        String normalizedKeyword = hasText(keyword) ? keyword.trim() : null;

        return userRepository.findAllWithDepartmentAndKeyword(normalizedKeyword, pageable)
            .map(
                u -> AdminUserSummaryResponseDto.from(u, pendingMyInfoUserIds.contains(u.getId())));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponseDto getUserDetail(Long adminId, Long userId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateRegistrationAuthority(admin);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean hasPendingMyInfoRequest =
                myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                        userId, MyInfoUpdateRequestStatus.PENDING);

        return AdminUserDetailResponseDto.from(user, hasPendingMyInfoRequest);
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
                .orElseThrow(
                    () ->
                        new CustomException(
                            ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

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
                .orElseThrow(
                    () ->
                        new CustomException(
                            ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_FOUND));

        request.reject(admin, reason.trim());
        myInfoUpdateRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<MyInfoUpdateRequestSummaryResponseDto> getPendingMyInfoUpdateRequests(
        Long adminId) {
        User admin =
            userRepository
                .findById(adminId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateMyInfoApprovalAuthority(admin);

        return myInfoUpdateRequestRepository
            .findAllByStatusWithUser(MyInfoUpdateRequestStatus.PENDING)
            .stream()
            .map(MyInfoUpdateRequestSummaryResponseDto::from)
            .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateRegistrationAuthority(User admin) {
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
        }
    }

    private void validateMyInfoApprovalAuthority(User admin) {
        if (admin.getRole() != Role.ADMIN && admin.getRole() != Role.MASTER_ADMIN) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_MY_INFO_UPDATE_APPROVAL);
        }
    }
}
