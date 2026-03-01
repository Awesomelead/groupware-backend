package kr.co.awesomelead.groupware_backend.domain.fcm.repository;

import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.fcm.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByName(String name);
}
