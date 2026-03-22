package kr.co.awesomelead.groupware_backend.domain.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import kr.co.awesomelead.groupware_backend.domain.fcm.entity.FcmToken;
import kr.co.awesomelead.groupware_backend.domain.fcm.repository.FcmTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    /** 특정 유저의 모든 디바이스로 메시지 발송 */
    @Async("fcmTaskExecutor")
    @Retryable(
            value = {FirebaseMessagingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0))
    public void sendToUser(Long userId, String title, String body, Map<String, String> data)
            throws FirebaseMessagingException {
        List<FcmToken> tokens = fcmTokenRepository.findAllByUserId(userId);
        if (tokens.isEmpty()) {
            log.info("FCM 발송 건너뜀 - userId: {} (등록된 토큰 없음)", userId);
            return;
        }

        List<String> tokenValues = tokens.stream().map(FcmToken::getToken).toList();

        MulticastMessage.Builder builder =
                MulticastMessage.builder()
                        .setNotification(
                                Notification.builder().setTitle(title).setBody(body).build())
                        .addAllTokens(tokenValues);

        if (data != null && !data.isEmpty()) {
            builder.putAllData(data);
        }

        BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
        log.info(
                "FCM 다중 발송 완료 - userId: {}, 성공: {}, 실패: {}",
                userId,
                response.getSuccessCount(),
                response.getFailureCount());

        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse sendResponse = responses.get(i);
                if (!sendResponse.isSuccessful()
                        && sendResponse.getException().getMessagingErrorCode()
                                == MessagingErrorCode.UNREGISTERED) {
                    String invalidToken = tokenValues.get(i);
                    log.warn("무효 토큰 삭제 - userId: {}, token: {}...{}", userId,
                            invalidToken.substring(0, 10), invalidToken.substring(invalidToken.length() - 6));
                    fcmTokenRepository.deleteByToken(invalidToken);
                }
            }
        }
    }

    @Recover
    public void recoverSendToUser(
            FirebaseMessagingException e,
            Long userId,
            String title,
            String body,
            Map<String, String> data) {
        log.error(
                "FCM 전송 최종 실패 (Retry 종료) - userId: {}, errorCode: {}, error: {}",
                userId,
                e.getMessagingErrorCode(),
                e.getMessage());

        // API 레벨 실패(네트워크/인증 오류)이므로 토큰 삭제 없이 로그만 기록
    }

    /** 토픽 구독 전체 유저에 메시지 발송 */
    @Retryable(
            value = {FirebaseMessagingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0))
    public void sendToTopic(String topicName, String title, String body)
            throws FirebaseMessagingException {
        Message message =
                Message.builder()
                        .setTopic(topicName)
                        .setNotification(
                                Notification.builder().setTitle(title).setBody(body).build())
                        .build();

        String messageId = FirebaseMessaging.getInstance().send(message);
        log.info("FCM 토픽 발송 완료 - topic: {}, messageId: {}", topicName, messageId);
    }

    @Recover
    public void recoverSendToTopic(
            FirebaseMessagingException e, String topicName, String title, String body) {
        log.error(
                "FCM 전송 최종 실패 (Retry 종료) - topic: {}, error: {}", topicName, e.getMessage());
    }
}
