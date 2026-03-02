package kr.co.awesomelead.groupware_backend.domain.fcm.repository;

import kr.co.awesomelead.groupware_backend.domain.fcm.entity.TopicMember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicMemberRepository extends JpaRepository<TopicMember, Long> {

    List<TopicMember> findAllByTopicId(Long topicId);

    List<TopicMember> findAllByUserId(Long userId);
}
