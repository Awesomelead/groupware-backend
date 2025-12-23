package kr.co.awesomelead.groupware_backend.domain.education.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EduAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // DB 컬럼명: user_id
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edu_report_id", nullable = false)
    private EduReport eduReport;

    @Column(nullable = false)
    private boolean attendance = false; // 출석 여부

    @Column(length = 200)
    private String signatureKey; // 서명 키
}
