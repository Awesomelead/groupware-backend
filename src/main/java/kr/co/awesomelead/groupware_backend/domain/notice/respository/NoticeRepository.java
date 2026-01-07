package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import kr.co.awesomelead.groupware_backend.domain.notice.entity.Notice;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    List<Notice> findByTypeOrderByPinnedDescUpdatedDateDesc(NoticeType type);

    List<Notice> findAllByOrderByPinnedDescUpdatedDateDesc();

    @Query(
            "select n from Notice n "
                    + "join fetch n.author "
                    + // 작성자 정보 미리 가져오기
                    "left join fetch n.attachments "
                    + // 첨부파일 정보 미리 가져오기 (없을 수도 있으니 left join)
                    "where n.id = :id")
    Optional<Notice> findByIdWithDetails(@Param("id") Long id);
}
