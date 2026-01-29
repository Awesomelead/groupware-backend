package kr.co.awesomelead.groupware_backend.domain.admin;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 클래스의")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @InjectMocks
    private AdminService adminService;
    private final Long adminId = 100L;
    private final Long userId = 1L;
    private final UserApprovalRequestDto requestDto = createRequestDto();

    @BeforeEach
    void setup() {
        User admin = new User();
        admin.setId(adminId);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
    }

    @Nested
    @DisplayName("approveUserRegistration 메서드는")
    class Describe_approveUserRegistration {

        @Nested
        @DisplayName("올바른 관리자가 대기 중인 사용자를 승인하면")
        class Context_with_admin_user {

            @Test
            @DisplayName("사용자 상태를 AVAILABLE로 변경하고 정보를 업데이트한다")
            void it_updates_user_info_and_status() {
                // given
                Department department =
                    Department.builder().id(1L).name(DepartmentName.SALES_DEPT).build();
                User pendingUser = new User();
                pendingUser.setId(userId);
                pendingUser.setStatus(Status.PENDING);

                when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));
                when(departmentRepository.findById(any())).thenReturn(Optional.of(department));

                // when
                adminService.approveUserRegistration(userId, requestDto, adminId);

                // then
                ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(userCaptor.capture());
                User savedUser = userCaptor.getValue();

                assertThat(savedUser.getStatus()).isEqualTo(Status.AVAILABLE);
                assertThat(savedUser.getRole()).isEqualTo(Role.USER);
            }
        }

        @Nested
        @DisplayName("현장직에 대해 ADMIN 권한을 주려고 하면")
        class Context_with_field_job_and_admin_role {

            @Test
            @DisplayName("INVALID_JOB_TYPE_FOR_ADMIN_ROLE 에러를 던진다")
            void it_throws_invalid_job_type_for_admin_role_exception() {
                // given
                Department department =
                    Department.builder().id(1L).name(DepartmentName.SALES_DEPT).build();
                User pendingUser = new User();
                pendingUser.setStatus(Status.PENDING);

                when(userRepository.findById(userId)).thenReturn(Optional.of(pendingUser));
                when(departmentRepository.findById(any())).thenReturn(Optional.of(department));

                UserApprovalRequestDto invalidRequestDto = createRequestDto();
                invalidRequestDto.setJobType(JobType.FIELD);
                invalidRequestDto.setRole(Role.ADMIN);

                // when & then
                assertThatThrownBy(
                    () ->
                        adminService.approveUserRegistration(
                            userId, invalidRequestDto, adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_JOB_TYPE_FOR_ADMIN_ROLE);
            }
        }

        @Nested
        @DisplayName("이미 승인된 유저를 다시 승인하려 하면")
        class Context_with_already_available_user {

            @Test
            @DisplayName("DUPLICATED_SIGNUP_REQUEST 에러를 던진다")
            void it_throws_duplicated_signup_request_exception() {
                // given
                User availableUser = new User();
                availableUser.setStatus(Status.AVAILABLE);
                when(userRepository.findById(userId)).thenReturn(Optional.of(availableUser));

                // when & then
                assertThatThrownBy(
                    () ->
                        adminService.approveUserRegistration(
                            userId, requestDto, adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATED_SIGNUP_REQUEST);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 유저 ID를 승인하려 하면")
        class Context_with_non_existent_user {

            @Test
            @DisplayName("USER_NOT_FOUND 에러를 던진다")
            void it_throws_user_not_found_exception() {
                // given
                when(userRepository.findById(userId)).thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(
                    () ->
                        adminService.approveUserRegistration(
                            userId, requestDto, adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
            }
        }

        @Nested
        @DisplayName("관리자 권한이 없는 유저가 승인을 시도하면")
        class Context_with_normal_user {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_REGISTRATION 에러를 던진다")
            void it_throws_no_authority_exception() {
                // given
                User normalUser = new User();
                normalUser.setRole(Role.USER);
                when(userRepository.findById(adminId)).thenReturn(Optional.of(normalUser));

                // when & then
                assertThatThrownBy(
                    () ->
                        adminService.approveUserRegistration(
                            userId, requestDto, adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
            }
        }
    }

    private UserApprovalRequestDto createRequestDto() {
        UserApprovalRequestDto dto = new UserApprovalRequestDto();
        dto.setJobType(JobType.MANAGEMENT);
        dto.setDepartmentId(1L);
        dto.setHireDate(LocalDate.now());
        dto.setPosition(Position.STAFF);
        return dto;
    }

    @Nested
    @DisplayName("updateUserRole 메서드는")
    class Describe_updateUserRole {

        @Nested
        @DisplayName("올바른 관리자가 사용자의 역할을 업데이트하면")
        class Context_with_admin_user {

            @Test
            @DisplayName("사용자의 역할이 업데이트된다")
            void it_updates_user_role_successfully() {
                // given
                User user = User.builder().id(1L).role(Role.USER).build();

                when(userRepository.findById(1L)).thenReturn(Optional.of(user));

                // when
                adminService.updateUserRole(1L, Role.ADMIN, 100L);

                // then
                assertThat(user.getRole()).isEqualTo(Role.ADMIN);
                assertThat(user.hasAuthority(Authority.ACCESS_NOTICE)).isEqualTo(true);
                assertThat(user.hasAuthority(Authority.MANAGE_EMPLOYEE_DATA)).isEqualTo(true);
            }
        }

        @Nested
        @DisplayName("관리자 권한이 없는 유저가 역할 업데이트를 시도하면")
        class Context_with_normal_user {

            @Test
            @DisplayName("NO_AUTHORITY_FOR_ROLE_UPDATE 에러를 던진다")
            void it_throws_no_authority_exception() {
                // given
                User normalUser = new User();
                normalUser.setRole(Role.USER);
                when(userRepository.findById(adminId)).thenReturn(Optional.of(normalUser));

                // when & then
                assertThatThrownBy(() -> adminService.updateUserRole(1L, Role.ADMIN, adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_ROLE_UPDATE);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 유저 ID로 역할 업데이트를 시도하면")
        class Context_with_non_existent_user {

            @Test
            @DisplayName("USER_NOT_FOUND 에러를 던진다")
            void it_throws_user_not_found_exception() {
                // given
                when(userRepository.findById(1L)).thenReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> adminService.updateUserRole(1L, Role.ADMIN, adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
            }
        }
    }
}
