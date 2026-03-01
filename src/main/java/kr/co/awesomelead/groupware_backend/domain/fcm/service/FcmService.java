package kr.co.awesomelead.groupware_backend.domain.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

import kr.co.awesomelead.groupware_backend.domain.fcm.entity.FcmToken;
import kr.co.awesomelead.groupware_backend.domain.fcm.repository.FcmTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    /** 특정 유저의 모든 디바이스로 메시지 발송 */
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
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

        try {
            var response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
            log.info(
                    "FCM 다중 발송 완료 - userId: {}, 성공: {}, 실패: {}",
                    userId,
                    response.getSuccessCount(),
                    response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("FCM 다중 발송 실패 - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    /** 토픽 구독 전체 유저에 메시지 발송 */
    public void sendToTopic(String topicName, String title, String body) {
        Message message =
                Message.builder()
                        .setTopic(topicName)
                        .setNotification(
                                Notification.builder().setTitle(title).setBody(body).build())
                        .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 토픽 발송 완료 - topic: {}, messageId: {}", topicName, messageId);
        } catch (FirebaseMessagingException e) {
            log.error("FCM 토픽 발송 실패 - topic: {}, error: {}", topicName, e.getMessage());
        }
    }
}
