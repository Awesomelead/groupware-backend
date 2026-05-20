package kr.co.awesomelead.groupware_backend.domain.visit.service;

import static kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit.hashValue;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckInRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitProcessRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitRequest;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitHost;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitRecord;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitCategory;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;
import kr.co.awesomelead.groupware_backend.domain.visit.mapper.VisitMapper;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.querydsl.VisitQueryRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {
    private static final DateTimeFormatter NOTIFICATION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final Set<VisitPurpose> ENVIRONMENT_SAFETY_REQUIRED_PURPOSES =
            EnumSet.of(
                    VisitPurpose.CUSTOMER_INSPECTION,
                    VisitPurpose.HAZARDOUS_SUBSTANCE,
                    VisitPurpose.FACILITY_CONSTRUCTION);

    private final VisitRepository visitRepository;
    private final VisitQueryRepository visitQueryRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final VisitMapper visitMapper;
    private final S3Service s3Service;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public Long registerOneDayPreVisit(OneDayVisitRequestDto dto) {
        List<User> hosts = findUsersByIds(dto.getHostIds());
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        Visit visit =
                visitMapper.toOneDayVisit(dto, hosts.get(0).getWorkLocation(), encodedPassword);
        syncAndValidatePermissions(visit, dto);
        addHostsToVisit(visit, hosts);

        Long visitId = visitRepository.save(visit).getId();

        sendVisitAlertToHostDepartments(
                NotificationMessage.VISIT_ONE_DAY_PRE,
                visitId,
                hosts,
                Map.of("isApprovalTarget", false, "status", VisitStatus.PENDING.name()),
                visit.getVisitorName(),
                visit.getStartDate(),
                dto.getPlannedEntryTime(),
                hosts.get(0).getDisplayName());

        sendEnvironmentSafetyAlertIfRequired(
                visit.getPurpose(),
                NotificationMessage.VISIT_ONE_DAY_PRE,
                visitId,
                Map.of("isApprovalTarget", false, "status", VisitStatus.PENDING.name()),
                visit.getVisitorName(),
                visit.getStartDate(),
                dto.getPlannedEntryTime(),
                hosts.get(0).getDisplayName());

        return visitId;
    }

    @Transactional
    public Long registerLongTermPreVisit(LongTermVisitRequestDto dto) {
        validateLongTermPeriod(dto.getStartDate(), dto.getEndDate());

        List<User> hosts = findUsersByIds(dto.getHostIds());
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        Visit visit =
                visitMapper.toLongTermVisit(dto, hosts.get(0).getWorkLocation(), encodedPassword);
        syncAndValidatePermissions(visit, dto);
        addHostsToVisit(visit, hosts);

        Long visitId = visitRepository.save(visit).getId();

        sendVisitAlertToHostDepartments(
                NotificationMessage.VISIT_LONG_TERM_PRE,
                visitId,
                hosts,
                Map.of("status", VisitStatus.PENDING.name(), "isApprovalTarget", true),
                visit.getVisitorName(),
                dto.getStartDate(),
                dto.getEndDate());

        sendEnvironmentSafetyAlertIfRequired(
                visit.getPurpose(),
                NotificationMessage.VISIT_LONG_TERM_PRE,
                visitId,
                Map.of("status", VisitStatus.PENDING.name(), "isApprovalTarget", true),
                visit.getVisitorName(),
                dto.getStartDate(),
                dto.getEndDate());

        return visitId;
    }

    @Transactional
    public Long registerOnSiteVisit(OnSiteVisitRequestDto dto) throws IOException {
        List<User> hosts = findUsersByIds(dto.getHostIds());
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String signatureKey = s3Service.uploadFile(dto.getSignatureFile());

        Visit visit =
                visitMapper.toOnSiteVisit(dto, hosts.get(0).getWorkLocation(), encodedPassword);
        syncAndValidatePermissions(visit, dto);
        addHostsToVisit(visit, hosts);

        VisitRecord record =
                VisitRecord.builder()
                        .visit(visit)
                        .visitDate(LocalDate.now())
                        .entryTime(LocalDateTime.now())
                        .exitTime(null)
                        .signatureKey(signatureKey)
                        .build();

        visit.getRecords().add(record);

        Long visitId = visitRepository.save(visit).getId();

        sendVisitAlertToHostDepartments(
                NotificationMessage.VISIT_CHECK_IN,
                visitId,
                hosts,
                Map.of("isApprovalTarget", false, "status", VisitStatus.IN_PROGRESS.name()),
                visit.getVisitorName(),
                formatNotificationTime(record.getEntryTime().toLocalTime()));

        sendEnvironmentSafetyAlertIfRequired(
                visit.getPurpose(),
                NotificationMessage.VISIT_CHECK_IN,
                visitId,
                Map.of("isApprovalTarget", false, "status", VisitStatus.IN_PROGRESS.name()),
                visit.getVisitorName(),
                formatNotificationTime(record.getEntryTime().toLocalTime()));

        return visitId;
    }

    private void syncAndValidatePermissions(Visit visit, VisitRequest dto) {
        if (dto.getPurpose() != null) {
            visit.setPurpose(dto.getPurpose());
        }
        if (dto.getPermissionType() != null) {
            visit.setPermissionType(dto.getPermissionType());
        }

        if (visit.getPermissionType() != AdditionalPermissionType.OTHER_PERMISSION) {
            visit.setPermissionDetail(null);
        } else if (StringUtils.hasText(dto.getPermissionDetail())) {
            visit.setPermissionDetail(dto.getPermissionDetail());
        }

        if (visit.getPurpose() == VisitPurpose.FACILITY_CONSTRUCTION) {
            if (visit.getPermissionType() == null
                    || visit.getPermissionType() == AdditionalPermissionType.NONE) {
                throw new CustomException(ErrorCode.ADDITIONAL_PERMISSION_REQUIRED);
            }
        }

        if (visit.getPermissionType() == AdditionalPermissionType.OTHER_PERMISSION) {
            if (!StringUtils.hasText(visit.getPermissionDetail())) {
                throw new CustomException(ErrorCode.PERMISSION_DETAIL_REQUIRED);
            }
        }
    }

    private void validateLongTermPeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.INVALID_VISIT_DATE_RANGE);
        }
        LocalDate maxEndDate = startDate.plusMonths(3);
        if (endDate.isAfter(maxEndDate)) {
            throw new CustomException(ErrorCode.LONG_TERM_PERIOD_EXCEEDED);
        }
    }

    private String formatNotificationTime(LocalTime time) {
        return time.format(NOTIFICATION_TIME_FORMATTER);
    }

    @Transactional
    public Long checkIn(CheckInRequestDto dto) throws IOException {
        Visit visit =
                visitRepository
                        .findById(dto.getVisitId())
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        visit.validateCheckInEligible();

        String signatureKey = s3Service.uploadFile(dto.getSignatureFile());

        VisitRecord record =
                VisitRecord.builder()
                        .visit(visit)
                        .visitDate(LocalDate.now())
                        .entryTime(LocalDateTime.now())
                        .signatureKey(signatureKey)
                        .build();

        visit.getRecords().add(record);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisited(true);

        List<User> hosts = visit.getHosts().stream().map(VisitHost::getUser).toList();
        sendVisitAlertToHostDepartments(
                NotificationMessage.VISIT_CHECK_IN,
                visit.getId(),
                hosts,
                Map.of("isApprovalTarget", false, "status", VisitStatus.IN_PROGRESS.name()),
                visit.getVisitorName(),
                formatNotificationTime(record.getEntryTime().toLocalTime()));

        sendEnvironmentSafetyAlertIfRequired(
                visit.getPurpose(),
                NotificationMessage.VISIT_CHECK_IN,
                visit.getId(),
                Map.of("isApprovalTarget", false, "status", VisitStatus.IN_PROGRESS.name()),
                visit.getVisitorName(),
                formatNotificationTime(record.getEntryTime().toLocalTime()));

        return visit.getId();
    }

    @Transactional
    public Long checkOut(Long userId, CheckOutRequestDto dto) {
        Visit visit =
                visitRepository
                        .findById(dto.getVisitId())
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));
        User manager = validateVisitorManageAuthority(userId);
        validateManagedDepartmentAccess(manager, visit);

        VisitRecord record =
                visit.getRecords().stream()
                        .filter(r -> r.getId().equals(dto.getVisitRecordId()))
                        .findFirst()
                        .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));

        boolean isInitialCheckOut = (record.getExitTime() == null);

        if (isInitialCheckOut) {
            if (visit.getStatus() != VisitStatus.IN_PROGRESS) {
                throw new CustomException(ErrorCode.NOT_IN_PROGRESS);
            }
            if (visit.getVisitCategory() == VisitCategory.PRE_LONG_TERM) {
                visit.setStatus(VisitStatus.APPROVED);
            } else {
                visit.setStatus(VisitStatus.COMPLETED);
            }
        }

        if (dto.getCheckOutTime().isBefore(record.getEntryTime())) {
            throw new CustomException(ErrorCode.INVALID_CHECKOUT_TIME);
        }

        record.setExitTime(dto.getCheckOutTime());

        return visit.getId();
    }

    @Transactional(readOnly = true)
    public List<MyVisitListResponseDto> getMyVisitList(VisitSearchRequestDto dto) {
        String inputPhoneHash = hashValue(dto.getPhoneNumber());

        List<Visit> visits =
                visitRepository.findByVisitorNameAndPhoneNumberHashOrderByIdDesc(
                        dto.getName(), inputPhoneHash);

        return visits.stream()
                .filter(visit -> StringUtils.hasText(visit.getPassword()))
                .filter(visit -> passwordEncoder.matches(dto.getPassword(), visit.getPassword()))
                .map(visitMapper::toMyVisitListResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MyVisitDetailResponseDto getMyVisitDetail(Long visitId) {
        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        MyVisitDetailResponseDto responseDto = visitMapper.toMyVisitDetailResponseDto(visit);

        if (responseDto.getRecords() != null) {
            responseDto
                    .getRecords()
                    .forEach(
                            record -> {
                                if (StringUtils.hasText(record.getSignatureUrl())) {
                                    record.setSignatureUrl(
                                            s3Service.getFileUrl(record.getSignatureUrl()));
                                }
                            });
        }
        return responseDto;
    }

    @Transactional
    public void updateMyVisit(Long visitId, MyVisitUpdateRequestDto dto) {
        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        if (!passwordEncoder.matches(dto.getPassword(), visit.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        validateUpdateStatus(visit);

        if (visit.getVisitCategory() == VisitCategory.PRE_LONG_TERM) {
            LocalDate effectiveStart =
                    (dto.getStartDate() != null) ? dto.getStartDate() : visit.getStartDate();
            LocalDate effectiveEnd =
                    (dto.getEndDate() != null) ? dto.getEndDate() : visit.getEndDate();

            validateLongTermPeriod(effectiveStart, effectiveEnd);
            visit.setStatus(VisitStatus.PENDING);
        }

        visitMapper.updateVisitFromDto(dto, visit);

        if (dto.getHostIds() != null && !dto.getHostIds().isEmpty()) {
            List<User> newHosts = findUsersByIds(dto.getHostIds());
            visit.getHosts().clear();
            addHostsToVisit(visit, newHosts);
        }

        if (dto.getVisitorPhoneNumber() != null) {
            visit.setVisitorPhoneNumber(dto.getVisitorPhoneNumber());
            visit.setPhoneNumberHash(hashValue(dto.getVisitorPhoneNumber()));
        }

        if (visit.getVisitCategory() != VisitCategory.PRE_LONG_TERM) {
            visit.setEndDate(visit.getStartDate());
        }

        syncAndValidatePermissions(visit, dto);
    }

    private void validateUpdateStatus(Visit visit) {
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            throw new CustomException(ErrorCode.INVALID_VISIT_STATUS);
        }

        if (visit.isVisited()) {
            throw new CustomException(ErrorCode.INVALID_VISIT_STATUS);
        }

        if (visit.getVisitCategory() == VisitCategory.PRE_LONG_TERM) {
            if (visit.getStatus() != VisitStatus.PENDING
                    && visit.getStatus() != VisitStatus.APPROVED) {
                throw new CustomException(ErrorCode.INVALID_VISIT_STATUS);
            }
        } else {
            if (visit.getStatus() != VisitStatus.NOT_VISITED) {
                throw new CustomException(ErrorCode.INVALID_VISIT_STATUS);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<VisitListResponseDto> getVisitsForAdmin(
            Long userId,
            Long departmentId,
            VisitStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        validateEmployeeAccess(userId);

        return visitQueryRepository
                .findVisitsForAdmin(departmentId, status, startDate, endDate, pageable)
                .map(visitMapper::toVisitListResponseDto);
    }

    @Transactional(readOnly = true)
    public MyVisitDetailResponseDto getVisitDetailForAdmin(Long userId, Long visitId) {
        validateEmployeeAccess(userId);

        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        MyVisitDetailResponseDto responseDto = visitMapper.toMyVisitDetailResponseDto(visit);

        if (responseDto.getRecords() != null) {
            responseDto
                    .getRecords()
                    .forEach(
                            record -> {
                                if (StringUtils.hasText(record.getSignatureUrl())) {
                                    record.setSignatureUrl(
                                            s3Service.getFileUrl(record.getSignatureUrl()));
                                }
                            });
        }

        return responseDto;
    }

    private void validateEmployeeAccess(Long userId) {
        userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private User validateVisitorManageAuthority(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.hasAuthority(Authority.MANAGE_VISITOR)) {
            throw new CustomException(ErrorCode.VISIT_ACCESS_DENIED);
        }
        return user;
    }

    private void validateManagedDepartmentAccess(User manager, Visit visit) {
        Long managerDepartmentId =
                manager.getDepartment() != null ? manager.getDepartment().getId() : null;

        if (managerDepartmentId == null) {
            throw new CustomException(ErrorCode.VISIT_ACCESS_DENIED);
        }

        boolean hasAccess =
                visit.getHosts().stream()
                        .filter(h -> h.getUser() != null && h.getUser().getDepartment() != null)
                        .anyMatch(
                                h ->
                                        h.getUser()
                                                .getDepartment()
                                                .getId()
                                                .equals(managerDepartmentId));

        if (!hasAccess) {
            throw new CustomException(ErrorCode.VISIT_ACCESS_DENIED);
        }
    }

    @Transactional
    public void processVisit(Long userId, Long visitId, VisitProcessRequestDto dto) {
        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));
        User manager = validateVisitorManageAuthority(userId);
        validateManagedDepartmentAccess(manager, visit);

        if (visit.getVisitCategory() != VisitCategory.PRE_LONG_TERM) {
            throw new CustomException(ErrorCode.NOT_LONG_TERM_VISIT);
        }

        if (visit.getStatus() != VisitStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_VISIT_STATUS);
        }

        if (dto.getStatus() == VisitStatus.REJECTED
                && !StringUtils.hasText(dto.getRejectionReason())) {
            throw new CustomException(ErrorCode.REJECTION_REASON_REQUIRED);
        }

        visit.process(dto.getStatus(), dto.getRejectionReason());

        notificationService.resolveRequiresApproval(NotificationDomainType.VISIT, visitId);
    }

    // --- Helper methods ---

    private List<User> findUsersByIds(List<Long> hostIds) {
        return hostIds.stream()
                .map(
                        id ->
                                userRepository
                                        .findById(id)
                                        .orElseThrow(
                                                () ->
                                                        new CustomException(
                                                                ErrorCode.USER_NOT_FOUND)))
                .toList();
    }

    private void addHostsToVisit(Visit visit, List<User> hostUsers) {
        hostUsers.forEach(
                user -> visit.getHosts().add(VisitHost.builder().visit(visit).user(user).build()));
    }

    private void sendVisitAlertToHostDepartments(
            NotificationMessage template,
            Long visitId,
            List<User> hosts,
            Map<String, Object> metadata,
            Object... contentArgs) {
        hosts.stream()
                .filter(h -> h.getDepartment() != null)
                .map(h -> h.getDepartment().getId())
                .collect(Collectors.toSet())
                .forEach(
                        deptId ->
                                notificationService.sendVisitAlertToDepartment(
                                        template, visitId, deptId, metadata, contentArgs));
    }

    private void sendEnvironmentSafetyAlertIfRequired(
            VisitPurpose purpose,
            NotificationMessage template,
            Long visitId,
            Map<String, Object> metadata,
            Object... contentArgs) {
        if (!ENVIRONMENT_SAFETY_REQUIRED_PURPOSES.contains(purpose)) {
            return;
        }
        departmentRepository
                .findByName(DepartmentName.ENVIRONMENT_SAFETY)
                .ifPresent(
                        dept ->
                                notificationService.sendVisitAlertToDepartment(
                                        template, visitId, dept.getId(), metadata, contentArgs));
    }
}
