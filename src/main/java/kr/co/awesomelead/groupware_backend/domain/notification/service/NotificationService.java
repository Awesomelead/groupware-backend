package kr.co.awesomelead.groupware_backend.domain.notification.service;

import kr.co.awesomelead.groupware_backend.domain.notification.dto.response.NotificationResponseDto;
import kr.co.awesomelead.groupware_backend.domain.notification.entity.Notification;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.repository.NotificationRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(
            Long userId,
            String title,
            String content,
            NotificationDomainType domainType,
            Long domainId,
            String redirectUrl) {
        Notification notification =
                Notification.of(userId, title, content, domainType, domainId, redirectUrl);
        notificationRepository.save(notification);
        log.info("알림 생성 - userId: {}, domainType: {}", userId, domainType);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponseDto::from);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_NOTIFICATION);
        }

        notification.markAsRead();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
}
