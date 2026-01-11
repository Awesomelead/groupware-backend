package kr.co.awesomelead.groupware_backend.domain.admin;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private AdminService adminService;

    @Test
    @DisplayName("사용자 등록 승인 성공")
    void approveUserRegistration_Success() {
        // given
        Long adminId = 100L;
        Long userId = 1L;

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(Role.ADMIN); // 관리자 권한 설정

        User pendingUser = new User(); // 테스트용 User 객체 생성
        pendingUser.setId(userId);
        pendingUser.setStatus(Status.PENDING);

        UserApprovalRequestDto requestDto = new UserApprovalRequestDto();
        requestDto.setHireDate(LocalDate.of(2025, 9, 22));
        requestDto.setJobType("정규직");
        requestDto.setPosition("사원");
        requestDto.setWorkLocation(Company.AWESOME);
        requestDto.setRole(Role.USER);

        // userRepository.findById가 호출되면 PENDING 상태의 유저를 반환하도록 설정
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));

        // when
        adminService.approveUserRegistration(userId, requestDto, adminId);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // 캡처된 User 객체의 필드가 예상대로 업데이트되었는지 검증
        assertThat(savedUser.getStatus()).isEqualTo(Status.AVAILABLE);
        assertThat(savedUser.getHireDate()).isEqualTo(requestDto.getHireDate());
        assertThat(savedUser.getPosition()).isEqualTo(requestDto.getPosition());
        assertThat(savedUser.getRole()).isEqualTo(requestDto.getRole());
    }

    @Test
    @DisplayName("사용자 등록 승인 실패 - 관리자 권한 없음")
    void approveUserRegistration_Fail_NoAuthority() {
        // given
        Long adminId = 100L;
        Long userId = 1L;

        User notAdmin = new User();
        notAdmin.setId(adminId);
        notAdmin.setRole(Role.USER); // 관리자가 아닌 일반 유저

        UserApprovalRequestDto requestDto = new UserApprovalRequestDto();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(notAdmin));

        // when & then
        CustomException exception =
                assertThrows(
                        CustomException.class,
                        () -> {
                            adminService.approveUserRegistration(userId, requestDto, adminId);
                        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
        verify(userRepository, never()).save(ArgumentMatchers.any(User.class));
    }

    @DisplayName("사용자 등록 승인 실패 - 대상 사용자를 찾을 수 없음")
    void approveUserRegistration_Fail_UserNotFound() {
        // given
        Long adminId = 100L;
        Long userId = 99L;

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(Role.ADMIN);

        UserApprovalRequestDto requestDto = new UserApprovalRequestDto();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        CustomException exception =
                assertThrows(
                        CustomException.class,
                        () -> {
                            adminService.approveUserRegistration(userId, requestDto, adminId);
                        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 등록 승인 실패 - 이미 처리된 요청")
    void approveUserRegistration_Fail_AlreadyProcessed() {
        // given
        Long adminId = 100L;
        Long userId = 1L;

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(Role.ADMIN);

        User availableUser = new User();
        availableUser.setId(userId);
        availableUser.setStatus(Status.AVAILABLE);

        UserApprovalRequestDto requestDto = new UserApprovalRequestDto();

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(userId)).thenReturn(Optional.of(availableUser));

        // when & then
        CustomException exception =
                assertThrows(
                        CustomException.class,
                        () -> {
                            adminService.approveUserRegistration(userId, requestDto, adminId);
                        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_SIGNUP_REQUEST);
        verify(userRepository, never()).save(ArgumentMatchers.any(User.class));
    }
}
