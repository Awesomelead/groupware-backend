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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "edu_reports")
// @Table(indexes = @Index(name = "idx_edu_date", columnList = "eduDate")) 차후 성능비교를 위해 대기
public class EduReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EduType eduType;

    @Column(nullable = false)
    private LocalDate eduDate;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @OneToMany(mappedBy = "eduReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EduAttachment> attachments = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean pinned = false;

    @Builder.Default
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
