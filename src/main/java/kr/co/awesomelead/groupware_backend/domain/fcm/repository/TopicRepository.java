package kr.co.awesomelead.groupware_backend.domain.fcm.repository;

import kr.co.awesomelead.groupware_backend.domain.fcm.entity.Topic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByName(String name);
}
