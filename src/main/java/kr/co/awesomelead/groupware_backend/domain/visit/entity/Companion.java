package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Companion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String name; // 동행자 이름

    @Column(length = 50)
    private String hostCompany; // 동행자 회사

    @Column(nullable = false, length = 11)
    private String phoneNumber; // 동행자 전화번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_info_id")
    private VisitInfo visitInfo; // 동행한 방문 정보


}
