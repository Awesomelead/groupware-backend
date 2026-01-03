package kr.co.awesomelead.groupware_backend.domain.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByEmailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordByPhoneRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.ResetPasswordRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.SignupRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.service.AuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.EmailAuthService;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.mapper.UserMapper;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private PhoneAuthService phoneAuthService;

    @Mock
    private EmailAuthService emailAuthService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

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
        signupDto.setRegistrationNumber("950101-1234567");
        signupDto.setPhoneNumber("01012345678");
        signupDto.setCompany(Company.AWESOME);

        // Builder 사용 + onPrePersist 호출
        User mockUser =
            User.builder()
                .email(signupDto.getEmail())
                .nameKor(signupDto.getNameKor())
                .nameEng(signupDto.getNameEng())
                .nationality(signupDto.getNationality())
                .registrationNumber(signupDto.getRegistrationNumber())
                .phoneNumber(signupDto.getPhoneNumber())
                .workLocation(Company.AWESOME)
                .role(Role.USER)
                .status(Status.PENDING)
                .build();

        // @PrePersist 로직 수동 실행
        mockUser.onPrePersist();

        // Mock 설정
        when(emailAuthService.isEmailVerified(signupDto.getEmail())).thenReturn(true);
        when(phoneAuthService.isPhoneVerified(signupDto.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByEmail(signupDto.getEmail())).thenReturn(false);
        when(userRepository.existsByRegistrationNumber(signupDto.getRegistrationNumber()))
            .thenReturn(false);
        when(userMapper.toEntity(signupDto)).thenReturn(mockUser);
        when(bCryptPasswordEncoder.encode(signupDto.getPassword())).thenReturn("encodedPassword");

        // when
        authService.signup(signupDto);

        // then
        verify(userRepository, times(1)).save(any(User.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getNameKor()).isEqualTo(signupDto.getNameKor());
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getStatus()).isEqualTo(Status.PENDING);

        assertThat(savedUser.getBirthDate()).isNotNull();
        assertThat(savedUser.getBirthDate()).isEqualTo(LocalDate.of(1995, 1, 1));

        // 이메일 & 휴대폰 인증 플래그 삭제 검증
        verify(emailAuthService, times(1)).clearVerification(signupDto.getEmail());
        verify(phoneAuthService, times(1)).clearVerification(signupDto.getPhoneNumber());
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
    @DisplayName("휴대폰 인증 후 비밀번호 재설정")
    class ResetPasswordByPhoneTest {

        private ResetPasswordByPhoneRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = new ResetPasswordByPhoneRequestDto();
            requestDto.setPhoneNumber(TEST_PHONE);
            requestDto.setNewPassword(NEW_PASSWORD);
            requestDto.setNewPasswordConfirm(NEW_PASSWORD);
        }

        @Test
        @DisplayName("성공: 휴대폰 인증 후 비밀번호가 정상적으로 재설정된다")
        void resetPasswordByPhone_Success() {
            // given
            String phoneHash = User.hashPhoneNumber(TEST_PHONE);
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(true);
            given(userRepository.findByPhoneNumberHash(phoneHash)).willReturn(
                Optional.of(testUser));
            given(bCryptPasswordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_NEW_PASSWORD);

            // when
            authService.resetPasswordByPhone(requestDto);

            // then
            verify(phoneAuthService).isPhoneVerified(TEST_PHONE);
            verify(userRepository).findByPhoneNumberHash(phoneHash);
            verify(bCryptPasswordEncoder).encode(NEW_PASSWORD);
            verify(userRepository).save(testUser);
            verify(phoneAuthService).clearVerification(TEST_PHONE);
            assertThat(testUser.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
        }

        @Test
        @DisplayName("실패: 휴대폰 인증을 하지 않은 경우")
        void resetPasswordByPhone_NotVerified() {
            // given
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByPhone(requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PHONE_NOT_VERIFIED);

            verify(phoneAuthService).isPhoneVerified(TEST_PHONE);
            verify(userRepository, never()).findByPhoneNumberHash(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 비밀번호 확인이 일치하지 않는 경우")
        void resetPasswordByPhone_PasswordMismatch() {
            // given
            requestDto.setNewPasswordConfirm("differentPassword!@#");
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByPhone(requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_MISMATCH);

            verify(userRepository, never()).findByPhoneNumberHash(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 전화번호인 경우")
        void resetPasswordByPhone_UserNotFound() {
            // given
            String phoneHash = User.hashPhoneNumber(TEST_PHONE);
            given(phoneAuthService.isPhoneVerified(TEST_PHONE)).willReturn(true);
            given(userRepository.findByPhoneNumberHash(phoneHash)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.resetPasswordByPhone(requestDto))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

            verify(userRepository).findByPhoneNumberHash(phoneHash);
            verify(userRepository, never()).save(any());
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
            given(bCryptPasswordEncoder.matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD)).willReturn(
                true);
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
            given(bCryptPasswordEncoder.matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD)).willReturn(
                false);

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
            given(bCryptPasswordEncoder.matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD)).willReturn(
                true);

            // when & then
            assertThatThrownBy(() -> authService.resetPassword(requestDto, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SAME_AS_CURRENT_PASSWORD);

            verify(bCryptPasswordEncoder).matches(OLD_PASSWORD, ENCODED_OLD_PASSWORD);
            verify(userRepository, never()).save(any());
        }
    }
}
