package kr.co.awesomelead.groupware_backend.domain.approval.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.global.common.entity.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_personal_settings")
public class ApprovalPersonalSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private Boolean delegateEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate_user_id")
    private User delegateUser;

    @Column(name = "delegate_start_date")
    private LocalDate delegateStartDate;

    @Column(name = "delegate_end_date")
    private LocalDate delegateEndDate;

    @Column(name = "signature_image_key", length = 500)
    private String signatureImageKey;

    @Builder.Default
    @OneToMany(
            mappedBy = "setting",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<ApprovalPersonalViewerTarget> defaultViewerTargets = new ArrayList<>();
}
