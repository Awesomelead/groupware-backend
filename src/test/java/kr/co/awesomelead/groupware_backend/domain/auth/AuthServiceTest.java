package kr.co.awesomelead.groupware_backend.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.LoginRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByPhoneRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.LoginResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.response.SignupResponseDto;
import kr.co.awesomelead.groupware_backend.domain.auth.service.AuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.EmailAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.RefreshTokenService;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.repository.SafetyTrainingSessionRepository;
import kr.co.awesomelead.groupware_backend.domain.user.dto.response.MyInfoAuthorityItemDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.mapper.UserMapper;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock private PhoneAuthService phoneAuthService;

    @Mock private EmailAuthService emailAuthService;

    @Mock private UserMapper userMapper;

    @Mock private NotificationService notificationService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JWTUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EntityManager entityManager;
    @Mock private SafetyTrainingSessionRepository safetyTrainingSessionRepository;

    @InjectMocks private AuthService authService;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PHONE = "01012345678";
    private final String OLD_PASSWORD = "oldPass123!@#";
    private final String NEW_PASSWORD = "newPass456!@#";
    private final String ENCODED_OLD_PASSWORD = "encoded_old_password";
    private final String ENCODED_NEW_PASSWORD = "encoded_new_password";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(TEST_EMAIL);
        testUser.setPassword(ENCODED_OLD_PASSWORD);
        testUser.setPhoneNumber(TEST_PHONE);
        testUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("로그인 성공 테스트 - 권한 정보 포함")
    void login_Success() {
        // given
        LoginRequestDto loginRequestDto = new LoginRequestDto();
        loginRequestDto.setEmail(TEST_EMAIL);
        loginRequestDto.setPassword(OLD_PASSWORD);

        testUser.addAuthority(Authority.MANAGE_DEPARTMENT_EDUCATION);

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        org.springframework.security.core.GrantedAuthority grantedAuthority = () -> "ROLE_USER";

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getName()).thenReturn(TEST_EMAIL);
        when(authentication.getAuthorities())
                .thenAnswer(invocation -> java.util.Collections.singletonList(grantedAuthority));

        when(jwtUtil.createJwt(anyString(), anyString(), anyLong())).thenReturn("access_token");
        when(refreshTokenService.createAndSaveRefreshToken(anyString(), anyString()))
                .thenReturn("refresh_token");
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // when
        LoginResponseDto response = authService.login(loginRequestDto);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getAuthorities()).isNotNull();
        assertThat(response.getAuthorities().size()).isEqualTo(1);

        MyInfoAuthorityItemDto authorityDto = response.getAuthorities().get(0);
        assertThat(authorityDto.getCode()).isEqualTo(Authority.MANAGE_DEPARTMENT_EDUCATION.name());
        assertThat(authorityDto.getLabel())
                .isEqualTo(Authority.MANAGE_DEPARTMENT_EDUCATION.getDescription());
        assertThat(authorityDto.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_Success() {
        // given
        SignupRequestDto signupDto = new SignupRequestDto();
        signupDto.setEmail("test@example.com");
        signupDto.setPassword("password123!");
        signupDto.setPasswordConfirm("password123!");
        signupDto.setNameKor("김어썸");
        signupDto.setNameEng("Awesome Kim");
        signupDto.setNationality("대한민국");
        signupDto.setZipcode("06234");
        signupDto.setAddress1("서울특별시 강남구 테헤란로 123");
        signupDto.setAddress2("어썸리드빌딩 5층");
        signupDto.setRegistrationNumber("950101-1234567");
        signupDto.setPhoneNumber("01012345678");
        signupDto.setCompany(Company.AWESOME);

        User mockUser =
                User.builder()
                        .email(signupDto.getEmail())
                        .nameKor(signupDto.getNameKor())
                        .nameEng(signupDto.getNameEng())
                        .nationality(signupDto.getNationality())
                        .zipcode(signupDto.getZipcode())
                        .address1(signupDto.getAddress1())
                        .address2(signupDto.getAddress2())
                        .registrationNumber(signupDto.getRegistrationNumber())
                        .phoneNumber(signupDto.getPhoneNumber())
                        .workLocation(Company.AWESOME)
                        .role(Role.USER)
                        .status(Status.PENDING)
                        .build();

        mockUser.onPrePersist();

        // 저장 후 반환될 User (ID 포함)
        User savedMockUser =
                User.builder()
                        .id(1L)
                        .email(signupDto.getEmail())
                        .nameKor(signupDto.getNameKor())
                        .nameEng(signupDto.getNameEng())
                        .nationality(signupDto.getNationality())
                        .zipcode(signupDto.getZipcode())
                        .address1(signupDto.getAddress1())
                        .address2(signupDto.getAddress2())
                        .registrationNumber(signupDto.getRegistrationNumber())
                        .phoneNumber(signupDto.getPhoneNumber())
                        .password("encodedPassword")
                        .workLocation(Company.AWESOME)
                        .role(Role.USER)
                        .status(Status.PENDING)
                        .birthDate(LocalDate.of(1995, 1, 1))
                        .phoneNumberHash(User.hashValue(signupDto.getPhoneNumber()))
                        .build();

        // Mock 설정
        when(emailAuthService.isEmailVerified(signupDto.getEmail())).thenReturn(true);
        when(phoneAuthService.isPhoneVerified(signupDto.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByEmail(signupDto.getEmail())).thenReturn(false);
        when(userRepository.existsByRegistrationNumber(signupDto.getRegistrationNumber()))
                .thenReturn(false);
        when(userMapper.toEntity(signupDto)).thenReturn(mockUser);
        when(bCryptPasswordEncoder.encode(signupDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedMockUser);

        // when
        SignupResponseDto result = authService.signup(signupDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(signupDto.getEmail());

        verify(userRepository, times(1)).save(any(User.class));
        verify(emailAuthService, times(1)).clearVerification(signupDto.getEmail());
        verify(phoneAuthService, times(1)).clearVerification(signupDto.getPhoneNumber());
        verify(notificationService, times(1))
                .sendAlertToAdminsRequiringApproval(any(), any(), any(), any(Map.class), any());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 비밀번호 불일치")
    void signup_Fail_PasswordMismatch() {
        // given
        SignupRequestDto signupDto = new SignupRequestDto();
        signupDto.setEmail("test@example.com");
        signupDto.setPassword("Password123!");
        signupDto.setPasswordConfirm("DifferentPassword123!");

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> authService.signup(signupDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never())
                .sendAlertToAdminsRequiringApproval(any(), any(), any(), any(Map.class), any());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 전화번호 미인증")
    void signup_Fail_PhoneNotVerified() {
        // given
        SignupRequestDto signupDto = new SignupRequestDto();
        signupDto.setEmail("test@example.com");
        signupDto.setPassword("Password123!");
        signupDto.setPasswordConfirm("Password123!");
        signupDto.setPhoneNumber("01012345678");

        // 전화번호 미인증
        when(phoneAuthService.isPhoneVerified(signupDto.getPhoneNumber())).thenReturn(false);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> authService.signup(signupDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_NOT_VERIFIED);
        verify(userRepository, never()).save(any(User.class));
        // 전화번호 인증 실패 시 이메일 확인은 실행 안 됨
        verify(emailAuthService, never()).isEmailVerified(any());
        verify(notificationService, never())
                .sendAlertToAdminsRequiringApproval(any(), any(), any(), any(Map.class), any());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 미인증")
    void signup_Fail_EmailNotVerified() {
        // given
        SignupRequestDto signupDto = new SignupRequestDto();
        signupDto.setEmail("test@example.com");
        signupDto.setPassword("Password123!");
        signupDto.setPasswordConfirm("Password123!");
        signupDto.setPhoneNumber("01012345678");

        // 전화번호 인증은 통과
        when(phoneAuthService.isPhoneVerified(signupDto.getPhoneNumber())).thenReturn(true);
        // 이메일 미인증
        when(emailAuthService.isEmailVerified(signupDto.getEmail())).thenReturn(false);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> authService.signup(signupDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never())
                .sendAlertToAdminsRequiringApproval(any(), any(), any(), any(Map.class), any());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 중복")
    void signup_Fail_DuplicateEmail() {
        // given
        SignupRequestDto signupDto = new SignupRequestDto();
        signupDto.setEmail("test@example.com");
        signupDto.setPassword("password123!");
        signupDto.setPasswordConfirm("password123!");
        signupDto.setPhoneNumber("01012345678");

        // 이메일 & 휴대폰 인증 통과
        when(emailAuthService.isEmailVerified(signupDto.getEmail())).thenReturn(true);
        when(phoneAuthService.isPhoneVerified(signupDto.getPhoneNumber())).thenReturn(true);
        // 이메일 중복
        when(userRepository.existsByEmail(signupDto.getEmail())).thenReturn(true);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> authService.signup(signupDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never())
                .sendAlertToAdminsRequiringApproval(any(), any(), any(), any(Map.class), any());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 주민등록번호 중복")
    void signup_Fail_DuplicateRegistrationNumber() {
        // given
        SignupRequestDto signupDto = new SignupRequestDto();
        signupDto.setEmail("test@example.com");
        signupDto.setPassword("Password123!");
        signupDto.setPasswordConfirm("Password123!");
        signupDto.setPhoneNumber("01012345678");
        signupDto.setRegistrationNumber("950101-1234567");

        // 이메일 & 휴대폰 인증 통과
        when(emailAuthService.isEmailVerified(signupDto.getEmail())).thenReturn(true);
        when(phoneAuthService.isPhoneVerified(signupDto.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByEmail(signupDto.getEmail())).thenReturn(false);
        // 주민번호 중복
        when(userRepository.existsByRegistrationNumber(signupDto.getRegistrationNumber()))
                .thenReturn(true);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> authService.signup(signupDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
        verify(userRepository, never()).save(any(User.class));
        verify(notificationService, never())
                .sendAlertToAdminsRequiringApproval(any(), any(), any(), any(Map.class), any());
    }

    @Nested
    @DisplayName("이메일 인증 후 비밀번호 재설정")
    class ResetPasswordByEmailTest {

        private ResetPasswordByEmailRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = new ResetPasswordByEmailRequestDto();
            requestDto.setEmail(TEST_EMAIL);
            requestDto.setNewPassword(NEW_PASSWORD);
            requestDto.setNewPasswordConfirm(NEW_PASSWORD);
        }

        @Test
        @DisplayName("성공: 이메일 인증 후 비밀번호가 정상적으로 재설정된다")
        void resetPasswordByEmail_Success() {
            // given
            given(emailAuthService.isEmailVerified(TEST_EMAIL)).willReturn(true);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(bCryptPasswordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW_PASSWORD);

            // when
            authService.resetPasswordByEmail(requestDto);

            // then
            verify(emailAuthService).isEmailVerified(TEST_EMAIL);
            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(bCryptPasswordEncoder).encode(NEW_PASSWORD);
            verify(userRepository).save(testUser);
            verify(emailAuthService).clearVerification(TEST_EMAIL);
            assertThat(testUser.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("실패: 이메일 인증을 하지 않은 경우")
        void resetPasswordByEmail_NotVerified() {
            // given
            given(emailAuthService.isEmailVerified(TEST_EMAIL)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByEmail(requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);

            verify(emailAuthService).isEmailVerified(TEST_EMAIL);
            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 비밀번호 확인이 일치하지 않는 경우")
        void resetPasswordByEmail_PasswordMismatch() {
            // given
            requestDto.setNewPasswordConfirm("differentPassword!@#");
            given(emailAuthService.isEmailVerified(TEST_EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByEmail(requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);

            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이메일인 경우")
        void resetPasswordByEmail_UserNotFound() {
            // given
            given(emailAuthService.isEmailVerified(TEST_EMAIL)).willReturn(true);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByEmail(requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("이메일 비밀번호 찾기 계정 검증")
    class VerifyAccountByEmailTest {

        @Test
        @DisplayName("성공: 이메일 계정 선검증 통과")
        void checkAccountByEmail_Success() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));

            // when
            authService.checkAccountByEmail(TEST_EMAIL);

            // then
            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(emailAuthService, never()).isEmailVerified(anyString());
        }

        @Test
        @DisplayName("실패: 선검증 시 이메일 계정이 존재하지 않으면 USER_NOT_FOUND")
        void checkAccountByEmail_UserNotFound() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.checkAccountByEmail(TEST_EMAIL))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(emailAuthService, never()).isEmailVerified(anyString());
        }

        @Test
        @DisplayName("성공: 이메일 인증 완료 및 계정 존재 시 통과한다")
        void verifyAccountByEmail_Success() {
            // given
            given(emailAuthService.isEmailVerified(TEST_EMAIL)).willReturn(true);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));

            // when
            authService.verifyAccountByEmail(TEST_EMAIL);

            // then
            verify(emailAuthService).isEmailVerified(TEST_EMAIL);
            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("실패: 이메일 인증이 완료되지 않으면 EMAIL_NOT_VERIFIED")
        void verifyAccountByEmail_NotVerified() {
            // given
            given(emailAuthService.isEmailVerified(TEST_EMAIL)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.verifyAccountByEmail(TEST_EMAIL))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_NOT_VERIFIED);

            verify(emailAuthService).isEmailVerified(TEST_EMAIL);
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("실패: 이메일 계정이 존재하지 않으면 USER_NOT_FOUND")
        void verifyAccountByEmail_UserNotFound() {
            // given
            given(emailAuthService.isEmailVerified(TEST_EMAIL)).willReturn(true);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.verifyAccountByEmail(TEST_EMAIL))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(emailAuthService).isEmailVerified(TEST_EMAIL);
            verify(userRepository).findByEmail(TEST_EMAIL);
        }
    }

    @Nested
    @DisplayName("휴대폰 인증 후 비밀번호 재설정")
    class ResetPasswordByPhoneTest {

        private ResetPasswordByPhoneRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = new ResetPasswordByPhoneRequestDto();
            requestDto.setEmail(TEST_EMAIL);
            requestDto.setNewPassword(NEW_PASSWORD);
            requestDto.setNewPasswordConfirm(NEW_PASSWORD);
        }

        @Test
        @DisplayName("성공: 휴대폰 인증 후 비밀번호가 정상적으로 재설정된다")
        void resetPasswordByPhone_Success() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(true);
            given(bCryptPasswordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW_PASSWORD);

            // when
            authService.resetPasswordByPhone(requestDto);

            // then
            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(TEST_PHONE);
            verify(bCryptPasswordEncoder).encode(NEW_PASSWORD);
            verify(userRepository).save(testUser);
            verify(phoneAuthService).clearVerification(TEST_PHONE);
            assertThat(testUser.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이메일인 경우")
        void resetPasswordByPhone_EmailNotFound() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByPhone(requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService, never()).isPhoneVerified(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 휴대폰 인증을 하지 않은 경우")
        void resetPasswordByPhone_NotVerified() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByPhone(requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NOT_VERIFIED);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(TEST_PHONE);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 비밀번호 확인이 일치하지 않는 경우")
        void resetPasswordByPhone_PasswordMismatch() {
            // given
            requestDto.setNewPasswordConfirm("differentPassword!@#");

            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByPhone(requestDto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(TEST_PHONE);
            verify(userRepository, never()).save(any());
            verify(phoneAuthService, never()).clearVerification(anyString());
        }
    }

    @Nested
    @DisplayName("이메일 찾기 인증요청 전 계정 선검증")
    class CheckAccountForFindEmailTest {

        @Test
        @DisplayName("성공: 이름과 전화번호가 일치하면 통과")
        void checkAccountForFindEmail_Success() {
            // given
            testUser.setNameKor("홍길동");
            testUser.setPhoneNumberHash(User.hashValue(TEST_PHONE));
            given(userRepository.findByPhoneNumberHash(User.hashValue(TEST_PHONE)))
                    .willReturn(Optional.of(testUser));

            // when
            authService.checkAccountForFindEmail("홍길동", TEST_PHONE);

            // then
            verify(userRepository).findByPhoneNumberHash(User.hashValue(TEST_PHONE));
        }

        @Test
        @DisplayName("실패: 전화번호로 사용자를 찾을 수 없으면 USER_NOT_FOUND")
        void checkAccountForFindEmail_UserNotFound() {
            // given
            given(userRepository.findByPhoneNumberHash(User.hashValue(TEST_PHONE)))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.checkAccountForFindEmail("홍길동", TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByPhoneNumberHash(User.hashValue(TEST_PHONE));
        }

        @Test
        @DisplayName("실패: 이름이 일치하지 않으면 USER_NOT_FOUND")
        void checkAccountForFindEmail_NameMismatch() {
            // given
            testUser.setNameKor("김철수");
            testUser.setPhoneNumberHash(User.hashValue(TEST_PHONE));
            given(userRepository.findByPhoneNumberHash(User.hashValue(TEST_PHONE)))
                    .willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> authService.checkAccountForFindEmail("홍길동", TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByPhoneNumberHash(User.hashValue(TEST_PHONE));
        }
    }

    @Nested
    @DisplayName("휴대폰 비밀번호 찾기 계정 검증")
    class VerifyAccountByPhoneTest {

        @Test
        @DisplayName("성공: 이메일/전화번호 계정 선검증 통과")
        void checkAccountByPhone_Success() {
            // given
            testUser.setPhoneNumberHash(User.hashValue(TEST_PHONE));
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));

            // when
            authService.checkAccountByPhone(TEST_EMAIL, TEST_PHONE);

            // then
            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService, never()).isPhoneVerified(anyString());
        }

        @Test
        @DisplayName("실패: 선검증 시 이메일이 존재하지 않으면 USER_NOT_FOUND")
        void checkAccountByPhone_UserNotFound() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.checkAccountByPhone(TEST_EMAIL, TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService, never()).isPhoneVerified(anyString());
        }

        @Test
        @DisplayName("실패: 선검증 시 전화번호가 계정과 다르면 PHONE_NUMBER_MISMATCH")
        void checkAccountByPhone_PhoneNumberMismatch() {
            // given
            testUser.setPhoneNumberHash(User.hashValue("01099999999"));
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> authService.checkAccountByPhone(TEST_EMAIL, TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NUMBER_MISMATCH);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService, never()).isPhoneVerified(anyString());
        }

        @Test
        @DisplayName("성공: 이메일과 전화번호가 동일 계정과 일치하면 통과한다")
        void verifyAccountByPhone_Success() {
            // given
            testUser.setPhoneNumberHash(User.hashValue(TEST_PHONE));
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(true);

            // when
            authService.verifyAccountByPhone(TEST_EMAIL, TEST_PHONE);

            // then
            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(TEST_PHONE);
        }

        @Test
        @DisplayName("실패: 이메일이 존재하지 않으면 USER_NOT_FOUND")
        void verifyAccountByPhone_UserNotFound() {
            // given
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.verifyAccountByPhone(TEST_EMAIL, TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByEmail(TEST_EMAIL);
        }

        @Test
        @DisplayName("실패: 전화번호가 계정과 다르면 PHONE_NUMBER_MISMATCH")
        void verifyAccountByPhone_PhoneNumberMismatch() {
            // given
            testUser.setPhoneNumberHash(User.hashValue("01099999999"));
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> authService.verifyAccountByPhone(TEST_EMAIL, TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NUMBER_MISMATCH);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService, never()).isPhoneVerified(anyString());
        }

        @Test
        @DisplayName("실패: 전화번호 인증이 완료되지 않으면 PHONE_NOT_VERIFIED")
        void verifyAccountByPhone_NotVerified() {
            // given
            testUser.setPhoneNumberHash(User.hashValue(TEST_PHONE));
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(testUser));
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.verifyAccountByPhone(TEST_EMAIL, TEST_PHONE))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NOT_VERIFIED);

            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(phoneAuthService).isPhoneVerified(TEST_PHONE);
        }
    }

    @Nested
    @DisplayName("로그인 후 비밀번호 변경")
    class ResetPasswordTest {

        private ResetPasswordRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = new ResetPasswordRequestDto();
            requestDto.setCurrentPassword(OLD_PASSWORD);
            requestDto.setNewPassword(NEW_PASSWORD);
            requestDto.setNewPasswordConfirm(NEW_PASSWORD);
        }

        @Test
        @DisplayName("성공: 현재 비밀번호 확인 후 비밀번호가 정상적으로 변경된다")
        void resetPassword_Success() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(bCryptPasswordEncoder.matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD))
                    .willReturn(true);
            given(bCryptPasswordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW_PASSWORD);

            // when
            authService.resetPassword(requestDto, userId);

            // then
            verify(userRepository).findById(userId);
            verify(bCryptPasswordEncoder).matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD);
            verify(bCryptPasswordEncoder).encode(NEW_PASSWORD);
            verify(userRepository).save(testUser);
            assertThat(testUser.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("실패: 비밀번호 확인이 일치하지 않는 경우")
        void resetPassword_PasswordMismatch() {
            // given
            Long userId = 1L;
            requestDto.setNewPasswordConfirm("differentPassword!@#");

            // when & then
            assertThatThrownBy(() -> authService.resetPassword(requestDto, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);

            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자인 경우")
        void resetPassword_UserNotFound() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.resetPassword(requestDto, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findById(userId);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 현재 비밀번호가 일치하지 않는 경우")
        void resetPassword_CurrentPasswordMismatch() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(bCryptPasswordEncoder.matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.resetPassword(requestDto, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CURRENT_PASSWORD_MISMATCH);

            verify(bCryptPasswordEncoder).matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 새 비밀번호가 현재 비밀번호와 같은 경우")
        void resetPassword_SameAsCurrentPassword() {
            // given
            Long userId = 1L;
            requestDto.setNewPassword(OLD_PASSWORD);
            requestDto.setNewPasswordConfirm(OLD_PASSWORD);
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(bCryptPasswordEncoder.matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.resetPassword(requestDto, userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_AS_CURRENT_PASSWORD);

            verify(bCryptPasswordEncoder).matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴(deleteUser)")
    class DeleteUserTest {

        private Query mockQuery;

        @BeforeEach
        void setUp() {
            // EntityManager는 @PersistenceContext로 주입되어 @InjectMocks가 처리하지 못하므로
            // ReflectionTestUtils로 직접 주입한다.
            ReflectionTestUtils.setField(authService, "entityManager", entityManager);

            mockQuery = org.mockito.Mockito.mock(Query.class);
            org.mockito.BDDMockito.lenient()
                    .when(entityManager.createQuery(anyString()))
                    .thenReturn(mockQuery);
            org.mockito.BDDMockito.lenient()
                    .when(mockQuery.setParameter(anyString(), any()))
                    .thenReturn(mockQuery);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 AnnualLeave JPQL 삭제 쿼리가 실행되지 않는다")
        void deleteUser_doesNotExecuteAnnualLeaveJpqlQuery() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean annualLeaveQueryFound =
                    executedJpqls.stream().anyMatch(jpql -> jpql.contains("AnnualLeave"));
            assertThat(annualLeaveQueryFound)
                    .as(
                            "AnnualLeave는 CascadeType.ALL + orphanRemoval=true로 매핑되어 있으므로"
                                    + " JPQL로 수동 삭제하면 안 된다")
                    .isFalse();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId로 deleteUser 호출 시 USER_NOT_FOUND 예외가 발생한다")
        void deleteUser_throwsWhenUserNotFound() {
            // given
            Long nonExistentUserId = 999L;
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.deleteUser(nonExistentUserId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(entityManager, never()).createQuery(anyString());
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 userRepository.delete() 전에"
                        + " safetyTrainingSessionRepository.updateCreatedByToNull()이 호출된다")
        void deleteUser_callsUpdateCreatedByToNullBeforeDelete() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);
            verify(safetyTrainingSessionRepository).updateCreatedByToNull(userId);
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 safetyTrainingSessionRepository.updateCreatedByToNull()이"
                        + " userRepository.delete() 보다 먼저 호출된다 - 호출 순서 검증")
        void deleteUser_updateCreatedByToNullCalledBeforeDeleteInOrder() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);
            InOrder inOrder = inOrder(safetyTrainingSessionRepository, userRepository);
            inOrder.verify(safetyTrainingSessionRepository).updateCreatedByToNull(userId);
            inOrder.verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시"
                        + " safetyTrainingSessionRepository.updateInstructorUserToNull()이 호출된다")
        void deleteUser_callsUpdateInstructorUserToNull_success() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            verify(safetyTrainingSessionRepository).updateInstructorUserToNull(userId);
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 safetyTrainingSessionRepository.updateInstructorUserToNull()이"
                        + " userRepository.delete() 보다 먼저 호출된다 - 호출 순서 검증")
        void deleteUser_updateInstructorUserToNullCalledBeforeDeleteInOrder() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            InOrder inOrder = inOrder(safetyTrainingSessionRepository, userRepository);
            inOrder.verify(safetyTrainingSessionRepository).updateInstructorUserToNull(userId);
            inOrder.verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 FcmToken JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteFcmTokenJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean fcmTokenQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("FcmToken")
                                                    && jpql.contains("ft.user.id")
                                                    && jpql.contains(":userId"));
            assertThat(fcmTokenQueryFound)
                    .as("deleteUser() 실행 시 FcmToken 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 FcmToken 삭제가 RefreshToken 삭제 직후에 실행된다 - 호출 순서 검증")
        void deleteUser_deletesFcmTokenAfterRefreshToken_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int refreshTokenIdx = -1;
            int fcmTokenIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("RefreshToken") && refreshTokenIdx == -1) {
                    refreshTokenIdx = i;
                }
                if (jpql.contains("FcmToken") && fcmTokenIdx == -1) {
                    fcmTokenIdx = i;
                }
            }

            assertThat(fcmTokenIdx).as("FcmToken 삭제 JPQL이 실행되어야 한다").isGreaterThanOrEqualTo(0);
            assertThat(refreshTokenIdx)
                    .as("RefreshToken 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(fcmTokenIdx)
                    .as("FcmToken 삭제는 RefreshToken 삭제 직후(= 그 이후)에 실행되어야 한다")
                    .isEqualTo(refreshTokenIdx + 1);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 TopicMember JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteTopicMemberJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean topicMemberQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("TopicMember")
                                                    && jpql.contains("tm.user.id")
                                                    && jpql.contains(":userId"));
            assertThat(topicMemberQueryFound)
                    .as("deleteUser() 실행 시 TopicMember 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 TopicMember 삭제가 FcmToken 삭제 직후에 실행된다 - 호출 순서 검증")
        void deleteUser_deletesTopicMemberAfterFcmToken_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int fcmTokenIdx = -1;
            int topicMemberIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("FcmToken") && fcmTokenIdx == -1) {
                    fcmTokenIdx = i;
                }
                if (jpql.contains("TopicMember") && topicMemberIdx == -1) {
                    topicMemberIdx = i;
                }
            }

            assertThat(fcmTokenIdx).as("FcmToken 삭제 JPQL이 실행되어야 한다").isGreaterThanOrEqualTo(0);
            assertThat(topicMemberIdx)
                    .as("TopicMember 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(topicMemberIdx)
                    .as("TopicMember 삭제는 FcmToken 삭제 직후(= 그 이후)에 실행되어야 한다")
                    .isEqualTo(fcmTokenIdx + 1);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalPersonalViewerTarget JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteApprovalPersonalViewerTargetJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean viewerTargetQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalPersonalViewerTarget")
                                                    && jpql.contains("vt.setting.user.id")
                                                    && jpql.contains(":userId"));
            assertThat(viewerTargetQueryFound)
                    .as("deleteUser() 실행 시 ApprovalPersonalViewerTarget 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalPersonalSetting JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteApprovalPersonalSettingJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean settingQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalPersonalSetting")
                                                    && jpql.contains("aps.user.id")
                                                    && jpql.contains(":userId"));
            assertThat(settingQueryFound)
                    .as("deleteUser() 실행 시 ApprovalPersonalSetting 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 ApprovalPersonalSetting 삭제가 ApprovalPersonalViewerTarget 삭제"
                        + " 직후에 실행된다 - 호출 순서 검증")
        void deleteUser_deletesApprovalPersonalSettingAfterViewerTarget_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int viewerTargetIdx = -1;
            int settingIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("ApprovalPersonalViewerTarget") && viewerTargetIdx == -1) {
                    viewerTargetIdx = i;
                }
                if (jpql.contains("ApprovalPersonalSetting") && settingIdx == -1) {
                    settingIdx = i;
                }
            }

            assertThat(viewerTargetIdx)
                    .as("ApprovalPersonalViewerTarget 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(settingIdx)
                    .as("ApprovalPersonalSetting 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(settingIdx)
                    .as("ApprovalPersonalSetting 삭제는 ApprovalPersonalViewerTarget 삭제 직후에 실행되어야 한다")
                    .isEqualTo(viewerTargetIdx + 1);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 SavedApprovalLineDetail JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteSavedApprovalLineDetailJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean savedLineDetailQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("SavedApprovalLineDetail")
                                                    && jpql.contains("d.savedLine.ownerUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(savedLineDetailQueryFound)
                    .as("deleteUser() 실행 시 SavedApprovalLineDetail 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 SavedApprovalLineDetail 삭제가 ApprovalPersonalSetting 삭제 직후에"
                        + " 실행된다 - 호출 순서 검증")
        void deleteUser_deletesSavedApprovalLineDetailAfterApprovalPersonalSetting_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int settingIdx = -1;
            int savedLineDetailIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("ApprovalPersonalSetting") && settingIdx == -1) {
                    settingIdx = i;
                }
                if (jpql.contains("SavedApprovalLineDetail") && savedLineDetailIdx == -1) {
                    savedLineDetailIdx = i;
                }
            }

            assertThat(settingIdx)
                    .as("ApprovalPersonalSetting 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(savedLineDetailIdx)
                    .as("SavedApprovalLineDetail 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(savedLineDetailIdx)
                    .as("SavedApprovalLineDetail 삭제는 ApprovalPersonalSetting 삭제 직후에 실행되어야 한다")
                    .isEqualTo(settingIdx + 1);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 SavedApprovalLine JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteSavedApprovalLineJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean savedApprovalLineQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("SavedApprovalLine")
                                                    && !jpql.contains("SavedApprovalLineDetail")
                                                    && jpql.contains("sal.ownerUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(savedApprovalLineQueryFound)
                    .as("deleteUser() 실행 시 SavedApprovalLine 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 SavedApprovalLine 삭제가 SavedApprovalLineDetail 삭제 직후에 실행된다 - 호출"
                        + " 순서 검증")
        void deleteUser_deletesSavedApprovalLineAfterSavedApprovalLineDetail_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int savedLineDetailIdx = -1;
            int savedLineIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("SavedApprovalLineDetail") && savedLineDetailIdx == -1) {
                    savedLineDetailIdx = i;
                }
                if (jpql.contains("SavedApprovalLine")
                        && !jpql.contains("SavedApprovalLineDetail")
                        && savedLineIdx == -1) {
                    savedLineIdx = i;
                }
            }

            assertThat(savedLineDetailIdx)
                    .as("SavedApprovalLineDetail 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(savedLineIdx)
                    .as("SavedApprovalLine 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(savedLineIdx)
                    .as("SavedApprovalLine 삭제는 SavedApprovalLineDetail 삭제 직후(= 그 이후)에 실행되어야 한다")
                    .isEqualTo(savedLineDetailIdx + 1);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalActionHistory JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteApprovalActionHistoryJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean actionHistoryQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalActionHistory")
                                                    && jpql.contains("ah.document.drafterUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(actionHistoryQueryFound)
                    .as("deleteUser() 실행 시 ApprovalActionHistory 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalDocumentLine JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteApprovalDocumentLineJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean documentLineQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalDocumentLine")
                                                    && jpql.contains("dl.document.drafterUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(documentLineQueryFound)
                    .as("deleteUser() 실행 시 ApprovalDocumentLine 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalDocumentRead JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteApprovalDocumentReadJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean documentReadQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalDocumentRead")
                                                    && jpql.contains("dr.document.drafterUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(documentReadQueryFound)
                    .as("deleteUser() 실행 시 ApprovalDocumentRead 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalAttachment JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteApprovalAttachmentJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean attachmentQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalAttachment")
                                                    && jpql.contains("aa.document.drafterUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(attachmentQueryFound)
                    .as("deleteUser() 실행 시 ApprovalAttachment 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 ApprovalDocument 자식 4개 삭제가 SavedApprovalLine 삭제 직후에 실행된다 - 호출"
                        + " 순서 검증")
        void deleteUser_deletesApprovalDocumentChildrenAfterSavedApprovalLine_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int savedApprovalLineIdx = -1;
            int actionHistoryIdx = -1;
            int documentLineIdx = -1;
            int documentReadIdx = -1;
            int attachmentIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("SavedApprovalLine")
                        && !jpql.contains("SavedApprovalLineDetail")
                        && savedApprovalLineIdx == -1) {
                    savedApprovalLineIdx = i;
                }
                if (jpql.contains("ApprovalActionHistory") && actionHistoryIdx == -1) {
                    actionHistoryIdx = i;
                }
                if (jpql.contains("ApprovalDocumentLine") && documentLineIdx == -1) {
                    documentLineIdx = i;
                }
                if (jpql.contains("ApprovalDocumentRead") && documentReadIdx == -1) {
                    documentReadIdx = i;
                }
                if (jpql.contains("ApprovalAttachment") && attachmentIdx == -1) {
                    attachmentIdx = i;
                }
            }

            assertThat(savedApprovalLineIdx)
                    .as("SavedApprovalLine 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(actionHistoryIdx)
                    .as("ApprovalActionHistory 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(documentLineIdx)
                    .as("ApprovalDocumentLine 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(documentReadIdx)
                    .as("ApprovalDocumentRead 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(attachmentIdx)
                    .as("ApprovalAttachment 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);

            assertThat(actionHistoryIdx)
                    .as("ApprovalActionHistory 삭제는 SavedApprovalLine 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(savedApprovalLineIdx);
            assertThat(documentLineIdx)
                    .as("ApprovalDocumentLine 삭제는 SavedApprovalLine 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(savedApprovalLineIdx);
            assertThat(documentReadIdx)
                    .as("ApprovalDocumentRead 삭제는 SavedApprovalLine 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(savedApprovalLineIdx);
            assertThat(attachmentIdx)
                    .as("ApprovalAttachment 삭제는 SavedApprovalLine 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(savedApprovalLineIdx);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalDocument JPQL 삭제 쿼리가 실행된다")
        void deleteUser_executesDeleteApprovalDocumentJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean approvalDocumentQueryFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalDocument")
                                                    && !jpql.contains("ApprovalDocumentLine")
                                                    && !jpql.contains("ApprovalDocumentRead")
                                                    && jpql.contains("ad.drafterUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(approvalDocumentQueryFound)
                    .as("deleteUser() 실행 시 ApprovalDocument 삭제 JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 ApprovalDocument 삭제가 자식 4종(ApprovalActionHistory,"
                        + " ApprovalDocumentLine, ApprovalDocumentRead, ApprovalAttachment) 삭제 직후에"
                        + " 실행된다 - 호출 순서 검증")
        void deleteUser_deletesApprovalDocumentAfterChildren_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int actionHistoryIdx = -1;
            int documentLineIdx = -1;
            int documentReadIdx = -1;
            int attachmentIdx = -1;
            int approvalDocumentIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("ApprovalActionHistory") && actionHistoryIdx == -1) {
                    actionHistoryIdx = i;
                }
                if (jpql.contains("ApprovalDocumentLine") && documentLineIdx == -1) {
                    documentLineIdx = i;
                }
                if (jpql.contains("ApprovalDocumentRead") && documentReadIdx == -1) {
                    documentReadIdx = i;
                }
                if (jpql.contains("ApprovalAttachment") && attachmentIdx == -1) {
                    attachmentIdx = i;
                }
                if (jpql.contains("ApprovalDocument")
                        && !jpql.contains("ApprovalDocumentLine")
                        && !jpql.contains("ApprovalDocumentRead")
                        && approvalDocumentIdx == -1) {
                    approvalDocumentIdx = i;
                }
            }

            assertThat(approvalDocumentIdx)
                    .as("ApprovalDocument 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(actionHistoryIdx)
                    .as("ApprovalActionHistory 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(documentLineIdx)
                    .as("ApprovalDocumentLine 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(documentReadIdx)
                    .as("ApprovalDocumentRead 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(attachmentIdx)
                    .as("ApprovalAttachment 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);

            assertThat(approvalDocumentIdx)
                    .as("ApprovalDocument 삭제는 ApprovalActionHistory 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(actionHistoryIdx);
            assertThat(approvalDocumentIdx)
                    .as("ApprovalDocument 삭제는 ApprovalDocumentLine 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(documentLineIdx);
            assertThat(approvalDocumentIdx)
                    .as("ApprovalDocument 삭제는 ApprovalDocumentRead 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(documentReadIdx);
            assertThat(approvalDocumentIdx)
                    .as("ApprovalDocument 삭제는 ApprovalAttachment 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(attachmentIdx);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalDocumentLine.targetUser NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyApprovalDocumentLineTargetUserJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalDocumentLine")
                                                    && jpql.contains("dl.targetUser")
                                                    && jpql.contains("dl.targetUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 ApprovalDocumentLine.targetUser NULLIFY UPDATE JPQL이"
                                    + " 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 ApprovalDocumentLine.processedByUser NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyApprovalDocumentLineProcessedByUserJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalDocumentLine")
                                                    && jpql.contains("dl.processedByUser")
                                                    && jpql.contains("dl.processedByUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 ApprovalDocumentLine.processedByUser NULLIFY UPDATE"
                                    + " JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalDocumentRead.targetUser NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyApprovalDocumentReadTargetUserJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalDocumentRead")
                                                    && jpql.contains("dr.targetUser")
                                                    && jpql.contains("dr.targetUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 ApprovalDocumentRead.targetUser NULLIFY UPDATE JPQL이"
                                    + " 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalActionHistory.actorUser NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyApprovalActionHistoryActorUserJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalActionHistory")
                                                    && jpql.contains("ah.actorUser")
                                                    && jpql.contains("ah.actorUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 ApprovalActionHistory.actorUser NULLIFY UPDATE JPQL이"
                                    + " 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 ApprovalAttachment.uploadedByUser NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyApprovalAttachmentUploadedByUserJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalAttachment")
                                                    && jpql.contains("aa.uploadedByUser")
                                                    && jpql.contains("aa.uploadedByUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 ApprovalAttachment.uploadedByUser NULLIFY UPDATE"
                                    + " JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 5개의 NULLIFY UPDATE 쿼리가 모두 ApprovalDocument 삭제 이후에 실행된다 - 호출 순서"
                        + " 검증")
        void deleteUser_nullifyQueriesExecutedAfterApprovalDocumentDelete_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int approvalDocumentDeleteIdx = -1;
            int nullifyDocumentLineTargetUserIdx = -1;
            int nullifyDocumentLineProcessedByUserIdx = -1;
            int nullifyDocumentReadTargetUserIdx = -1;
            int nullifyActionHistoryActorUserIdx = -1;
            int nullifyAttachmentUploadedByUserIdx = -1;

            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("ApprovalDocument")
                        && !jpql.contains("ApprovalDocumentLine")
                        && !jpql.contains("ApprovalDocumentRead")
                        && jpql.contains("ad.drafterUser.id")
                        && approvalDocumentDeleteIdx == -1) {
                    approvalDocumentDeleteIdx = i;
                }
                if (jpql.contains("ApprovalDocumentLine")
                        && jpql.contains("dl.targetUser")
                        && nullifyDocumentLineTargetUserIdx == -1) {
                    nullifyDocumentLineTargetUserIdx = i;
                }
                if (jpql.contains("ApprovalDocumentLine")
                        && jpql.contains("dl.processedByUser")
                        && nullifyDocumentLineProcessedByUserIdx == -1) {
                    nullifyDocumentLineProcessedByUserIdx = i;
                }
                if (jpql.contains("ApprovalDocumentRead")
                        && jpql.contains("dr.targetUser")
                        && nullifyDocumentReadTargetUserIdx == -1) {
                    nullifyDocumentReadTargetUserIdx = i;
                }
                if (jpql.contains("ApprovalActionHistory")
                        && jpql.contains("ah.actorUser")
                        && nullifyActionHistoryActorUserIdx == -1) {
                    nullifyActionHistoryActorUserIdx = i;
                }
                if (jpql.contains("ApprovalAttachment")
                        && jpql.contains("aa.uploadedByUser")
                        && nullifyAttachmentUploadedByUserIdx == -1) {
                    nullifyAttachmentUploadedByUserIdx = i;
                }
            }

            assertThat(approvalDocumentDeleteIdx)
                    .as("ApprovalDocument 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(nullifyDocumentLineTargetUserIdx)
                    .as("ApprovalDocumentLine.targetUser NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThan(approvalDocumentDeleteIdx);
            assertThat(nullifyDocumentLineProcessedByUserIdx)
                    .as("ApprovalDocumentLine.processedByUser NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThan(approvalDocumentDeleteIdx);
            assertThat(nullifyDocumentReadTargetUserIdx)
                    .as("ApprovalDocumentRead.targetUser NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThan(approvalDocumentDeleteIdx);
            assertThat(nullifyActionHistoryActorUserIdx)
                    .as("ApprovalActionHistory.actorUser NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThan(approvalDocumentDeleteIdx);
            assertThat(nullifyAttachmentUploadedByUserIdx)
                    .as("ApprovalAttachment.uploadedByUser NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThan(approvalDocumentDeleteIdx);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalTemplate.createdBy NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyApprovalTemplateCreatedByJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalTemplate")
                                                    && !jpql.contains("ApprovalTemplateLine")
                                                    && jpql.contains("at.createdBy")
                                                    && jpql.contains("at.createdBy.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 ApprovalTemplate.createdBy NULLIFY UPDATE JPQL이"
                                    + " 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 ApprovalTemplateLine.targetUser NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyApprovalTemplateLineTargetUserJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("ApprovalTemplateLine")
                                                    && jpql.contains("atl.targetUser")
                                                    && jpql.contains("atl.targetUser.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 ApprovalTemplateLine.targetUser NULLIFY UPDATE JPQL이"
                                    + " 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 ApprovalTemplate/ApprovalTemplateLine NULLIFY 쿼리 2개가"
                        + " SafetyTrainingSessionAttendee 삭제 이후에 실행된다 - 호출 순서 검증")
        void deleteUser_nullifyApprovalTemplateQueriesAfterAttendeeDelete_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int attendeeDeleteIdx = -1;
            int nullifyTemplateCreatedByIdx = -1;
            int nullifyTemplateLineTargetUserIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("SafetyTrainingSessionAttendee") && attendeeDeleteIdx == -1) {
                    attendeeDeleteIdx = i;
                }
                if (jpql.contains("ApprovalTemplate")
                        && !jpql.contains("ApprovalTemplateLine")
                        && jpql.contains("at.createdBy")
                        && nullifyTemplateCreatedByIdx == -1) {
                    nullifyTemplateCreatedByIdx = i;
                }
                if (jpql.contains("ApprovalTemplateLine")
                        && jpql.contains("atl.targetUser")
                        && nullifyTemplateLineTargetUserIdx == -1) {
                    nullifyTemplateLineTargetUserIdx = i;
                }
            }

            assertThat(attendeeDeleteIdx)
                    .as("SafetyTrainingSessionAttendee 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(nullifyTemplateCreatedByIdx)
                    .as("ApprovalTemplate.createdBy NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThan(attendeeDeleteIdx);
            assertThat(nullifyTemplateLineTargetUserIdx)
                    .as("ApprovalTemplateLine.targetUser NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThan(attendeeDeleteIdx);
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 ApprovalPersonalViewerTarget 삭제가 Notice 삭제 이후에 실행된다 - 호출 순서"
                        + " 검증")
        void deleteUser_deletesApprovalPersonalViewerTargetAfterNotice_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int lastNoticeIdx = -1;
            int viewerTargetIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("Notice")) {
                    lastNoticeIdx = i;
                }
                if (jpql.contains("ApprovalPersonalViewerTarget") && viewerTargetIdx == -1) {
                    viewerTargetIdx = i;
                }
            }

            assertThat(lastNoticeIdx).as("Notice 관련 삭제 JPQL이 실행되어야 한다").isGreaterThanOrEqualTo(0);
            assertThat(viewerTargetIdx)
                    .as("ApprovalPersonalViewerTarget 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(viewerTargetIdx)
                    .as("ApprovalPersonalViewerTarget 삭제는 Notice 관련 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(lastNoticeIdx);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 EduReport.createdBy NULLIFY JPQL 쿼리가 실행된다")
        void deleteUser_executesNullifyEduReportCreatedByJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean eduReportNullifyFound =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("EduReport")
                                                    && jpql.contains("er.createdBy")
                                                    && jpql.contains(":userId"));
            assertThat(eduReportNullifyFound)
                    .as("deleteUser() 실행 시 EduReport.createdBy NULLIFY JPQL이 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 EduReport.createdBy NULLIFY가 EduAttendance 삭제 이후에 실행된다 - 호출 순서"
                        + " 검증")
        void deleteUser_nullifiesEduReportCreatedByAfterEduAttendanceDelete_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int eduAttendanceIdx = -1;
            int eduReportNullifyIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("EduAttendance") && eduAttendanceIdx == -1) {
                    eduAttendanceIdx = i;
                }
                if (jpql.contains("EduReport")
                        && jpql.contains("er.createdBy")
                        && eduReportNullifyIdx == -1) {
                    eduReportNullifyIdx = i;
                }
            }

            assertThat(eduAttendanceIdx)
                    .as("EduAttendance 삭제 JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(eduReportNullifyIdx)
                    .as("EduReport.createdBy NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(eduReportNullifyIdx)
                    .as("EduReport.createdBy NULLIFY는 EduAttendance 삭제 이후에 실행되어야 한다")
                    .isGreaterThan(eduAttendanceIdx);
        }

        @Test
        @DisplayName("성공: deleteUser 실행 시 RequestHistory.processedBy NULLIFY UPDATE 쿼리가 실행된다")
        void deleteUser_executesNullifyRequestHistoryProcessedByJpql() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();
            boolean found =
                    executedJpqls.stream()
                            .anyMatch(
                                    jpql ->
                                            jpql.contains("RequestHistory")
                                                    && jpql.contains("rh.processedBy")
                                                    && jpql.contains("rh.processedBy.id")
                                                    && jpql.contains(":userId"));
            assertThat(found)
                    .as(
                            "deleteUser() 실행 시 RequestHistory.processedBy NULLIFY UPDATE JPQL이"
                                    + " 포함되어야 한다")
                    .isTrue();
        }

        @Test
        @DisplayName(
                "성공: deleteUser 실행 시 RequestHistory.processedBy NULLIFY가"
                        + " RequestHistory 삭제(rh.user.id) 직전에 실행된다 - 호출 순서 검증")
        void deleteUser_nullifyRequestHistoryProcessedByBeforeRequestHistoryDelete_orderCheck() {
            // given
            Long userId = 1L;
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));

            // when
            authService.deleteUser(userId);

            // then
            ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(entityManager, org.mockito.Mockito.atLeastOnce())
                    .createQuery(jpqlCaptor.capture());

            List<String> executedJpqls = jpqlCaptor.getAllValues();

            int nullifyProcessedByIdx = -1;
            int deleteRequestHistoryIdx = -1;
            for (int i = 0; i < executedJpqls.size(); i++) {
                String jpql = executedJpqls.get(i);
                if (jpql.contains("RequestHistory")
                        && jpql.contains("rh.processedBy")
                        && nullifyProcessedByIdx == -1) {
                    nullifyProcessedByIdx = i;
                }
                if (jpql.contains("RequestHistory")
                        && jpql.contains("rh.user.id")
                        && deleteRequestHistoryIdx == -1) {
                    deleteRequestHistoryIdx = i;
                }
            }

            assertThat(nullifyProcessedByIdx)
                    .as("RequestHistory.processedBy NULLIFY JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(deleteRequestHistoryIdx)
                    .as("RequestHistory 삭제(rh.user.id) JPQL이 실행되어야 한다")
                    .isGreaterThanOrEqualTo(0);
            assertThat(nullifyProcessedByIdx)
                    .as(
                            "RequestHistory.processedBy NULLIFY는 RequestHistory 삭제(rh.user.id) 직전에"
                                    + " 실행되어야 한다")
                    .isEqualTo(deleteRequestHistoryIdx - 1);
        }
    }
}
