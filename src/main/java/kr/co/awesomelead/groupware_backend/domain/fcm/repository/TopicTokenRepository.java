package kr.co.awesomelead.groupware_backend.domain.fcm.repository;

import kr.co.awesomelead.groupware_backend.domain.fcm.entity.TopicToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicTokenRepository extends JpaRepository<TopicToken, Long> {

    List<TopicToken> findAllByTopicId(Long topicId);

    List<TopicToken> findAllByTokenId(Long tokenId);
}
