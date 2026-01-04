package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeAttachment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {}
