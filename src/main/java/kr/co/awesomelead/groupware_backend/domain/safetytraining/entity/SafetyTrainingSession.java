package kr.co.awesomelead.groupware_backend.domain.safetytraining.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyEducationType;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingSessionStatus;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "safety_training_sessions")
public class SafetyTrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_type", nullable = false, length = 30)
    private SafetyEducationType educationType;

    @Lob
    @Column(name = "education_methods_json", columnDefinition = "TEXT", nullable = false)
    private String educationMethodsJson;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "education_date_text", nullable = false, length = 200)
    private String educationDateText;

    @Lob
    @Column(name = "education_content", columnDefinition = "TEXT")
    private String educationContent;

    @Column(nullable = false, length = 200)
    private String place;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_scope", nullable = false, length = 20)
    private Company companyScope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_user_id", nullable = false)
    private User instructorUser;

    @Column(name = "instructor_name_snapshot", nullable = false, length = 100)
    private String instructorNameSnapshot;

    @Builder.Default
    @Column(name = "target_count", nullable = false)
    private int targetCount = 0;

    @Builder.Default
    @Column(name = "attended_count", nullable = false)
    private int attendedCount = 0;

    @Builder.Default
    @Column(name = "absent_count", nullable = false)
    private int absentCount = 0;

    @Lob
    @Column(name = "absent_reason_summary", columnDefinition = "TEXT")
    private String absentReasonSummary;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SafetyTrainingSessionStatus status = SafetyTrainingSessionStatus.OPEN;

    @Column(name = "report_file_key", length = 500)
    private String reportFileKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = SafetyTrainingSessionStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
