package kr.co.awesomelead.groupware_backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoResponseDto;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.UpdateMyInfoRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.MyInfoUpdateRequest;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhoneAuthService phoneAuthService;
    @Mock
    private MyInfoUpdateRequestRepository myInfoUpdateRequestRepository;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private UserService userService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME_KOR = "김철수";
    private static final String TEST_NAME_ENG = "Kim Chulsoo";
    private static final String TEST_PHONE = "01012345678";
    private static final String NEW_NAME_ENG = "Kim Chulsoo Updated";
    private static final String NEW_PHONE = "01098765432";

    @BeforeEach
    void setUp() {
        // UserDetails Mock 설정
        given(userDetails.getUsername()).willReturn(TEST_EMAIL);
    }

    // Helper method: 새로운 User 객체 생성
    private User createTestUser() {
        Department testDepartment =
            Department.builder()
                .id(1L)
                .name(DepartmentName.CHUNGNAM_HQ)
                .company(Company.AWESOME)
                .build();

        User user =
            User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .password("encodedPassword")
                .nameKor(TEST_NAME_KOR)
                .nameEng(TEST_NAME_ENG)
                .nationality("대한민국")
                .registrationNumber("9001011234567")
                .phoneNumber(TEST_PHONE)
                .birthDate(LocalDate.of(1990, 1, 1))
                .jobType(JobType.MANAGEMENT)
                .position(Position.ASSISTANT_MANAGER)
                .role(Role.USER)
                .status(Status.AVAILABLE)
                .workLocation(Company.AWESOME)
                .department(testDepartment)
                .build();

        // 전화번호 해시 설정
        user.setPhoneNumberHash(User.hashValue(TEST_PHONE));

        return user;
    }

    @Nested
    @DisplayName("내 정보 조회")
    class GetMyInfoTest {

        @Test
        @DisplayName("성공: 사용자 정보를 정상적으로 조회한다")
        void getMyInfo_Success() {
            // given
            User testUser = createTestUser();
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));

            // when
            MyInfoResponseDto response = userService.getMyInfo(userDetails);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getNameKor()).isEqualTo(TEST_NAME_KOR);
            assertThat(response.getNameEng()).isEqualTo(TEST_NAME_ENG);
            assertThat(response.getPhoneNumber()).isEqualTo(TEST_PHONE);
            assertThat(response.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
            assertThat(response.getNationality()).isEqualTo("대한민국");
            assertThat(response.getRegistrationNumberFront()).isEqualTo("900101-1******");
            assertThat(response.getWorkLocation()).isEqualTo(Company.AWESOME);
            assertThat(response.getDepartmentName()).isEqualTo(DepartmentName.CHUNGNAM_HQ);
            assertThat(response.getJobType()).isEqualTo(JobType.MANAGEMENT);
            assertThat(response.getPosition()).isEqualTo(Position.ASSISTANT_MANAGER);

            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없는 경우")
        void getMyInfo_UserNotFound() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyInfo(userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("내 정보 수정")
    class UpdateMyInfoTest {

        private UpdateMyInfoRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = new UpdateMyInfoRequestDto();
        }

        @Test
        @DisplayName("성공: 영문 이름만 수정 요청 생성")
        void updateMyInfo_NameEngOnly_Success() {
            // given
            User testUser = createTestUser();
            requestDto.setNameEng(NEW_NAME_ENG);

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);

            // when
            MyInfoResponseDto response = userService.updateMyInfo(userDetails, requestDto);

            // then
            assertThat(response).isNotNull();
            assertThat(testUser.getNameEng()).isEqualTo(TEST_NAME_ENG);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(myInfoUpdateRequestRepository).save(any(MyInfoUpdateRequest.class));
        }

        @Test
        @DisplayName("성공: 전화번호만 수정 요청 생성 (알리고 인증 완료)")
        void updateMyInfo_PhoneNumberOnly_Success() {
            // given
            User testUser = createTestUser();
            requestDto.setPhoneNumber(NEW_PHONE);
            String newPhoneHash = User.hashValue(NEW_PHONE);

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);
            given(phoneAuthService.isPhoneVerified(NEW_PHONE)).willReturn(true);
            given(userRepository.existsByPhoneNumberHash(newPhoneHash)).willReturn(false);

            // when
            MyInfoResponseDto response = userService.updateMyInfo(userDetails, requestDto);

            // then
            assertThat(response).isNotNull();
            assertThat(testUser.getPhoneNumber()).isEqualTo(TEST_PHONE);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(NEW_PHONE);
            verify(userRepository).existsByPhoneNumberHash(newPhoneHash);
            verify(myInfoUpdateRequestRepository).save(any(MyInfoUpdateRequest.class));
            verify(phoneAuthService).clearVerification(NEW_PHONE);
        }

        @Test
        @DisplayName("성공: 영문 이름과 전화번호 모두 수정 요청 생성")
        void updateMyInfo_BothFields_Success() {
            // given
            User testUser = createTestUser();
            requestDto.setNameEng(NEW_NAME_ENG);
            requestDto.setPhoneNumber(NEW_PHONE);
            String newPhoneHash = User.hashValue(NEW_PHONE);

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);
            given(phoneAuthService.isPhoneVerified(NEW_PHONE)).willReturn(true);
            given(userRepository.existsByPhoneNumberHash(newPhoneHash)).willReturn(false);

            // when
            MyInfoResponseDto response = userService.updateMyInfo(userDetails, requestDto);

            // then
            assertThat(response).isNotNull();
            assertThat(testUser.getNameEng()).isEqualTo(TEST_NAME_ENG);
            assertThat(testUser.getPhoneNumber()).isEqualTo(TEST_PHONE);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(NEW_PHONE);
            verify(myInfoUpdateRequestRepository).save(any(MyInfoUpdateRequest.class));
        }

        @Test
        @DisplayName("실패: 영문 이름이 현재 이름과 동일하고 다른 변경사항이 없으면")
        void updateMyInfo_SameNameEng() {
            // given
            User testUser = createTestUser();
            requestDto.setNameEng(TEST_NAME_ENG); // 현재 이름과 동일

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(userDetails, requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MY_INFO_UPDATE_NO_CHANGES);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(myInfoUpdateRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 전화번호가 현재 전화번호와 동일한 경우")
        void updateMyInfo_SamePhoneNumber() {
            // given
            User testUser = createTestUser();
            requestDto.setPhoneNumber(TEST_PHONE); // 현재 전화번호와 동일

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(userDetails, requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NUMBER_ALREADY_SAME);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService, never()).isPhoneVerified(anyString());
            verify(myInfoUpdateRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 전화번호 인증을 하지 않은 경우")
        void updateMyInfo_PhoneNotVerified() {
            // given
            User testUser = createTestUser();
            requestDto.setPhoneNumber(NEW_PHONE);

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);
            given(phoneAuthService.isPhoneVerified(NEW_PHONE)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(userDetails, requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NOT_VERIFIED);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(NEW_PHONE);
            verify(myInfoUpdateRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 전화번호가 이미 사용 중인 경우")
        void updateMyInfo_PhoneNumberAlreadyExists() {
            // given
            User testUser = createTestUser();
            requestDto.setPhoneNumber(NEW_PHONE);
            String newPhoneHash = User.hashValue(NEW_PHONE);

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);
            given(phoneAuthService.isPhoneVerified(NEW_PHONE)).willReturn(true);
            given(userRepository.existsByPhoneNumberHash(newPhoneHash)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(userDetails, requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                    "errorCode", ErrorCode.PHONE_NUMBER_ALREADY_EXISTS);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(NEW_PHONE);
            verify(userRepository).existsByPhoneNumberHash(newPhoneHash);
            verify(myInfoUpdateRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 변경사항이 없는 경우")
        void updateMyInfo_NoChanges() {
            // given
            User testUser = createTestUser();
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(false);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(userDetails, requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MY_INFO_UPDATE_NO_CHANGES);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(myInfoUpdateRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 이미 대기중인 내 정보 수정 요청이 존재하면")
        void updateMyInfo_AlreadyPending() {
            // given
            User testUser = createTestUser();
            requestDto.setAddress1("서울시 강남구");
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.existsByUserIdAndStatus(
                testUser.getId(), MyInfoUpdateRequestStatus.PENDING))
                .willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(userDetails, requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                    "errorCode", ErrorCode.MY_INFO_UPDATE_ALREADY_PENDING);

            verify(myInfoUpdateRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없는 경우")
        void updateMyInfo_UserNotFound() {
            // given
            requestDto.setNameEng(NEW_NAME_ENG);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateMyInfo(userDetails, requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(myInfoUpdateRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("내 정보 수정 요청 취소")
    class CancelMyInfoUpdateRequestTest {

        @Test
        @DisplayName("성공: 본인 요청이 PENDING이면 취소된다")
        void cancelMyInfoUpdateRequest_success() {
            // given
            User testUser = createTestUser();
            MyInfoUpdateRequest request =
                    MyInfoUpdateRequest.builder()
                            .id(10L)
                            .user(testUser)
                            .status(MyInfoUpdateRequestStatus.PENDING)
                            .build();

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(myInfoUpdateRequestRepository.findById(10L)).willReturn(Optional.of(request));

            // when
            userService.cancelMyInfoUpdateRequest(userDetails, 10L);

            // then
            assertThat(request.getStatus()).isEqualTo(MyInfoUpdateRequestStatus.CANCELED);
            verify(myInfoUpdateRequestRepository).save(request);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 요청은 취소할 수 없다")
        void cancelMyInfoUpdateRequest_forbidden() {
            // given
            User me = createTestUser();
            User other = createTestUser();
            other.setId(99L);
            MyInfoUpdateRequest request =
                    MyInfoUpdateRequest.builder()
                            .id(10L)
                            .user(other)
                            .status(MyInfoUpdateRequestStatus.PENDING)
                            .build();

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(me));
            given(myInfoUpdateRequestRepository.findById(10L)).willReturn(Optional.of(request));

            // when & then
            assertThatThrownBy(() -> userService.cancelMyInfoUpdateRequest(userDetails, 10L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", ErrorCode.NO_AUTHORITY_FOR_MY_INFO_UPDATE_CANCEL);

            verify(myInfoUpdateRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: PENDING이 아니면 취소할 수 없다")
        void cancelMyInfoUpdateRequest_notCancelable() {
            // given
            User me = createTestUser();
            MyInfoUpdateRequest request =
                    MyInfoUpdateRequest.builder()
                            .id(10L)
                            .user(me)
                            .status(MyInfoUpdateRequestStatus.APPROVED)
                            .build();

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(me));
            given(myInfoUpdateRequestRepository.findById(10L)).willReturn(Optional.of(request));

            // when & then
            assertThatThrownBy(() -> userService.cancelMyInfoUpdateRequest(userDetails, 10L))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue(
                            "errorCode", ErrorCode.MY_INFO_UPDATE_REQUEST_NOT_CANCELABLE);

            verify(myInfoUpdateRequestRepository, never()).save(any());
        }
    }
}
