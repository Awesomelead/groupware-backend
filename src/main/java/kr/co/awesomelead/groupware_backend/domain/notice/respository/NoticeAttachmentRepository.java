package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeAttachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {
    Optional<NoticeAttachment> findByIdAndNoticeId(Long id, Long noticeId);
}
