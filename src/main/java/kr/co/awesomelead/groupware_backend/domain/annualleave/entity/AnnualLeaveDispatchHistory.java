package kr.co.awesomelead.groupware_backend.domain.annualleave.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AnnualLeaveDispatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 120)
    private String sheetName;

    @Column(length = 500)
    private String fileKey;

    @Column(nullable = true)
    private LocalDate baseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Company company;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateDispatch(User uploader, String originalFileName, String sheetName, String fileKey, LocalDate baseDate, Company company) {
        this.uploadedBy = uploader;
        this.originalFileName = originalFileName;
        this.sheetName = sheetName;
        this.fileKey = fileKey;
        this.baseDate = baseDate;
        this.company = company;
    }
}
