package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

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

    @Column(nullable = false, length = 11)
    private String phoneNumber; // 방문자 전화번호

    @Column(nullable = false, columnDefinition = "CHAR(4)")
    private String password; // 방문자 비밀번호 (4자리 숫자)

    @OneToMany(mappedBy = "visitor")
    @JsonManagedReference
    private List<VisitInfo> visitInfos = new ArrayList<>(); // 방문 기록들
}
