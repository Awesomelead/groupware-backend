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
import kr.co.awesomelead.groupware_backend.domain.auth.service.JoinService;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private PhoneAuthService phoneAuthService;

    @InjectMocks
    private JoinService joinService;

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

        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByEmail(joinDto.getEmail())).thenReturn(false);
        when(userRepository.existsByRegistrationNumber(joinDto.getRegistrationNumber()))
            .thenReturn(false);
        when(bCryptPasswordEncoder.encode(joinDto.getPassword())).thenReturn("encodedPassword");

        // when
        joinService.joinProcess(joinDto);

        // then
        // userRepository.save() 메서드가 정확히 1번 호출되었는지 검증
        verify(userRepository, times(1)).save(any(User.class));

        // ArgumentCaptor를 사용해 save 메서드에 전달된 User 객체를 캡처
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // 캡처된 User 객체의 필드가 예상대로 설정되었는지 검증
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword"); // 암호화된 비밀번호 확인
        assertThat(savedUser.getNameKor()).isEqualTo(joinDto.getNameKor());
        assertThat(savedUser.getRole()).isEqualTo(Role.USER); // 기본값 확인
        assertThat(savedUser.getStatus()).isEqualTo(Status.PENDING); // 기본값 확인

        verify(phoneAuthService, times(1)).clearVerification(joinDto.getPhoneNumber());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 아이디 중복")
    void joinProcess_Fail_DuplicateLoginId() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("password123!");
        joinDto.setPasswordConfirm("password123!");
        joinDto.setPhoneNumber("01012345678");

        // userRepository.existsByLoginId가 true를 반환하도록 설정 (아이디 중복 있음)
        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())).thenReturn(true);
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
        joinDto.setPasswordConfirm("DifferentPassword123!"); // 다른 비밀번호

        // when & then
        CustomException exception =
            assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_MISMATCH);
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

        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber()))
            .thenReturn(false); // 인증 안 됨

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

        when(phoneAuthService.isPhoneVerified(joinDto.getPhoneNumber())).thenReturn(true);
        when(userRepository.existsByEmail(joinDto.getEmail())).thenReturn(false);
        when(userRepository.existsByRegistrationNumber(joinDto.getRegistrationNumber()))
            .thenReturn(true); // 주민번호 중복

        // when & then
        CustomException exception =
            assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REGISTRATION_NUMBER);
        verify(userRepository, never()).save(any(User.class));
    }
}
