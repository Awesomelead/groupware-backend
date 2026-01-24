package kr.co.awesomelead.groupware_backend.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String AUTH_CODE_PREFIX = "auth:email:";
    private static final int AUTH_CODE_EXPIRATION_MINUTES = 5;

    // 이메일 인증번호 발송
    public void sendAuthCode(String email) {
        // 인증번호 생성
        String authCode = generateAuthCode();

        // 이메일 발송
        try {
            sendEmail(email, authCode);
        } catch (MessagingException e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.EMAIL_SEND_FAILED);
        }

        // Redis에 인증번호 저장 (5분 유효)
        String key = AUTH_CODE_PREFIX + email;
        redisTemplate
                .opsForValue()
                .set(key, authCode, AUTH_CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    // 이메일 인증번호 검증
    public void verifyAuthCode(String email, String authCode) {
        String key = AUTH_CODE_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            throw new CustomException(ErrorCode.AUTH_CODE_EXPIRED);
        }

        if (!savedCode.equals(authCode)) {
            throw new CustomException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        // 인증 성공 시 Redis에서 삭제
        redisTemplate.delete(key);

        // 인증 성공 플래그 저장 (회원가입/비밀번호 찾기 시 사용, 20분 유효)
        String verifiedKey = AUTH_CODE_PREFIX + "verified:" + email;
        redisTemplate.opsForValue().set(verifiedKey, "true", 20, TimeUnit.MINUTES);

        log.info("이메일 인증번호 검증 성공 - 이메일: {}", email);
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        String verifiedKey = AUTH_CODE_PREFIX + "verified:" + email;
        String verified = redisTemplate.opsForValue().get(verifiedKey);
        return "true".equals(verified);
    }

    // 인증 완료 플래그 삭제
    public void clearVerification(String email) {
        String verifiedKey = AUTH_CODE_PREFIX + "verified:" + email;
        redisTemplate.delete(verifiedKey);
    }

    // 6자리 랜덤 인증번호 생성
    private String generateAuthCode() {
        SecureRandom rnadom = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(rnadom.nextInt(10));
        }

        return sb.toString();
    }

    // 이메일 발송
    private void sendEmail(String to, String authCode) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("[어썸그룹] 이메일 인증번호");

        String htmlContent =
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Malgun Gothic', Arial, sans-serif; line-height: 1.6; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4A90E2; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; }
                        .code-box { background-color: white; border: 2px dashed #4A90E2; padding: 20px; text-align: center; font-size: 32px; font-weight: bold; color: #4A90E2; margin: 20px 0; letter-spacing: 5px; }
                        .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>어썸그룹 이메일 인증</h1>
                        </div>
                        <div class="content">
                            <p>안녕하세요,</p>
                            <p>어썸그룹 이메일 인증번호입니다.</p>
                            <p>아래 인증번호를 입력하여 이메일 인증을 완료해주세요.</p>

                            <div class="code-box">
                                %s
                            </div>

                            <p style="color: #E74C3C;">※ 인증번호는 <strong>5분간 유효</strong>합니다.</p>
                            <p>본인이 요청하지 않은 경우, 이 이메일을 무시하셔도 됩니다.</p>
                        </div>
                        <div class="footer">
                            <p>© 어썸그룹. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                        .formatted(authCode);

        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
