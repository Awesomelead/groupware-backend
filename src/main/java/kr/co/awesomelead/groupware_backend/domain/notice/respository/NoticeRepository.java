package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import kr.co.awesomelead.groupware_backend.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

}
