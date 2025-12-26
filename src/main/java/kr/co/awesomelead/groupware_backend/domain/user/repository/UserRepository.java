package kr.co.awesomelead.groupware_backend.domain.user.repository;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<User> findByEmail(String username);
}
