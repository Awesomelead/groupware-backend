package kr.co.awesomelead.groupware_backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.user.enums.MyInfoUpdateRequestStatus;
import kr.co.awesomelead.groupware_backend.global.common.entity.BaseTimeEntity;
import kr.co.awesomelead.groupware_backend.global.encryption.Encryptor;
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
@Table(name = "personal_info_update_requests")
public class MyInfoUpdateRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)
    private String requestedNameEng;

    @Column(length = 300)
    @Convert(converter = Encryptor.class)
    private String requestedPhoneNumber;

    @Column(length = 64)
    private String requestedPhoneNumberHash;

    @Column(length = 500)
    @Convert(converter = Encryptor.class)
    private String requestedZipcode;

    @Column(length = 500)
    @Convert(converter = Encryptor.class)
    private String requestedAddress1;

    @Column(length = 500)
    @Convert(converter = Encryptor.class)
    private String requestedAddress2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MyInfoUpdateRequestStatus status = MyInfoUpdateRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 255)
    private String rejectReason;

    public void approve(User reviewer) {
        this.status = MyInfoUpdateRequestStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.rejectReason = null;
    }

    public void reject(User reviewer, String reason) {
        this.status = MyInfoUpdateRequestStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.rejectReason = reason;
    }
}
