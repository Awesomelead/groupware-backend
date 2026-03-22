package kr.co.awesomelead.groupware_backend.domain.fcm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import java.util.List;
import java.util.Map;
import kr.co.awesomelead.groupware_backend.domain.fcm.entity.FcmToken;
import kr.co.awesomelead.groupware_backend.domain.fcm.repository.FcmTokenRepository;
import kr.co.awesomelead.groupware_backend.domain.fcm.service.FcmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(FcmServiceTest.TestConfig.class)
class FcmServiceTest {

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        public FcmTokenRepository fcmTokenRepository() {
            return mock(FcmTokenRepository.class);
        }

        @Bean
        public FcmService fcmService(FcmTokenRepository fcmTokenRepository) {
            return new FcmService(fcmTokenRepository);
        }
    }

    @Autowired
    private FcmService fcmService;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @BeforeEach
    void setUp() {
        reset(fcmTokenRepository);
    }

    // ─── sendToTopic ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendToTopic - 첫 번째 시도 성공 시 재시도 없음")
    void sendToTopic_noRetry_whenSuccess() throws FirebaseMessagingException {
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        when(mockMessaging.send(any())).thenReturn("msg-id-ok");

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            staticMock.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            fcmService.sendToTopic("all-users", "제목", "내용");

            verify(mockMessaging, times(1)).send(any());
        }
    }

    @Test
    @DisplayName("sendToTopic - FirebaseMessagingException 발생 시 최대 3회 재시도 후 @Recover 호출")
    void sendToTopic_retriesThreeTimes_thenRecovers() throws FirebaseMessagingException {
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException mockException = mock(FirebaseMessagingException.class);
        when(mockMessaging.send(any())).thenThrow(mockException);

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            staticMock.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            // @Recover가 예외를 흡수하므로 외부로 전파되지 않음
            fcmService.sendToTopic("all-users", "제목", "내용");

            verify(mockMessaging, times(3)).send(any());
        }
    }

    @Test
    @DisplayName("sendToTopic - 두 번째 시도에서 성공 시 1번만 재시도")
    void sendToTopic_succeedsOnSecondAttempt() throws FirebaseMessagingException {
        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException mockException = mock(FirebaseMessagingException.class);
        when(mockMessaging.send(any())).thenThrow(mockException).thenReturn("msg-id-ok");

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            staticMock.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            fcmService.sendToTopic("all-users", "제목", "내용");

            verify(mockMessaging, times(2)).send(any());
        }
    }

    // ─── sendToUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("sendToUser - 등록된 FCM 토큰이 없으면 Firebase 호출 없음")
    void sendToUser_skips_whenNoTokens() throws FirebaseMessagingException {
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of());

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            fcmService.sendToUser(1L, "제목", "내용", Map.of());

            staticMock.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("sendToUser - FirebaseMessagingException 발생 시 최대 3회 재시도 후 @Recover 호출")
    void sendToUser_retriesThreeTimes_thenRecovers() throws FirebaseMessagingException {
        FcmToken mockToken = mock(FcmToken.class);
        when(mockToken.getToken()).thenReturn("device-token-abc");
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of(mockToken));

        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        FirebaseMessagingException mockException = mock(FirebaseMessagingException.class);
        when(mockMessaging.sendEachForMulticast(any())).thenThrow(mockException);

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            staticMock.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            fcmService.sendToUser(1L, "제목", "내용", Map.of("domainType", "APPROVAL"));

            verify(mockMessaging, times(3)).sendEachForMulticast(any());
        }
    }

    @Test
    @DisplayName("sendToUser - 발송 결과에 UNREGISTERED 토큰이 있으면 해당 토큰만 삭제")
    void sendToUser_deletesToken_whenUnregistered() throws FirebaseMessagingException {
        String tokenValue = "device-token-abc";
        FcmToken mockToken = mock(FcmToken.class);
        when(mockToken.getToken()).thenReturn(tokenValue);
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of(mockToken));

        FirebaseMessagingException mockException = mock(FirebaseMessagingException.class);
        when(mockException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isSuccessful()).thenReturn(false);
        when(failedResponse.getException()).thenReturn(mockException);

        BatchResponse mockBatchResponse = mock(BatchResponse.class);
        when(mockBatchResponse.getSuccessCount()).thenReturn(0);
        when(mockBatchResponse.getFailureCount()).thenReturn(1);
        when(mockBatchResponse.getResponses()).thenReturn(List.of(failedResponse));

        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        when(mockMessaging.sendEachForMulticast(any())).thenReturn(mockBatchResponse);

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            staticMock.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            fcmService.sendToUser(1L, "제목", "내용", Map.of());

            verify(fcmTokenRepository, times(1)).deleteByToken(tokenValue);
        }
    }

    @Test
    @DisplayName("sendToUser - 발송 결과에 UNREGISTERED 외 에러가 있으면 토큰 삭제 안 함")
    void sendToUser_doesNotDeleteToken_whenNonUnregisteredError() throws FirebaseMessagingException {
        String tokenValue = "device-token-abc";
        FcmToken mockToken = mock(FcmToken.class);
        when(mockToken.getToken()).thenReturn(tokenValue);
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of(mockToken));

        FirebaseMessagingException mockException = mock(FirebaseMessagingException.class);
        when(mockException.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);

        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isSuccessful()).thenReturn(false);
        when(failedResponse.getException()).thenReturn(mockException);

        BatchResponse mockBatchResponse = mock(BatchResponse.class);
        when(mockBatchResponse.getSuccessCount()).thenReturn(0);
        when(mockBatchResponse.getFailureCount()).thenReturn(1);
        when(mockBatchResponse.getResponses()).thenReturn(List.of(failedResponse));

        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        when(mockMessaging.sendEachForMulticast(any())).thenReturn(mockBatchResponse);

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            staticMock.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            fcmService.sendToUser(1L, "제목", "내용", Map.of());

            verify(fcmTokenRepository, never()).deleteByToken(any());
        }
    }

    @Test
    @DisplayName("sendToUser - 첫 번째 시도 성공 시 재시도 없음")
    void sendToUser_noRetry_whenSuccess() throws FirebaseMessagingException {
        FcmToken mockToken = mock(FcmToken.class);
        when(mockToken.getToken()).thenReturn("device-token-abc");
        when(fcmTokenRepository.findAllByUserId(1L)).thenReturn(List.of(mockToken));

        FirebaseMessaging mockMessaging = mock(FirebaseMessaging.class);
        var mockResponse = mock(com.google.firebase.messaging.BatchResponse.class);
        when(mockResponse.getSuccessCount()).thenReturn(1);
        when(mockResponse.getFailureCount()).thenReturn(0);
        when(mockMessaging.sendEachForMulticast(any())).thenReturn(mockResponse);

        try (MockedStatic<FirebaseMessaging> staticMock = mockStatic(FirebaseMessaging.class)) {
            staticMock.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

            fcmService.sendToUser(1L, "제목", "내용", null);

            verify(mockMessaging, times(1)).sendEachForMulticast(any());
        }
    }
}
