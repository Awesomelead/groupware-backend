package kr.co.awesomelead.groupware_backend.domain.user.repository;

import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String username);
}
