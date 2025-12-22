package kr.co.awesomelead.groupware_backend.domain.notice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

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

import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공지 유형 (일반, 긴급 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeType type;

    // 제목
    @Column(nullable = false, length = 100)
    private String title;

    // 내용
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @JsonBackReference
    private User author;

    // 작성일 (자동 생성)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate private LocalDateTime updatedDate;

    // 조회수
    @Column(nullable = false)
    private int viewCount = 0;

    // 상단 고정 여부
    @Column(nullable = false)
    private boolean pinned = false;

    // 첨부파일 리스트
    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NoticeAttachment> attachments = new ArrayList<>();

    public void addAttachment(NoticeAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setNotice(this);
    }

    // 조회수 증가 메서드
    public void increaseViewCount() {
        this.viewCount++;
    }
}
