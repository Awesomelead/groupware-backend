package kr.co.awesomelead.groupware_backend.auth.repository;

import kr.co.awesomelead.groupware_backend.auth.entity.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    Optional<RefreshToken> findByEmail(String username);

    void delete(RefreshToken token);
}
