package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeTargetRepository extends JpaRepository<NoticeTarget, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NoticeTarget nt where nt.notice.id = :noticeId")
    void deleteByNoticeId(@Param("noticeId") Long noticeId);
}
