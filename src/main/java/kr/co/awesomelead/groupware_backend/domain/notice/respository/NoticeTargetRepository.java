package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import kr.co.awesomelead.groupware_backend.domain.notice.entity.NoticeTarget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeTargetRepository extends JpaRepository<NoticeTarget, Long> {

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NoticeTarget nt where nt.notice.id = :noticeId")
    void deleteByNoticeId(@Param("noticeId") Long noticeId);

    @Query(
            "select nt from NoticeTarget nt "
                    + "join fetch nt.user u "
                    + "left join fetch u.department "
                    + "where nt.notice.id = :noticeId "
                    + "order by nt.id asc")
    List<NoticeTarget> findAllByNoticeIdWithUserAndDepartment(@Param("noticeId") Long noticeId);
}
