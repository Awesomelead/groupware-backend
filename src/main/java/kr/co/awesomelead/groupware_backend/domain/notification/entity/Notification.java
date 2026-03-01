package kr.co.awesomelead.groupware_backend.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationDomainType domainType;

    private Long domainId;

    @Column(length = 500)
    private String redirectUrl;

    @Column(nullable = false)
    private Boolean isRead;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    private Notification(
        Long userId,
        String title,
        String content,
        NotificationDomainType domainType,
        Long domainId,
        String redirectUrl) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.domainType = domainType;
        this.domainId = domainId;
        this.redirectUrl = redirectUrl;
        this.isRead = false;
    }

    public static Notification of(
        Long userId,
        String title,
        String content,
        NotificationDomainType domainType,
        Long domainId,
        String redirectUrl) {
        validate(userId, title, content, domainType);
        return new Notification(userId, title, content, domainType, domainId, redirectUrl);
    }

    private static void validate(
        Long userId, String title, String content, NotificationDomainType domainType) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다.");
        }
        if (domainType == null) {
            throw new IllegalArgumentException("domainType은 필수입니다.");
        }
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
