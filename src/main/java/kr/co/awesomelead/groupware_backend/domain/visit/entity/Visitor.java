package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import kr.co.awesomelead.groupware_backend.global.encryption.PhoneNumberEncryptor;

import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Setter
@Getter
@Entity
public class Visitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name; // 방문자 이름

    @Column(nullable = false, length = 300)
    @Convert(converter = PhoneNumberEncryptor.class)
    private String phoneNumber; // 방문자 전화번호

    @Column(nullable = false, length = 64, unique = true)
    private String phoneNumberHash; // SHA-256 해시 (조회용)

    @Column(columnDefinition = "CHAR(4)")
    private String password; // 방문자 비밀번호 (4자리 숫자)

    @OneToMany(mappedBy = "visitor")
    @JsonManagedReference
    private List<Visit> visitInfos = new ArrayList<>(); // 방문 기록들

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
