package kr.co.awesomelead.groupware_backend.domain.fcm.service;

import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.fcm.entity.FcmToken;
import kr.co.awesomelead.groupware_backend.domain.fcm.enums.DeviceType;
import kr.co.awesomelead.groupware_backend.domain.fcm.repository.FcmTokenRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * FCM 토큰 등록 또는 갱신 (Upsert) - 동일 userId + deviceType 조합이 존재하면 토큰 업데이트 - 새로운 조합이면 신규 생성
     */
    @Transactional
    public void registerToken(Long userId, String token, DeviceType deviceType) {
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Optional<FcmToken> existing = fcmTokenRepository.findByUserIdAndDeviceType(userId,
            deviceType);

        if (existing.isPresent()) {
            existing.get().updateToken(token);
            log.info("FCM 토큰 갱신 - userId: {}, deviceType: {}", userId, deviceType);
        } else {
            FcmToken fcmToken = FcmToken.of(user, token, deviceType);
            fcmTokenRepository.save(fcmToken);
            log.info("FCM 토큰 신규 등록 - userId: {}, deviceType: {}", userId, deviceType);
        }
    }

    /**
     * FCM 토큰 삭제 (로그아웃 시)
     */
    @Transactional
    public void deleteToken(Long userId, String token) {
        FcmToken fcmToken = fcmTokenRepository
            .findByToken(token)
            .orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

        if (!fcmToken.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        fcmTokenRepository.delete(fcmToken);
        log.info("FCM 토큰 삭제 - userId: {}", userId);
    }
}
