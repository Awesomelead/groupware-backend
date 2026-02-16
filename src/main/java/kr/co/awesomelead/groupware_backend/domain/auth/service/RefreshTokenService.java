package kr.co.awesomelead.groupware_backend.domain.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.auth.entity.RefreshToken;
import kr.co.awesomelead.groupware_backend.domain.auth.repository.RefreshTokenRepository;
import kr.co.awesomelead.groupware_backend.domain.auth.util.JWTUtil;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JWTUtil jwtUtil;

    // Refresh Token의 유효 기간
    @Value("${spring.jwt.refresh-validation}")
    private long refreshTokenValidation;

    /**
     * 새로운 Refresh Token을 생성하고 DB에 저장하는 메소드
     */
    @Transactional
    public String createAndSaveRefreshToken(String email, String role) {
        // 1. 새로운 토큰 값 생성
        String newRefreshTokenValue = jwtUtil.createJwt(email, role, refreshTokenValidation);

        // 2. 만료 시간 계산
        LocalDateTime expiration = LocalDateTime.now()
            .plus(refreshTokenValidation, ChronoUnit.MILLIS);

        // 3. 해당 사용자의 기존 Refresh Token이 있다면 삭제
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByEmail(email);

        if (existingTokenOpt.isPresent()) {
            RefreshToken existingToken = existingTokenOpt.get();
            existingToken.setTokenValue(newRefreshTokenValue);
            existingToken.setExpirationDate(expiration);
        } else {
            RefreshToken newRefreshToken =
                RefreshToken.builder()
                    .email(email)
                    .tokenValue(newRefreshTokenValue)
                    .expirationDate(expiration)
                    .build();
            refreshTokenRepository.save(newRefreshToken);
        }

        return newRefreshTokenValue;
    }

    @Transactional
    public void deleteRefreshToken(String refreshTokenValue) {
        // 전달받은 토큰 값으로 DB에서 해당 토큰을 찾아 존재하면 삭제
        refreshTokenRepository
            .findByTokenValue(refreshTokenValue)
            .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public RefreshToken validateRefreshToken(String tokenValue) {
        // DB에서 토큰을 찾지 못하면 예외 발생
        RefreshToken token =
            refreshTokenRepository
                .findByTokenValue(tokenValue)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        // DB에 저장된 만료 시간으로 유효성 검사
        if (token.isExpired()) {
            // 만료된 토큰은 DB에서 삭제 후 예외 발생
            refreshTokenRepository.delete(token);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        // JWT 자체 만료/위조 체크
        try {
            jwtUtil.getUsername(token.getTokenValue());
            jwtUtil.getRole(token.getTokenValue());
        } catch (ExpiredJwtException e) {
            refreshTokenRepository.delete(token);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            refreshTokenRepository.delete(token);
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        return token;
    }
}
