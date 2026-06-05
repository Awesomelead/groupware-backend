package kr.co.awesomelead.groupware_backend.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import kr.co.awesomelead.groupware_backend.domain.auth.service.EmailAuthService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailAuthService 클래스의")
class EmailAuthServiceTest {

    @InjectMocks private EmailAuthService emailAuthService;
    @Mock private JavaMailSender mailSender;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("sendAuthCode 메서드는 인증번호 메일을 발송하고 Redis에 인증번호를 저장한다")
    void sendAuthCode_sends_email_and_stores_auth_code() throws Exception {
        // given
        String email = "user@example.com";
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        ReflectionTestUtils.setField(emailAuthService, "fromEmail", "noreply@example.com");
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when & then
        assertThatCode(() -> emailAuthService.sendAuthCode(email)).doesNotThrowAnyException();

        assertThat(mimeMessage.getSubject()).isEqualTo("[어썸그룹] 이메일 인증번호 안내");
        verify(mailSender).send(any(MimeMessage.class));
        verify(valueOperations)
                .set(eq("auth:email:" + email), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }
}
