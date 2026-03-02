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
     * Admin/MasterAdmin 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param template   알림 메시지 템플릿
     * @param domainType 생성할 알림의 도메인 타입
     * @param domainId   생성할 알림의 도메인 ID (선택)
     * @param args       템플릿 포맷팅에 사용할 인자
     */
    @Transactional
    public void sendAlertToAdmins(
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Object... args) {
        String title = template.getTitle();
        String content = template.formatContent(args);

        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        admins.addAll(userRepository.findAllByRole(Role.MASTER_ADMIN));

        for (User admin : admins) {
            // 1. FCM 푸시 알림 전송 (토큰이 없으면 내부에서 skip)
            fcmService.sendToUser(admin.getId(), title, content, null);

            // 2. 알림함 저장
            createNotification(
                    admin.getId(), title, content, domainType, domainId, null);
        }

        log.info(
                "관리자 그룹 알림 전송 완료 - 대상 Admin 수: {}, 템플릿: {}",
                admins.size(),
                template.name());
    }

    /**
     * 특정 단일 유저에게 FCM 알림 전송 + Notification 저장
     *
     * @param userId     수신 유저 ID
     * @param template   알림 메시지 템플릿
     * @param domainType 생성할 알림의 도메인 타입
     * @param domainId   생성할 알림의 도메인 ID (선택)
     * @param args       템플릿 포맷팅에 사용할 인자
     */
    @Transactional
    public void sendAlertToUser(
            Long userId,
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Object... args) {
        String title = template.getTitle();
        String content = template.formatContent(args);

        // 1. FCM 푸시 알림 전송
        fcmService.sendToUser(userId, title, content, null);

        // 2. 알림함 저장
        createNotification(userId, title, content, domainType, domainId, null);

        log.info("단일 유저 알림 전송 완료 - userId: {}, 템플릿: {}", userId, template.name());
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

    /**
     * 교육(EduReport) 등록 시 대상 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param eduTypeLabel  교육 유형 라벨 (예: SAFETY, PSM, 부서명 등)
     * @param eduTitle      등록된 교육 제목
     * @param reportId      등록된 교육 ID (알림 domainId로 저장)
     * @param targetUserIds 알림을 받을 유저 ID 집합 (리스트 형태)
     */
    @Transactional
    public void sendEduReportAlertToTargets(
            String eduTypeLabel, String eduTitle, Long reportId, List<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.info("교육 알림 전송 건너뜀 - 대상 없음, reportId: {}", reportId);
            return;
        }

        String title = NotificationMessage.EDU_REPORT_CREATED.getTitle();
        String content = NotificationMessage.EDU_REPORT_CREATED.formatContent(eduTypeLabel, eduTitle);

        for (Long userId : targetUserIds) {
            // 1. FCM 푸시 알림 전송 (토큰이 없으면 내부에서 skip)
            fcmService.sendToUser(userId, title, content, null);

            // 2. 알림함 저장
            createNotification(
                    userId, title, content, NotificationDomainType.EDUCATION, reportId, null);
        }

        log.info(
                "교육 알림 전송 완료 - reportId: {}, 대상 수: {}", reportId, targetUserIds.size());
    }

    /**
     * 방문 이벤트(사전 예약/입실) 발생 시 host 담당 부서 소속 전원에게 FCM 알림 전송 + Notification 저장
     *
     * @param template         알림 메시지 템플릿
     * @param visitId          방문 ID (domainId로 저장)
     * @param hostDepartmentId host 직원의 부서 ID
     * @param contentArgs      템플릿 포맷팅에 사용할 인자
     */
    @Transactional
    public void sendVisitAlertToDepartment(
            NotificationMessage template,
            Long visitId,
            Long hostDepartmentId,
            Object... contentArgs) {
        List<Long> targetUserIds = userRepository.findAllIdsByDepartmentId(hostDepartmentId);

        if (targetUserIds.isEmpty()) {
            log.info("방문 알림 전송 건너뜀 - 대상 없음, visitId: {}", visitId);
            return;
        }

        String title = template.getTitle();
        String content = template.formatContent(contentArgs);

        for (Long userId : targetUserIds) {
            // 1. FCM 푸시 알림 전송 (토큰이 없으면 내부에서 skip)
            fcmService.sendToUser(userId, title, content, null);

            // 2. 알림함 저장
            createNotification(
                    userId, title, content, NotificationDomainType.VISIT, visitId, null);
        }

        log.info(
                "방문 알림 전송 완료 - visitId: {}, 대상 수: {}, 템플릿: {}",
                visitId,
                targetUserIds.size(),
                template.name());
    }
}
