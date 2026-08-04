package kr.co.awesomelead.groupware_backend.domain.aligo.service;

import kr.co.awesomelead.groupware_backend.domain.aligo.enums.PhoneAuthChannel;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhoneAuthService {

    private final kr.co.awesomelead.groupware_backend.domain.aligo.service.AligoKakaoService
            aligoKakaoService;
    private final StringRedisTemplate redisTemplate;

    private static final String AUTH_CODE_PREFIX = "auth:phone:";
    private static final int AUTH_CODE_EXPIRATION_MINUTES = 5;

    @Value("${aligo.api.test-mode:false}")
    private boolean testMode;

    /** 인증번호 발송 */
    public void sendAuthCode(String phoneNumber) {
        sendAuthCode(phoneNumber, PhoneAuthChannel.KAKAO);
    }

    /** 인증번호 발송 */
    public void sendAuthCode(String phoneNumber, PhoneAuthChannel channel) {
        // 인증번호 생성
        String authCode = generateAuthCode();
        PhoneAuthChannel resolvedChannel =
                channel != null ? channel : PhoneAuthChannel.KAKAO;

        boolean success =
                switch (resolvedChannel) {
                    case KAKAO -> aligoKakaoService.sendAuthCodeAlimtalk(phoneNumber, authCode);
                    case SMS -> aligoKakaoService.sendAuthCodeSms(phoneNumber, authCode);
                };

        if (!success) {
            throw new CustomException(ErrorCode.ALIMTALK_SEND_FAILED);
        }

        // Redis에 인증번호 저장 (5분 유효)
        String key = AUTH_CODE_PREFIX + phoneNumber;
        redisTemplate
                .opsForValue()
                .set(key, authCode, AUTH_CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // 테스트 모드일 경우 콘솔에 인증번호 출력
        if (testMode) { // ← 변수명 변경
            log.warn("========================================");
            log.warn("📱 [테스트 모드] 인증번호 확인");
            log.warn("전화번호: {}", phoneNumber);
            log.warn("인증번호: {}", authCode);
            log.warn("========================================");
        }

        log.info("인증번호 발송 성공 - 전화번호: {}, 발송방식: {}", phoneNumber, resolvedChannel);
    }

    /** 인증번호 검증 */
    public void verifyAuthCode(String phoneNumber, String authCode) {
        String key = AUTH_CODE_PREFIX + phoneNumber;
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new CustomException(ErrorCode.AUTH_CODE_EXPIRED);
        }

        if (!savedCode.equals(authCode)) {
            throw new CustomException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        // 인증 성공 시 Redis에서 삭제
        redisTemplate.delete(key);

        // 인증 성공 플래그 저장 (회원가입 시 사용, 20분 유효)
        String verifiedKey = AUTH_CODE_PREFIX + "verified:" + phoneNumber;
        redisTemplate.opsForValue().set(verifiedKey, "true", 20, TimeUnit.MINUTES);

        log.info("인증번호 검증 성공 - 전화번호: {}", phoneNumber);
    }

    /** 전화번호 인증 여부 확인 (회원가입 시 사용) */
    public boolean isPhoneVerified(String phoneNumber) {
        String verifiedKey = AUTH_CODE_PREFIX + "verified:" + phoneNumber;
        String verified = redisTemplate.opsForValue().get(verifiedKey);
        return "true".equals(verified);
    }

    /** 인증 완료 플래그 삭제 (회원가입 완료 후 호출) */
    public void clearVerification(String phoneNumber) {
        String verifiedKey = AUTH_CODE_PREFIX + "verified:" + phoneNumber;
        redisTemplate.delete(verifiedKey);
    }

    /** 6자리 랜덤 인증번호 생성 */
    private String generateAuthCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(9) + 1);
        }

        return sb.toString();
    }
}
