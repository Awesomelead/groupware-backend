package kr.co.awesomelead.groupware_backend.domain.auth;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock private PhoneAuthService phoneAuthService;

    @Mock private EmailAuthService emailAuthService;

    @Mock private UserMapper userMapper;

    @InjectMocks private AuthService authService;

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
}
