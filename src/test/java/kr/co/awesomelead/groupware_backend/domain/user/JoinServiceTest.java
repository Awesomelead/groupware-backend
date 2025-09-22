package kr.co.awesomelead.groupware_backend.domain.user;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.user.dto.JoinRequestDto;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.user.service.JoinService;
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

@ExtendWith(MockitoExtension.class) // Mockito 확장 기능을 JUnit 5와 통합
@ActiveProfiles("test")
class JoinServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private JoinService joinService;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void joinProcess_Success() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("test@example.com");
        joinDto.setPassword("password123!");
        joinDto.setNameKor("김어썸");
        joinDto.setNameEng("Awesome Kim");
        joinDto.setNationality("대한민국");
        joinDto.setRegistrationNumber("950101-1234567");
        joinDto.setPhoneNumber("01012345678");

        when(userRepository.existsByEmail(joinDto.getEmail())).thenReturn(false);
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
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_USER); // 기본값 확인
        assertThat(savedUser.getStatus()).isEqualTo(Status.PENDING); // 기본값 확인
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 아이디 중복")
    void joinProcess_Fail_DuplicateLoginId() {
        // given
        JoinRequestDto joinDto = new JoinRequestDto();
        joinDto.setEmail("testEmail@example.com");
        joinDto.setPassword("password123!");

        // userRepository.existsByLoginId가 true를 반환하도록 설정 (아이디 중복 있음)
        when(userRepository.existsByEmail(joinDto.getEmail())).thenReturn(true);

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> joinService.joinProcess(joinDto));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID);
        verify(userRepository, never()).save(any(User.class));
    }
}
