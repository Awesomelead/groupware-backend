package kr.co.awesomelead.groupware_backend.domain.admin;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.admin.dto.request.UserApprovalRequestDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.AdminUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.MyInfoUpdateRequestSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.dto.response.PendingUserSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.admin.enums.AuthorityAction;
import kr.co.awesomelead.groupware_backend.domain.admin.service.AdminService;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.MyInfoUpdateRequestRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService 클래스의")
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private MyInfoUpdateRequestRepository myInfoUpdateRequestRepository;
    @Mock private PhoneAuthService phoneAuthService;
    @InjectMocks private AdminService adminService;
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

    @Nested
    @DisplayName("getPendingSignupUsers 메서드는")
    class Describe_getPendingSignupUsers {

        @Test
        @DisplayName("관리자가 조회하면 PENDING 사용자 목록을 반환한다")
        void it_returns_pending_users() {
            // given
            Department department =
                    Department.builder().id(1L).name(DepartmentName.MANAGEMENT_SUPPORT).build();
            User pendingUser =
                    User.builder()
                            .id(21L)
                            .nameKor("홍길동")
                            .status(Status.PENDING)
                            .department(department)
                            .build();
            when(userRepository.findAllByStatusWithDepartment(Status.PENDING))
                    .thenReturn(List.of(pendingUser));

            // when
            List<PendingUserSummaryResponseDto> result =
                    adminService.getPendingSignupUsers(adminId);

            // then
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0).getUserId()).isEqualTo(21L);
            assertThat(result.get(0).getNameKor()).isEqualTo("홍길동");
            assertThat(result.get(0).getDepartmentName())
                    .isEqualTo(DepartmentName.MANAGEMENT_SUPPORT);
            assertThat(result.get(0).getStatus()).isEqualTo(Status.PENDING);
        }

        @Test
        @DisplayName("관리자 권한이 없는 사용자가 조회하면 NO_AUTHORITY_FOR_REGISTRATION 에러를 던진다")
        void it_throws_when_requester_is_not_admin() {
            // given
            User normalUser = new User();
            normalUser.setRole(Role.USER);
            when(userRepository.findById(adminId)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> adminService.getPendingSignupUsers(adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
        }
    }

    @Nested
    @DisplayName("getUsers 메서드는")
    class Describe_getUsers {

        @Test
        @DisplayName("관리자가 조회하면 직원 목록과 수정요청 뱃지 여부를 반환한다")
        void it_returns_users_with_pending_my_info_badge() {
            // given
            Department department =
                    Department.builder().id(1L).name(DepartmentName.MANAGEMENT_SUPPORT).build();
            User user = User.builder().id(17L).nameKor("고영민").department(department).build();
            user.setStatus(Status.AVAILABLE);

            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
            when(userRepository.findAllWithDepartmentAndKeyword(
                            "홍길동", Position.STAFF, 11L, JobType.MANAGEMENT, Role.USER, pageable))
                    .thenReturn(userPage);
            when(myInfoUpdateRequestRepository.findDistinctUserIdsByStatus(
                            MyInfoUpdateRequestStatus.PENDING))
                    .thenReturn(List.of(17L));

            // when
            Page<AdminUserSummaryResponseDto> result =
                    adminService.getUsers(
                            adminId,
                            "홍길동",
                            Position.STAFF,
                            11L,
                            JobType.MANAGEMENT,
                            Role.USER,
                            pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1L);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(17L);
            assertThat(result.getContent().get(0).isHasPendingMyInfoRequest()).isEqualTo(true);
            assertThat(result.getContent().get(0).getSignupStatus()).isEqualTo(Status.AVAILABLE);
        }

        @Test
        @DisplayName("권한 없는 사용자가 조회하면 NO_AUTHORITY_FOR_REGISTRATION 에러를 던진다")
        void it_throws_when_requester_is_not_admin() {
            // given
            User normalUser = new User();
            normalUser.setRole(Role.USER);
            when(userRepository.findById(adminId)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(
                            () ->
                                    adminService.getUsers(
                                            adminId,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            PageRequest.of(0, 20)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
        }
    }

    @Nested
    @DisplayName("getUserDetail 메서드는")
    class Describe_getUserDetail {

        @Test
        @DisplayName("관리자가 조회하면 사용자 상세를 반환한다")
        void it_returns_user_detail() {
            // given
            Department department =
                    Department.builder().id(11L).name(DepartmentName.MANAGEMENT_SUPPORT).build();
            User user =
                    User.builder()
                            .id(17L)
                            .nameKor("고영민")
                            .department(department)
                            .role(Role.USER)
                            .build();

            when(userRepository.findById(17L)).thenReturn(Optional.of(user));
            when(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                            17L, MyInfoUpdateRequestStatus.PENDING))
                    .thenReturn(true);

            // when
            AdminUserDetailResponseDto result = adminService.getUserDetail(adminId, 17L);

            // then
            assertThat(result.getUserId()).isEqualTo(17L);
            assertThat(result.getDepartmentId()).isEqualTo(11L);
            assertThat(result.getDepartmentName()).isEqualTo(DepartmentName.MANAGEMENT_SUPPORT);
            assertThat(result.isHasPendingMyInfoRequest()).isEqualTo(true);
        }

        @Test
        @DisplayName("상세 조회 대상이 없으면 USER_NOT_FOUND 에러를 던진다")
        void it_throws_when_target_user_not_found() {
            // given
            when(userRepository.findById(17L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.getUserDetail(adminId, 17L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("권한 없는 사용자가 조회하면 NO_AUTHORITY_FOR_REGISTRATION 에러를 던진다")
        void it_throws_when_requester_is_not_admin() {
            // given
            User normalUser = new User();
            normalUser.setRole(Role.USER);
            when(userRepository.findById(adminId)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(() -> adminService.getUserDetail(adminId, 17L))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_REGISTRATION);
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

    @Nested
    @DisplayName("updateUserAuthority 메서드는")
    class Describe_updateUserAuthority {

        @Test
        @DisplayName("관리자가 여러 권한을 ADD 하면 모두 추가된다")
        void it_adds_multiple_authorities() {
            // given
            User targetUser = User.builder().id(userId).role(Role.USER).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

            List<Authority> authorities = List.of(Authority.ACCESS_NOTICE, Authority.ACCESS_VISIT);

            // when
            adminService.updateUserAuthority(userId, authorities, AuthorityAction.ADD, adminId);

            // then
            assertThat(targetUser.hasAuthority(Authority.ACCESS_NOTICE)).isEqualTo(true);
            assertThat(targetUser.hasAuthority(Authority.ACCESS_VISIT)).isEqualTo(true);
            verify(userRepository).save(targetUser);
        }

        @Test
        @DisplayName("관리자가 여러 권한을 REMOVE 하면 모두 제거된다")
        void it_removes_multiple_authorities() {
            // given
            User targetUser = User.builder().id(userId).role(Role.USER).build();
            targetUser.addAuthority(Authority.ACCESS_NOTICE);
            targetUser.addAuthority(Authority.ACCESS_VISIT);
            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

            List<Authority> authorities = List.of(Authority.ACCESS_NOTICE, Authority.ACCESS_VISIT);

            // when
            adminService.updateUserAuthority(userId, authorities, AuthorityAction.REMOVE, adminId);

            // then
            assertThat(targetUser.hasAuthority(Authority.ACCESS_NOTICE)).isEqualTo(false);
            assertThat(targetUser.hasAuthority(Authority.ACCESS_VISIT)).isEqualTo(false);
            verify(userRepository).save(targetUser);
        }

        @Test
        @DisplayName("이미 가진 권한을 ADD 하면 AUTHORITY_ALREADY_ASSIGNED 에러를 던진다")
        void it_throws_when_adding_already_assigned_authority() {
            // given
            User targetUser = User.builder().id(userId).role(Role.USER).build();
            targetUser.addAuthority(Authority.ACCESS_NOTICE);
            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(
                            () ->
                                    adminService.updateUserAuthority(
                                            userId,
                                            List.of(Authority.ACCESS_NOTICE),
                                            AuthorityAction.ADD,
                                            adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.AUTHORITY_ALREADY_ASSIGNED);
        }

        @Test
        @DisplayName("없는 권한을 REMOVE 하면 AUTHORITY_NOT_ASSIGNED 에러를 던진다")
        void it_throws_when_removing_not_assigned_authority() {
            // given
            User targetUser = User.builder().id(userId).role(Role.USER).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(
                            () ->
                                    adminService.updateUserAuthority(
                                            userId,
                                            List.of(Authority.ACCESS_NOTICE),
                                            AuthorityAction.REMOVE,
                                            adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.AUTHORITY_NOT_ASSIGNED);
        }

        @Test
        @DisplayName("권한 목록이 비어 있으면 INVALID_ARGUMENT 에러를 던진다")
        void it_throws_when_authority_list_is_empty() {
            // given
            User targetUser = User.builder().id(userId).role(Role.USER).build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(
                            () ->
                                    adminService.updateUserAuthority(
                                            userId, List.of(), AuthorityAction.ADD, adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_ARGUMENT);
        }

        @Test
        @DisplayName("관리자 권한이 없는 유저가 권한 수정을 시도하면 NO_AUTHORITY_FOR_ROLE_UPDATE 에러를 던진다")
        void it_throws_when_requester_is_not_admin() {
            // given
            User normalUser = new User();
            normalUser.setRole(Role.USER);
            when(userRepository.findById(adminId)).thenReturn(Optional.of(normalUser));

            // when & then
            assertThatThrownBy(
                            () ->
                                    adminService.updateUserAuthority(
                                            userId,
                                            List.of(Authority.ACCESS_NOTICE),
                                            AuthorityAction.ADD,
                                            adminId))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.NO_AUTHORITY_FOR_ROLE_UPDATE);
        }
    }

    @Nested
    @DisplayName("개인정보 수정 요청 승인/반려 메서드는")
    class Describe_myInfoUpdateApproval {

        @Test
        @DisplayName("관리자가 승인하면 요청 상태가 APPROVED로 바뀌고 사용자 정보가 반영된다")
        void approveMyInfoUpdate_success() {
            // given
            User targetUser =
                    User.builder().id(userId).nameEng("OLD").phoneNumber("01011112222").build();
            targetUser.setPhoneNumberHash(User.hashValue("01011112222"));
            MyInfoUpdateRequest request =
                    MyInfoUpdateRequest.builder()
                            .id(10L)
                            .user(targetUser)
                            .requestedNameEng("NEW")
                            .requestedPhoneNumber("01099998888")
                            .requestedPhoneNumberHash(User.hashValue("01099998888"))
                            .status(MyInfoUpdateRequestStatus.PENDING)
                            .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
            when(myInfoUpdateRequestRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                            userId, MyInfoUpdateRequestStatus.PENDING))
                    .thenReturn(Optional.of(request));
            when(userRepository.existsByPhoneNumberHash(User.hashValue("01099998888")))
                    .thenReturn(false);

            // when
            adminService.approveMyInfoUpdate(userId, adminId);

            // then
            assertThat(targetUser.getNameEng()).isEqualTo("NEW");
            assertThat(targetUser.getPhoneNumber()).isEqualTo("01099998888");
            assertThat(request.getStatus()).isEqualTo(MyInfoUpdateRequestStatus.APPROVED);
            verify(userRepository).save(targetUser);
            verify(myInfoUpdateRequestRepository).save(request);
        }

        @Test
        @DisplayName("관리자가 반려하면 요청 상태가 REJECTED로 바뀐다")
        void rejectMyInfoUpdate_success() {
            // given
            User targetUser = User.builder().id(userId).build();
            MyInfoUpdateRequest request =
                    MyInfoUpdateRequest.builder()
                            .id(10L)
                            .user(targetUser)
                            .status(MyInfoUpdateRequestStatus.PENDING)
                            .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
            when(myInfoUpdateRequestRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                            userId, MyInfoUpdateRequestStatus.PENDING))
                    .thenReturn(Optional.of(request));

            // when
            adminService.rejectMyInfoUpdate(userId, "증빙 불충분", adminId);

            // then
            assertThat(request.getStatus()).isEqualTo(MyInfoUpdateRequestStatus.REJECTED);
            assertThat(request.getRejectReason()).isEqualTo("증빙 불충분");
            verify(myInfoUpdateRequestRepository).save(request);
        }

        @Test
        @DisplayName("대기 요청 목록 조회 시 PENDING 요청 목록을 반환한다")
        void getPendingMyInfoUpdateRequests_success() {
            // given
            User targetUser =
                    User.builder().id(userId).nameKor("홍길동").email("hong@test.com").build();
            MyInfoUpdateRequest request =
                    MyInfoUpdateRequest.builder()
                            .id(77L)
                            .user(targetUser)
                            .requestedNameEng("HONG")
                            .status(MyInfoUpdateRequestStatus.PENDING)
                            .build();
            when(myInfoUpdateRequestRepository.findAllByStatusWithUser(
                            MyInfoUpdateRequestStatus.PENDING))
                    .thenReturn(List.of(request));

            // when
            List<MyInfoUpdateRequestSummaryResponseDto> result =
                    adminService.getPendingMyInfoUpdateRequests(adminId);

            // then
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0).getRequestId()).isEqualTo(77L);
            assertThat(result.get(0).getUserId()).isEqualTo(userId);
            assertThat(result.get(0).getRequestedNameEng()).isEqualTo("HONG");
        }
    }
}
