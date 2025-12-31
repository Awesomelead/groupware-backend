package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import kr.co.awesomelead.groupware_backend.global.encryption.PhoneNumberEncryptor;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
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

    @Column(columnDefinition = "CHAR(4)")
    private String password; // 방문자 비밀번호 (4자리 숫자)

    @OneToMany(mappedBy = "visitor")
    @JsonManagedReference
    private List<Visit> visitInfos = new ArrayList<>(); // 방문 기록들
}
