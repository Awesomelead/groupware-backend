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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;
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
@Table(
        name = "safety_training_session_attendees",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_safety_training_session_attendee_session_user",
                    columnNames = {"session_id", "user_id"})
        })
public class SafetyTrainingSessionAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private SafetyTrainingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SafetyTrainingAttendeeStatus status = SafetyTrainingAttendeeStatus.PENDING;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_key", length = 500)
    private String signatureKey;

    @Column(name = "absent_reason", length = 500)
    private String absentReason;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = SafetyTrainingAttendeeStatus.PENDING;
        }
    }
}
