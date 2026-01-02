package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import kr.co.awesomelead.groupware_backend.global.encryption.PhoneNumberEncryptor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Companion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name; // 동행자 이름

    @Column(length = 50)
    private String visitorCompany; // 동행자 회사

    @Column(nullable = false, length = 300)
    @Convert(converter = PhoneNumberEncryptor.class)
    private String phoneNumber; // 동행자 전화번호

    @Column(nullable = false, length = 64, unique = true)
    private String phoneNumberHash; // SHA-256 해시 (조회용)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private Visit visit; // 동행한 방문 정보

    @PrePersist
    @PreUpdate
    public void onPrePersist() {
        // 전화번호 해시 생성 (평문 상태에서)
        if (this.phoneNumber != null && this.phoneNumberHash == null) {
            this.phoneNumberHash = hashPhoneNumber(this.phoneNumber);
        }
    }

    // 전화번호 SHA-256 해시 생성
    public static String hashPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(phoneNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
