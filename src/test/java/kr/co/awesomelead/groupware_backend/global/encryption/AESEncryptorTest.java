package kr.co.awesomelead.groupware_backend.global.encryption;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AESEncryptorTest {

    @Autowired
    private AESEncryptor aesEncryptor;

    @Test
    @DisplayName("문자열 암호화 후 복호화하면 원본과 같아야 한다")
    void encryptAndDecrypt() {
        // given
        String original = "950101-1234567";

        // when
        String encrypted = aesEncryptor.encrypt(original);
        String decrypted = aesEncryptor.decrypt(encrypted);

        // then
        assertThat(encrypted).isNotEqualTo(original); // 암호화됨
        assertThat(decrypted).isEqualTo(original);     // 복호화하면 원본
    }

    @Test
    @DisplayName("같은 문자열을 두 번 암호화하면 결과가 달라야 한다 (IV 랜덤)")
    void encryptTwice_shouldDifferent() {
        // given
        String original = "01012345678";

        // when
        String encrypted1 = aesEncryptor.encrypt(original);
        String encrypted2 = aesEncryptor.encrypt(original);

        // then
        assertThat(encrypted1).isNotEqualTo(encrypted2); // IV가 다르므로 암호문도 다름
        assertThat(aesEncryptor.decrypt(encrypted1)).isEqualTo(original);
        assertThat(aesEncryptor.decrypt(encrypted2)).isEqualTo(original);
    }

    @Test
    @DisplayName("null이나 빈 문자열은 그대로 반환해야 한다")
    void encryptNullOrEmpty() {
        // when & then
        assertThat(aesEncryptor.encrypt(null)).isNull();
        assertThat(aesEncryptor.encrypt("")).isEmpty();
        assertThat(aesEncryptor.decrypt(null)).isNull();
        assertThat(aesEncryptor.decrypt("")).isEmpty();
    }

    @Test
    @DisplayName("전화번호 암호화 테스트")
    void encryptPhoneNumber() {
        // given
        String phoneNumber = "01012345678";

        // when
        String encrypted = aesEncryptor.encrypt(phoneNumber);
        String decrypted = aesEncryptor.decrypt(encrypted);

        // then
        assertThat(decrypted).isEqualTo(phoneNumber);
        System.out.println("원본: " + phoneNumber);
        System.out.println("암호화: " + encrypted);
        System.out.println("암호화 길이: " + encrypted.length());
    }
}