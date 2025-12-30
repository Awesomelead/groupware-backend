package kr.co.awesomelead.groupware_backend.domain.auth;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.dto.request.JoinRequestDto;
import kr.co.awesomelead.groupware_backend.domain.auth.service.EmailAuthService;
import kr.co.awesomelead.groupware_backend.domain.auth.service.JoinService;
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

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JoinServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock private PhoneAuthService phoneAuthService;
    @Mock private EmailAuthService emailAuthService;
    @Mock private UserMapper userMapper;

    @InjectMocks private JoinService joinService;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void joinProcess_Success() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("password123!");
        joinDto.setPasswordConfirm("password123!");
        joinDto.setNameKor("김어썸");
        joinDto.setNameEng("Awesome Kim");
        joinDto.setNationality("대한민국");
        joinDto.setRegistrationNumber("950101-1234567");
        joinDto.setPhoneNumber("01012345678");
        joinDto.setCompany(Company.AWESOME);

        // Mapper가 반환할 User 객체 생성
        User mockUser = new User();
        mockUser.setEmail(joinDto.getEmail());
        mockUser.setNameKor(joinDto.getNameKor());
        mockUser.setNameEng(joinDto.getNameEng());
        mockUser.setNationality(joinDto.getNationality());
        mockUser.setRegistrationNumber(joinDto.getRegistrationNumber());
        mockUser.setPhoneNumber(joinDto.getPhoneNumber());
        mockUser.setWorkLocation(Company.AWESOME);
        mockUser.setRole(Role.USER);
        mockUser.setStatus(Status.PENDING);

        // Mock 설정
        when(emailAuthService.isEmailVerified(joinDto.getEmail())).thenReturn(true);
        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByEmail(joinDto.getEmail())).thenReturn(false);
        when(userRepository.existsByRegistrationNumber(joinDto.getRegistrationNumber()))
                .thenReturn(false);
        when(userMapper.toEntity(joinDto)).thenReturn(mockUser); // Mapper Mock
        when(bCryptPasswordEncoder.encode(joinDto.getPassword())).thenReturn("encodedPassword");

        // when
        joinService.joinProcess(joinDto);

        // then
        verify(userRepository, times(1)).save(any(User.class));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getNameKor()).isEqualTo(joinDto.getNameKor());
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getStatus()).isEqualTo(Status.PENDING);

        // 이메일 & 휴대폰 인증 플래그 삭제 검증
        verify(emailAuthService, times(1)).clearVerification(joinDto.getEmail());
        verify(phoneAuthService, times(1)).clearVerification(joinDto.getPhoneNumber());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 중복")
    void joinProcess_Fail_DuplicateEmail() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("password123!");
        joinDto.setPasswordConfirm("password123!");
        joinDto.setPhoneNumber("01012345678");

        // 이메일 & 휴대폰 인증은 통과
        when(emailAuthService.isEmailVerified(joinDto.getEmail())).thenReturn(true);
        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())).thenReturn(true);

        // 이메일 중복
        when(userRepository.existsByEmail(joinDto.getEmail())).thenReturn(true);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 비밀번호 불일치")
    void joinProcess_Fail_PasswordMismatch() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("Password123!");
        joinDto.setPasswordConfirm("DifferentPassword123!");

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_MISMATCH);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이메일 미인증")
    void joinProcess_Fail_EmailNotVerified() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("Password123!");
        joinDto.setPasswordConfirm("Password123!");
        joinDto.setPhoneNumber("01012345678");

        // 이메일 미인증
        when(emailAuthService.isEmailVerified(joinDto.getEmail())).thenReturn(false);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 전화번호 미인증")
    void joinProcess_Fail_PhoneNotVerified() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("Password123!");
        joinDto.setPasswordConfirm("Password123!");
        joinDto.setPhoneNumber("01012345678");

        // 이메일 인증은 통과
        when(emailAuthService.isEmailVerified(joinDto.getEmail())).thenReturn(true);

        // 전화번호 미인증
        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())).thenReturn(false);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PHONE_NOT_VERIFIED);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 주민등록번호 중복")
    void joinProcess_Fail_DuplicateRegistrationNumber() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("Password123!");
        joinDto.setPasswordConfirm("Password123!");
        joinDto.setPhoneNumber("01012345678");
        joinDto.setRegistrationNumber("950101-1234567");

        // 이메일 & 휴대폰 인증 통과
        when(emailAuthService.isEmailVerified(joinDto.getEmail())).thenReturn(true);
        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())).thenReturn(true);

        when(userRepository.existsByEmail(joinDto.getEmail())).thenReturn(false);

        // 주민번호 중복
        when(userRepository.existsByRegistrationNumber(joinDto.getRegistrationNumber()))
                .thenReturn(true);

        // when & then
        CustomException exception =
                assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
        verify(userRepository, never()).save(any(User.class));
    }
}
