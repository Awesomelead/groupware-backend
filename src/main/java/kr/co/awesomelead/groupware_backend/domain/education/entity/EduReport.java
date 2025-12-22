package kr.co.awesomelead.groupware_backend.domain.education.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "edu_reports")
public class EduReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EduType eduType;

    @Column(nullable = false)
    private LocalDate eduDate;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @OneToMany(mappedBy = "eduReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EduAttachment> attachments = new ArrayList<>();

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private boolean signatureRequired = false; // true일 때만 EduAttendance.signatureKey 작성

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = true) // 부서교육의 경우에만 작성
    private Department department;

    public void addAttachment(EduAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setEduReport(this);
    }
}
