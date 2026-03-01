package kr.co.awesomelead.groupware_backend.domain.fcm.repository;

import java.util.List;
import java.util.Optional;
import kr.co.awesomelead.groupware_backend.domain.fcm.entity.FcmToken;
import kr.co.awesomelead.groupware_backend.domain.fcm.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByUserIdAndDeviceType(Long userId, DeviceType deviceType);

    List<FcmToken> findAllByUserId(Long userId);

    Optional<FcmToken> findByToken(String token);
}
