package kr.co.awesomelead.groupware_backend.global.encryption;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AESEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    // 문자열 암호화
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 1. IV(Initialization Vector) 생성 - 매번 랜덤하게
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 2. Cipher 초기화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 3. 암호화 수행
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 4. IV + 암호문을 합쳐서 저장 (복호화할 때 IV 필요)
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            // 5. Base64 인코딩하여 문자열로 반환
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            log.error("암호화 실패: {}", e.getMessage());
            throw new RuntimeException("암호화 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 문자열 복호화
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // 1. Base64 디코딩
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // 2. IV 추출 (앞 12바이트)
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);

            // 3. 암호문 추출 (나머지)
            byte[] encryptedBytes = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            // 4. Cipher 초기화
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 5. 복호화 수행
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("복호화 실패: {}", e.getMessage());
            throw new RuntimeException("복호화 처리 중 오류가 발생했습니다.", e);
        }
    }
}
