package kr.co.awesomelead.groupware_backend.domain.admin.service;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
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

        // 1. userId로 PENDING 상태의 사용자를 찾습니다.
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.DUPLICATED_SIGNUP_REQUEST);
        }

        // 2. DTO의 정보로 사용자 엔티티를 업데이트합니다.
        user.setHireDate(requestDto.getHireDate());
        user.setJobType(requestDto.getJobType());
        user.setPosition(requestDto.getPosition());
        user.setWorkLocation(requestDto.getWorkLocation());
        user.setRole(requestDto.getRole());

        // 3. 사용자의 상태를 AVAILABLE로 변경합니다.
        user.setStatus(Status.AVAILABLE);

        userRepository.save(user);
    }
}
