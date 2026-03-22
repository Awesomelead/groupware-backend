package kr.co.awesomelead.groupware_backend.domain.fcm.event;

import com.google.firebase.messaging.FirebaseMessagingException;

import kr.co.awesomelead.groupware_backend.domain.fcm.service.FcmService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmEventListener {

    private final FcmService fcmService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFcmSendEvent(FcmSendEvent event) throws FirebaseMessagingException {
        log.debug("FCM 이벤트 수신 (AFTER_COMMIT) - userId: {}", event.userId());
        fcmService.sendToUser(event.userId(), event.title(), event.body(), event.data());
    }
}
