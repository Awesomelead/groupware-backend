package kr.co.awesomelead.groupware_backend.domain.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String AUTH_CODE_PREFIX = "auth:email:";
    private static final String AUTH_CODE_PLACEHOLDER = "{{AUTH_CODE}}";
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
        helper.setSubject("[어썸그룹] 이메일 인증번호 안내");

        String htmlContent =
            """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                        <title>어썸그룹 이메일 인증</title>
                        <style>
                            body, table, td, p, a {
                                font-family: 'Pretendard', 'Apple SD Gothic Neo', 'Malgun Gothic', 'Noto Sans KR', Arial, sans-serif !important;
                            }
                
                            @media only screen and (max-width: 620px) {
                                .wrapper {
                                    width: 100% !important;
                                }
                
                                .card {
                                    padding: 24px 20px !important;
                                }
                
                                .code {
                                    font-size: 30px !important;
                                    letter-spacing: 6px !important;
                                }
                            }
                        </style>
                    </head>
                    <body style="margin: 0; padding: 0; background-color: #F4F6F8; color: #121212;">
                        <div style="display: none; max-height: 0; overflow: hidden; opacity: 0;">
                            어썸그룹 이메일 인증번호 안내 메일입니다.
                        </div>
                
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="background-color: #F4F6F8;">
                            <tr>
                                <td align="center" style="padding: 32px 16px;">
                                    <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="600" class="wrapper" style="width: 100%; max-width: 600px;">
                                        <tr>
                                            <td style="padding-bottom: 12px;">
                                                <span style="display: inline-block; padding: 8px 12px; border-radius: 999px; background-color: #F3F7FC; color: #1E6CED; font-size: 12px; font-weight: 600; line-height: 1;">
                                                    AWESOME GROUP
                                                </span>
                                            </td>
                                        </tr>
                
                                        <tr>
                                            <td class="card" style="background-color: #FFFFFF; border: 1px solid #E7E7E7; border-radius: 16px; padding: 32px;">
                                                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%">
                                                    <tr>
                                                        <td style="font-size: 24px; font-weight: 700; line-height: 1.4; color: #121212;">
                                                            이메일 인증을 진행해주세요
                                                        </td>
                                                    </tr>
                                                    <tr>
                                                        <td style="padding-top: 10px; font-size: 15px; line-height: 1.7; color: #737373;">
                                                            어썸그룹 계정 확인을 위해 아래 인증번호를 입력해주세요.
                                                        </td>
                                                    </tr>
                                                </table>
                
                                                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="margin-top: 24px; background-color: #F3F7FC; border-radius: 14px;">
                                                    <tr>
                                                        <td style="padding: 18px 20px;">
                                                            <div style="font-size: 12px; font-weight: 600; color: #1E6CED; margin-bottom: 8px;">
                                                                인증번호
                                                            </div>
                                                            <div style="background-color: #FFFFFF; border: 1px solid #88A9E1; border-radius: 12px; padding: 20px; text-align: center;">
                                                                <div class="code" style="font-size: 34px; line-height: 1; font-weight: 700; color: #1E6CED; letter-spacing: 8px;">
                                                                    {{AUTH_CODE}}
                                                                </div>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </table>
                
                                                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="margin-top: 18px; border: 1px solid #E7E7E7; border-radius: 12px;">
                                                    <tr>
                                                        <td style="padding: 18px 20px; font-size: 14px; line-height: 1.7; color: #414141;">
                                                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%">
                                                                <tr>
                                                                    <td style="width: 64px; padding-bottom: 6px; color: #737373; white-space: nowrap; vertical-align: top;">유효시간</td>
                                                                    <td style="padding-bottom: 6px; color: #414141; vertical-align: top;">
                                                                        <strong style="color: #1E6CED;">5분</strong>
                                                                    </td>
                                                                </tr>
                                                                <tr>
                                                                    <td style="width: 64px; color: #737373; white-space: nowrap; vertical-align: top;">안내</td>
                                                                    <td style="color: #414141; vertical-align: top;">
                                                                        본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                
                                        <tr>
                                            <td style="padding-top: 16px; text-align: center; font-size: 12px; line-height: 1.6; color: #9D9D9D;">
                                                본 메일은 발신전용 메일입니다.<br />
                                                © Awesome Group. All rights reserved.
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                """
                .replace(AUTH_CODE_PLACEHOLDER, authCode);

        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
