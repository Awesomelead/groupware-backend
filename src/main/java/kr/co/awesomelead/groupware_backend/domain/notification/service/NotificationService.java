package kr.co.awesomelead.groupware_backend.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.awesomelead.groupware_backend.domain.fcm.event.FcmSendEvent;
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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void createNotification(
            Long userId,
            String title,
            String content,
            NotificationDomainType domainType,
            Long domainId) {
        createNotification(userId, title, content, domainType, domainId, null);
    }

    @Transactional
    public void createNotification(
            Long userId,
            String title,
            String content,
            NotificationDomainType domainType,
            Long domainId,
            Map<String, Object> metadata) {
        createNotification(userId, title, content, domainType, domainId, metadata, false);
    }

    @Transactional
    public void createNotification(
            Long userId,
            String title,
            String content,
            NotificationDomainType domainType,
            Long domainId,
            Map<String, Object> metadata,
            boolean requiresApproval) {
        Notification notification =
                Notification.of(
                        userId, title, content, domainType, domainId, metadata, requiresApproval);
        notificationRepository.save(notification);
        log.info("알림 생성 - userId: {}, domainType: {}", userId, domainType);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(
            Long userId, boolean pendingApproval, Pageable pageable) {
        Page<Notification> page =
                pendingApproval
                        ? notificationRepository
                                .findByUserIdAndRequiresApprovalTrueOrderByCreatedAtDesc(
                                        userId, pageable)
                        : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationResponseDto::from);
    }

    @Transactional
    public void resolveRequiresApproval(NotificationDomainType domainType, Long domainId) {
        notificationRepository.resolveRequiresApprovalByDomainTypeAndDomainId(domainType, domainId);
        log.info("requiresApproval 해제 - domainType: {}, domainId: {}", domainType, domainId);
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

    /**
     * Admin/MasterAdmin 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param template 알림 메시지 템플릿
     * @param domainType 생성할 알림의 도메인 타입
     * @param domainId 생성할 알림의 도메인 ID (선택)
     * @param args 템플릿 포맷팅에 사용할 인자
     */
    @Transactional
    public void sendAlertToAdmins(
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Object... args) {
        sendAlertToAdmins(template, domainType, domainId, null, args);
    }

    @Transactional
    public void sendAlertToAdmins(
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Map<String, Object> metadata,
            Object... args) {
        sendAlertToAdminsInternal(template, domainType, domainId, metadata, false, args);
    }

    @Transactional
    public void sendAlertToAdminsRequiringApproval(
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Map<String, Object> metadata,
            Object... args) {
        sendAlertToAdminsInternal(template, domainType, domainId, metadata, true, args);
    }

    private void sendAlertToAdminsInternal(
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Map<String, Object> metadata,
            boolean requiresApproval,
            Object... args) {
        String title = template.getTitle();
        String content = template.formatContent(args);

        List<User> admins = userRepository.findAllByRole(Role.ADMIN);
        admins.addAll(userRepository.findAllByRole(Role.MASTER_ADMIN));

        for (User admin : admins) {
            // 1. 알림함 저장
            createNotification(
                    admin.getId(),
                    title,
                    content,
                    domainType,
                    domainId,
                    metadata,
                    requiresApproval);

            // 2. FCM 이벤트 발행 (트랜잭션 커밋 후 비동기 발송)
            eventPublisher.publishEvent(
                    new FcmSendEvent(
                            admin.getId(),
                            title,
                            content,
                            buildFcmData(domainType, domainId, metadata)));
        }

        log.info("관리자 그룹 알림 전송 완료 - 대상 Admin 수: {}, 템플릿: {}", admins.size(), template.name());
    }

    /**
     * 특정 단일 유저에게 FCM 알림 전송 + Notification 저장
     *
     * @param userId 수신 유저 ID
     * @param template 알림 메시지 템플릿
     * @param domainType 생성할 알림의 도메인 타입
     * @param domainId 생성할 알림의 도메인 ID
     * @param args 템플릿 포맷팅에 사용할 인자
     */
    @Transactional
    public void sendAlertToUser(
            Long userId,
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Object... args) {
        sendAlertToUser(userId, template, domainType, domainId, null, args);
    }

    @Transactional
    public void sendAlertToUser(
            Long userId,
            NotificationMessage template,
            NotificationDomainType domainType,
            Long domainId,
            Map<String, Object> metadata,
            Object... args) {
        String title = template.getTitle();
        String content = template.formatContent(args);

        // 1. 알림함 저장
        createNotification(userId, title, content, domainType, domainId, metadata);

        // 2. FCM 이벤트 발행 (트랜잭션 커밋 후 비동기 발송)
        eventPublisher.publishEvent(
                new FcmSendEvent(
                        userId, title, content, buildFcmData(domainType, domainId, metadata)));

        log.info("단일 유저 알림 전송 완료 - userId: {}, 템플릿: {}", userId, template.name());
    }

    /**
     * 공지사항 등록 시 대상 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param noticeTitle 등록된 공지사항 제목
     * @param noticeId 등록된 공지사항 ID (알림 domainId로 저장)
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
            // 1. 알림함 저장
            createNotification(userId, title, content, NotificationDomainType.NOTICE, noticeId);

            // 2. FCM 이벤트 발행 (트랜잭션 커밋 후 비동기 발송)
            eventPublisher.publishEvent(
                    new FcmSendEvent(
                            userId,
                            title,
                            content,
                            buildFcmData(NotificationDomainType.NOTICE, noticeId)));
        }

        log.info("공지 알림 전송 완료 - noticeId: {}, 대상 수: {}", noticeId, targetUserIds.size());
    }

    /**
     * 교육(EduReport) 등록 시 대상 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param eduTypeLabel 교육 유형 라벨 (예: SAFETY, PSM, 부서명 등)
     * @param eduTitle 등록된 교육 제목
     * @param reportId 등록된 교육 ID (알림 domainId로 저장)
     * @param targetUserIds 알림을 받을 유저 ID 집합 (리스트 형태)
     */
    @Transactional
    public void sendEduReportAlertToTargets(
            String eduTypeLabel, String eduTitle, Long reportId, List<Long> targetUserIds) {
        sendEduReportAlertToTargets(eduTypeLabel, eduTitle, reportId, targetUserIds, null);
    }

    @Transactional
    public void sendEduReportAlertToTargets(
            String eduTypeLabel,
            String eduTitle,
            Long reportId,
            List<Long> targetUserIds,
            Map<String, Object> metadata) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.info("교육 알림 전송 건너뜀 - 대상 없음, reportId: {}", reportId);
            return;
        }

        String title = NotificationMessage.EDU_REPORT_CREATED.getTitle();
        String content =
                NotificationMessage.EDU_REPORT_CREATED.formatContent(eduTypeLabel, eduTitle);

        for (Long userId : targetUserIds) {
            // 1. 알림함 저장
            createNotification(
                    userId, title, content, NotificationDomainType.EDUCATION, reportId, metadata);

            // 2. FCM 이벤트 발행 (트랜잭션 커밋 후 비동기 발송)
            eventPublisher.publishEvent(
                    new FcmSendEvent(
                            userId,
                            title,
                            content,
                            buildFcmData(NotificationDomainType.EDUCATION, reportId, metadata)));
        }

        log.info("교육 알림 전송 완료 - reportId: {}, 대상 수: {}", reportId, targetUserIds.size());
    }

    /**
     * 방문 이벤트(사전 예약/입실) 발생 시 host 담당 부서 소속 전원에게 FCM 알림 전송 + Notification 저장
     *
     * @param template 알림 메시지 템플릿
     * @param visitId 방문 ID (domainId로 저장)
     * @param hostDepartmentId host 직원의 부서 ID
     * @param contentArgs 템플릿 포맷팅에 사용할 인자
     */
    @Transactional
    public void sendVisitAlertToDepartment(
            NotificationMessage template,
            Long visitId,
            Long hostDepartmentId,
            Object... contentArgs) {
        sendVisitAlertToDepartment(template, visitId, hostDepartmentId, null, contentArgs);
    }

    @Transactional
    public void sendVisitAlertToDepartment(
            NotificationMessage template,
            Long visitId,
            Long hostDepartmentId,
            Map<String, Object> metadata,
            Object... contentArgs) {
        List<Long> targetUserIds = userRepository.findAllIdsByDepartmentId(hostDepartmentId);

        if (targetUserIds.isEmpty()) {
            log.info("방문 알림 전송 건너뜀 - 대상 없음, visitId: {}", visitId);
            return;
        }

        String title = template.getTitle();
        String content = template.formatContent(contentArgs);
        boolean requiresApproval =
                metadata != null && Boolean.TRUE.equals(metadata.get("isApprovalTarget"));

        for (Long userId : targetUserIds) {
            // 1. 알림함 저장
            createNotification(
                    userId,
                    title,
                    content,
                    NotificationDomainType.VISIT,
                    visitId,
                    metadata,
                    requiresApproval);

            // 2. FCM 이벤트 발행 (트랜잭션 커밋 후 비동기 발송)
            eventPublisher.publishEvent(
                    new FcmSendEvent(
                            userId,
                            title,
                            content,
                            buildFcmData(NotificationDomainType.VISIT, visitId, metadata)));
        }

        log.info(
                "방문 알림 전송 완료 - visitId: {}, 대상 수: {}, 템플릿: {}",
                visitId,
                targetUserIds.size(),
                template.name());
    }

    /**
     * 연차 갱신/등록 시 단일 유저에게 알림 전송
     *
     * @param userId 알림 대상 유저 ID
     * @param baseDateFormatted 포맷팅된 기준일 문자열 (예: "2025년 08월 01일")
     */
    @Transactional
    public void sendAnnualLeaveAlertToUser(Long userId, String baseDateFormatted) {
        NotificationMessage template = NotificationMessage.ANNUAL_LEAVE_UPDATED;
        String title = template.getTitle();
        String content = template.formatContent(baseDateFormatted);

        // 1. 알림함 DB 저장 (domainId는 단일 엔티티 연차 특성상 null 처리)
        createNotification(userId, title, content, NotificationDomainType.ANNUAL_LEAVE, null);

        // 2. FCM 이벤트 발행 (트랜잭션 커밋 후 비동기 발송)
        eventPublisher.publishEvent(
                new FcmSendEvent(
                        userId,
                        title,
                        content,
                        buildFcmData(NotificationDomainType.ANNUAL_LEAVE, null)));

        log.info("연차 알림 전송 완료 - userId: {}, 기준일: {}", userId, baseDateFormatted);
    }

    /**
     * 급여명세서 발송 시 단일 유저에게 알림 전송
     *
     * @param userId 알림 대상 유저 ID
     * @param payslipId 발송된 Payslip ID (domainId로 저장)
     */
    @Transactional
    public void sendPayslipAlertToUser(Long userId, Long payslipId) {
        NotificationMessage template = NotificationMessage.PAYSLIP_SENT;
        String title = template.getTitle();
        String content = template.formatContent();

        // 1. 알림함 DB 저장
        createNotification(userId, title, content, NotificationDomainType.PAYSLIP, payslipId);

        // 2. FCM 이벤트 발행 (트랜잭션 커밋 후 비동기 발송)
        eventPublisher.publishEvent(
                new FcmSendEvent(
                        userId,
                        title,
                        content,
                        buildFcmData(NotificationDomainType.PAYSLIP, payslipId)));

        log.info("급여명세서 알림 전송 완료 - userId: {}, payslipId: {}", userId, payslipId);
    }

    /**
     * 전자결재 생성 시 첫 번째 결재자와 참조자(REFERRER)에게 알림 전송
     *
     * @param approvalId 결재 문서 ID
     * @param docTitle 결재 문서 제목
     * @param firstApproverId 첫 번째 결재자 유저 ID
     * @param referrerIds 참조자(REFERRER) 유저 ID 목록
     */
    @Transactional
    public void sendApprovalCreatedAlert(
            Long approvalId, String docTitle, Long firstApproverId, List<Long> referrerIds) {

        // 1. 첫 번째 결재자에게 알림
        String approverTitle = NotificationMessage.APPROVAL_CREATED_APPROVER.getTitle();
        String approverContent =
                NotificationMessage.APPROVAL_CREATED_APPROVER.formatContent(docTitle);
        createNotification(
                firstApproverId,
                approverTitle,
                approverContent,
                NotificationDomainType.APPROVAL,
                approvalId,
                null,
                true);
        eventPublisher.publishEvent(
                new FcmSendEvent(
                        firstApproverId,
                        approverTitle,
                        approverContent,
                        buildFcmData(NotificationDomainType.APPROVAL, approvalId)));
        log.info(
                "전자결재 생성 알림(결재자) 전송 - approvalId: {}, approverId: {}", approvalId, firstApproverId);

        // 2. 참조자들에게 알림
        String referrerTitle = NotificationMessage.APPROVAL_CREATED_REFERRER.getTitle();
        String referrerContent =
                NotificationMessage.APPROVAL_CREATED_REFERRER.formatContent(docTitle);
        for (Long referrerId : referrerIds) {
            createNotification(
                    referrerId,
                    referrerTitle,
                    referrerContent,
                    NotificationDomainType.APPROVAL,
                    approvalId);
            eventPublisher.publishEvent(
                    new FcmSendEvent(
                            referrerId,
                            referrerTitle,
                            referrerContent,
                            buildFcmData(NotificationDomainType.APPROVAL, approvalId)));
        }
        log.info("전자결재 생성 알림(참조자 {}명) 전송 완료 - approvalId: {}", referrerIds.size(), approvalId);
    }

    /**
     * N번째 결재자가 승인 시 N+1번째 결재자에게 알림 전송
     *
     * @param nextApproverId 다음 결재자 유저 ID
     * @param approvalId 결재 문서 ID
     * @param docTitle 결재 문서 제목
     */
    @Transactional
    public void sendApprovalNextStepAlert(Long nextApproverId, Long approvalId, String docTitle) {
        String title = NotificationMessage.APPROVAL_CREATED_APPROVER.getTitle();
        String content = NotificationMessage.APPROVAL_CREATED_APPROVER.formatContent(docTitle);

        createNotification(
                nextApproverId,
                title,
                content,
                NotificationDomainType.APPROVAL,
                approvalId,
                null,
                true);
        eventPublisher.publishEvent(
                new FcmSendEvent(
                        nextApproverId,
                        title,
                        content,
                        buildFcmData(NotificationDomainType.APPROVAL, approvalId)));

        log.info(
                "전자결재 다음 결재자 알림 전송 - approvalId: {}, nextApproverId: {}",
                approvalId,
                nextApproverId);
    }

    /**
     * 전자결재 반려 시 기안자에게 사유와 함께 알림 전송
     *
     * @param drafterId 기안자 유저 ID
     * @param approvalId 결재 문서 ID
     * @param docTitle 결재 문서 제목
     * @param comment 반려 사유
     */
    @Transactional
    public void sendApprovalRejectedAlert(
            Long drafterId, Long approvalId, String docTitle, String comment) {
        String title = NotificationMessage.APPROVAL_REJECTED.getTitle();
        String content = NotificationMessage.APPROVAL_REJECTED.formatContent(docTitle, comment);

        createNotification(drafterId, title, content, NotificationDomainType.APPROVAL, approvalId);
        eventPublisher.publishEvent(
                new FcmSendEvent(
                        drafterId,
                        title,
                        content,
                        buildFcmData(NotificationDomainType.APPROVAL, approvalId)));

        log.info("전자결재 반려 알림 전송 - approvalId: {}, drafterId: {}", approvalId, drafterId);
    }

    /**
     * 전자결재 최종 승인 시 기안자와 열람권자(VIEWER)에게 알림 전송
     *
     * @param approvalId 결재 문서 ID
     * @param docTitle 결재 문서 제목
     * @param drafterId 기안자 유저 ID
     * @param viewerIds 열람권자(VIEWER) 유저 ID 목록
     */
    @Transactional
    public void sendApprovalFinallyApprovedAlert(
            Long approvalId, String docTitle, Long drafterId, List<Long> viewerIds) {
        String title = NotificationMessage.APPROVAL_FINALLY_APPROVED.getTitle();
        String content = NotificationMessage.APPROVAL_FINALLY_APPROVED.formatContent(docTitle);

        // 1. 기안자에게 알림
        createNotification(drafterId, title, content, NotificationDomainType.APPROVAL, approvalId);
        eventPublisher.publishEvent(
                new FcmSendEvent(
                        drafterId,
                        title,
                        content,
                        buildFcmData(NotificationDomainType.APPROVAL, approvalId)));

        // 2. 열람권자들에게 알림
        for (Long viewerId : viewerIds) {
            createNotification(
                    viewerId, title, content, NotificationDomainType.APPROVAL, approvalId);
            eventPublisher.publishEvent(
                    new FcmSendEvent(
                            viewerId,
                            title,
                            content,
                            buildFcmData(NotificationDomainType.APPROVAL, approvalId)));
        }

        log.info(
                "전자결재 최종 승인 알림 전송 - approvalId: {}, drafterId: {}, viewers: {}명",
                approvalId,
                drafterId,
                viewerIds.size());
    }

    /**
     * 안전보건교육 세션 생성 시 대상 유저 전체에게 FCM 알림 전송 + Notification 저장
     *
     * @param sessionId 생성된 세션 ID (domainId로 저장)
     * @param sessionTitle 세션 제목
     * @param targetUserIds 알림을 받을 유저 ID 목록
     */
    @Transactional
    public void sendSafetyTrainingSessionAlertToAttendees(
            Long sessionId, String sessionTitle, List<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            log.info("안전보건교육 알림 전송 건너뜀 - 대상 없음, sessionId: {}", sessionId);
            return;
        }

        String title = NotificationMessage.SAFETY_TRAINING_SESSION_CREATED.getTitle();
        String content =
                NotificationMessage.SAFETY_TRAINING_SESSION_CREATED.formatContent(sessionTitle);
        Map<String, Object> metadata = Map.of("educationType", "SAFETY", "detailType", "SESSION");

        for (Long userId : targetUserIds) {
            createNotification(
                    userId,
                    title,
                    content,
                    NotificationDomainType.SAFETY_TRAINING,
                    sessionId,
                    metadata);
            eventPublisher.publishEvent(
                    new FcmSendEvent(
                            userId,
                            title,
                            content,
                            buildFcmData(
                                    NotificationDomainType.SAFETY_TRAINING, sessionId, metadata)));
        }

        log.info("안전보건교육 세션 알림 전송 완료 - sessionId: {}, 대상 수: {}", sessionId, targetUserIds.size());
    }

    private Map<String, String> buildFcmData(NotificationDomainType domainType, Long domainId) {
        return buildFcmData(domainType, domainId, null);
    }

    private Map<String, String> buildFcmData(
            NotificationDomainType domainType, Long domainId, Map<String, Object> metadata) {
        Map<String, String> data = new HashMap<>();
        data.put("domainType", domainType.name());
        data.put("domainId", domainId != null ? domainId.toString() : "");
        if (metadata != null && !metadata.isEmpty()) {
            try {
                data.put("metadata", objectMapper.writeValueAsString(metadata));
            } catch (JsonProcessingException e) {
                log.error("FCM metadata 직렬화 실패", e);
            }
        }
        return data;
    }
}
