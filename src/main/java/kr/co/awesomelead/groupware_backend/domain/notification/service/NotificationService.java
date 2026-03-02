package kr.co.awesomelead.groupware_backend.domain.notification.service;

import kr.co.awesomelead.groupware_backend.domain.fcm.service.FcmService;
import kr.co.awesomelead.groupware_backend.domain.notification.dto.response.NotificationResponseDto;
import kr.co.awesomelead.groupware_backend.domain.notification.entity.Notification;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;
import kr.co.awesomelead.groupware_backend.domain.notification.repository.NotificationRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(
            Long userId,
            String title,
            String content,
            NotificationDomainType domainType,
            Long domainId,
            String redirectUrl) {
        Notification notification = Notification.of(userId, title, content, domainType, domainId, redirectUrl);
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
        Notification notification = notificationRepository
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

    /**
     * 회원가입 완료 시 Admin/MasterAdmin 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param newUserName 가입한 신규 유저의 표시 이름
     */
    @Transactional
    public void sendSignupAlertToAdmins(String newUserName) {
        String title = NotificationMessage.SIGNUP_ADMIN_ALERT.getTitle();
        String content = NotificationMessage.SIGNUP_ADMIN_ALERT.formatContent(newUserName);

        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        admins.addAll(userRepository.findAllByRole(Role.MASTER_ADMIN));

        for (User admin : admins) {
            // 1. FCM 푸시 알림 전송 (토큰이 없으면 내부에서 skip)
            fcmService.sendToUser(admin.getId(), title, content, null);

            // 2. 알림함 저장
            createNotification(admin.getId(), title, content, NotificationDomainType.AUTH, null, null);
        }

        log.info("신규 가입 알림 전송 완료 - 대상 Admin 수: {}, 신규 유저: {}", admins.size(), newUserName);
    }

    /**
     * 공지사항 등록 시 대상 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param noticeTitle   등록된 공지사항 제목
     * @param noticeId      등록된 공지사항 ID (알림 domainId로 저장)
     * @param targetUserIds 알림을 받을 유저 ID 집합
     */
    @Transactional
    public void sendNoticeAlertToTargets(
            String noticeTitle, Long noticeId, Set<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.info("공지 알림 전송 건너뜀 - 대상 없음, noticeId: {}", noticeId);
            return;
        }

        String title = NotificationMessage.NOTICE_CREATED.getTitle();
        String content = NotificationMessage.NOTICE_CREATED.formatContent(noticeTitle);

        for (Long userId : targetUserIds) {
            // 1. FCM 푸시 알림 전송 (토큰이 없으면 내부에서 skip)
            fcmService.sendToUser(userId, title, content, null);

            // 2. 알림함 저장
            createNotification(
                    userId, title, content, NotificationDomainType.NOTICE, noticeId, null);
        }

        log.info(
                "공지 알림 전송 완료 - noticeId: {}, 대상 수: {}", noticeId, targetUserIds.size());
    }
}
