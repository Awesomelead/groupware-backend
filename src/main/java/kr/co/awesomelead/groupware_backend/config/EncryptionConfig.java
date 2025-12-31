package kr.co.awesomelead.groupware_backend.config;

import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${encryption.secret-key}")
    private String secretKey;

    @Value("${encryption.salt}")
    private String salt;

    @Bean
    public SecretKey aesSecretKey() throws Exception {
        // PBKDF2로 안전한 키 생성
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            secretKey.toCharArray(),
            salt.getBytes(),
            65536,  // 반복 횟수
            256     // 키 길이
        );
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
