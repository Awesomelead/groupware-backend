package kr.co.awesomelead.groupware_backend.domain.aligo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.co.awesomelead.groupware_backend.domain.aligo.enums.PhoneAuthChannel;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.AligoKakaoService;
import kr.co.awesomelead.groupware_backend.domain.aligo.service.PhoneAuthService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class PhoneAuthServiceTest {

    @Mock private AligoKakaoService aligoKakaoService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @InjectMocks private PhoneAuthService phoneAuthService;

    private static final String PHONE_NUMBER = "01012345678";

    @Test
    void sendAuthCode_usesKakaoChannelByDefault() {
        when(aligoKakaoService.sendAuthCodeAlimtalk(eq(PHONE_NUMBER), anyString()))
                .thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        phoneAuthService.sendAuthCode(PHONE_NUMBER);

        verify(aligoKakaoService).sendAuthCodeAlimtalk(eq(PHONE_NUMBER), anyString());
        verify(valueOperations)
                .set(
                        eq("auth:phone:" + PHONE_NUMBER),
                        anyString(),
                        eq(5L),
                        eq(TimeUnit.MINUTES));
    }

    @Test
    void sendAuthCode_usesSmsChannel() {
        when(aligoKakaoService.sendAuthCodeSms(eq(PHONE_NUMBER), anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        phoneAuthService.sendAuthCode(PHONE_NUMBER, PhoneAuthChannel.SMS);

        verify(aligoKakaoService).sendAuthCodeSms(eq(PHONE_NUMBER), anyString());
        verify(valueOperations)
                .set(
                        eq("auth:phone:" + PHONE_NUMBER),
                        anyString(),
                        eq(5L),
                        eq(TimeUnit.MINUTES));
    }
}
