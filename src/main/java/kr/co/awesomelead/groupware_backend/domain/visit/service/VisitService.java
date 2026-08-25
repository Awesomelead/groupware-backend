package kr.co.awesomelead.groupware_backend.domain.visit.service;

import static kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit.hashValue;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationDomainType;
import kr.co.awesomelead.groupware_backend.domain.notification.enums.NotificationMessage;
import kr.co.awesomelead.groupware_backend.domain.notification.service.NotificationService;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
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

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
        normalizeVisitorInfo(visit);
        syncAndValidatePermissions(visit, dto);
        addHostsToVisit(visit, hosts);

        Long visitId = visitRepository.save(visit).getId();

        sendVisitAlerts(
                visit.getPurpose(),
                NotificationMessage.VISIT_ONE_DAY_PRE,
                visitId,
                hosts,
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
        normalizeVisitorInfo(visit);
        syncAndValidatePermissions(visit, dto);
        addHostsToVisit(visit, hosts);

        Long visitId = visitRepository.save(visit).getId();

        sendVisitAlerts(
                visit.getPurpose(),
                NotificationMessage.VISIT_LONG_TERM_PRE,
                visitId,
                hosts,
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
        normalizeVisitorInfo(visit);
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

        sendVisitAlerts(
                visit.getPurpose(),
                NotificationMessage.VISIT_CHECK_IN,
                visitId,
                hosts,
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
        sendVisitAlerts(
                visit.getPurpose(),
                NotificationMessage.VISIT_CHECK_IN,
                visit.getId(),
                hosts,
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
        if (manager.getPosition() != Position.SECURITY_GUARD) {
            validateManagedDepartmentAccess(manager, visit);
        }

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
        record.setExitTimeUpdatedBy(manager);

        return visit.getId();
    }

    @Transactional(readOnly = true)
    public List<MyVisitListResponseDto> getMyVisitList(VisitSearchRequestDto dto) {
        String inputPhoneHash = hashValue(dto.getPhoneNumber());

        List<Visit> visits =
                visitRepository.findByVisitorNameAndPhoneNumberHashOrderByIdDesc(
                        dto.getName(), inputPhoneHash);

        if (visits.isEmpty()) {
            throw new CustomException(ErrorCode.VISIT_NOT_FOUND);
        }

        List<MyVisitListResponseDto> matchedVisits =
                visits.stream()
                        .filter(visit -> StringUtils.hasText(visit.getPassword()))
                        .filter(
                                visit ->
                                        passwordEncoder.matches(
                                                dto.getPassword(), visit.getPassword()))
                        .map(visitMapper::toMyVisitListResponseDto)
                        .toList();

        if (matchedVisits.isEmpty()) {
            throw new CustomException(ErrorCode.VISITOR_AUTHENTICATION_FAILED);
        }

        return matchedVisits;
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
                                record.setExitTimeUpdatedBy(null);
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
        normalizeVisitorInfo(visit);

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

    private void normalizeVisitorInfo(Visit visit) {
        if (visit.getVisitorName() != null) {
            visit.setVisitorName(visit.getVisitorName().strip());
        }
        if (visit.getVisitorCompany() != null) {
            visit.setVisitorCompany(visit.getVisitorCompany().strip());
        }
        if (visit.getCarNumber() != null) {
            visit.setCarNumber(visit.getCarNumber().replaceAll("\\s+", ""));
        }
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
        validateVisitorManageAuthority(userId);

        return visitQueryRepository
                .findVisitsForAdmin(departmentId, status, startDate, endDate, pageable)
                .map(visitMapper::toVisitListResponseDto);
    }

    @Transactional(readOnly = true)
    public byte[] getVisitsExcel(
            Long userId,
            Long departmentId,
            VisitStatus status,
            LocalDate startDate,
            LocalDate endDate) {
        validateVisitorManageAuthority(userId);

        List<Visit> visits =
                visitQueryRepository.findVisitsForAdminExcel(
                        departmentId, status, startDate, endDate);

        return createVisitExcel(visits);
    }

    @Transactional(readOnly = true)
    public MyVisitDetailResponseDto getVisitDetailForAdmin(Long userId, Long visitId) {
        validateVisitorManageAuthority(userId);

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

    private byte[] createVisitExcel(List<Visit> visits) {
        String[] headers = {
            "No.",
            "상태",
            "방문 유형",
            "방문 목적",
            "방문 회사",
            "내방객 회사",
            "내방객 이름",
            "내방객 전화번호",
            "차량번호",
            "담당자",
            "담당부서",
            "방문 시작일",
            "방문 종료일",
            "추가 허가 유형",
            "추가 허가 상세",
            "반려사유",
            "방문일",
            "희망 입실시간",
            "희망 퇴실시간",
            "실제 입실시간",
            "실제 퇴실시간",
            "퇴실 처리자",
            "서명 여부",
            "서명 URL"
        };

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("내방객기록");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            setExcelBorderThin(headerStyle);

            CellStyle dataStyle = workbook.createCellStyle();
            setExcelBorderThin(dataStyle);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                createExcelCell(headerRow, i, headers[i], headerStyle);
            }

            int rowNo = 1;
            for (Visit visit : visits) {
                List<VisitRecord> records =
                        visit.getRecords().stream()
                                .sorted(
                                        Comparator.comparing(
                                                        VisitRecord::getVisitDate,
                                                        Comparator.nullsLast(
                                                                Comparator.naturalOrder()))
                                                .thenComparing(
                                                        VisitRecord::getId,
                                                        Comparator.nullsLast(
                                                                Comparator.naturalOrder())))
                                .toList();

                if (records.isEmpty()) {
                    Row row = sheet.createRow(rowNo);
                    fillVisitExcelRow(row, rowNo++, visit, null, dataStyle);
                    continue;
                }

                for (VisitRecord record : records) {
                    Row row = sheet.createRow(rowNo);
                    fillVisitExcelRow(row, rowNo++, visit, record, dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max((int) (currentWidth * 1.3), 3000));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void fillVisitExcelRow(
            Row row, int rowNo, Visit visit, VisitRecord record, CellStyle dataStyle) {
        int col = 0;
        createExcelCell(row, col++, String.valueOf(rowNo), dataStyle);
        createExcelCell(
                row,
                col++,
                visit.getStatus() != null ? visit.getStatus().getDescription() : "",
                dataStyle);
        createExcelCell(
                row,
                col++,
                visit.getVisitCategory() != null ? visit.getVisitCategory().getDescription() : "",
                dataStyle);
        createExcelCell(
                row,
                col++,
                visit.getPurpose() != null ? visit.getPurpose().getDescription() : "",
                dataStyle);
        createExcelCell(
                row,
                col++,
                visit.getHostCompany() != null ? visit.getHostCompany().getDescription() : "",
                dataStyle);
        createExcelCell(row, col++, visit.getVisitorCompany(), dataStyle);
        createExcelCell(row, col++, visit.getVisitorName(), dataStyle);
        createExcelCell(row, col++, visit.getVisitorPhoneNumber(), dataStyle);
        createExcelCell(row, col++, visit.getCarNumber(), dataStyle);
        createExcelCell(row, col++, getHostNames(visit), dataStyle);
        createExcelCell(row, col++, getHostDepartmentNames(visit), dataStyle);
        createExcelCell(row, col++, formatDate(visit.getStartDate()), dataStyle);
        createExcelCell(row, col++, formatDate(visit.getEndDate()), dataStyle);
        createExcelCell(
                row,
                col++,
                visit.getPermissionType() != null ? visit.getPermissionType().getDescription() : "",
                dataStyle);
        createExcelCell(row, col++, visit.getPermissionDetail(), dataStyle);
        createExcelCell(row, col++, visit.getRejectionReason(), dataStyle);
        createExcelCell(
                row, col++, record != null ? formatDate(record.getVisitDate()) : "", dataStyle);
        createExcelCell(row, col++, formatTime(visit.getPlannedEntryTime()), dataStyle);
        createExcelCell(row, col++, formatTime(visit.getPlannedExitTime()), dataStyle);
        createExcelCell(
                row, col++, record != null ? formatDateTime(record.getEntryTime()) : "", dataStyle);
        createExcelCell(
                row, col++, record != null ? formatDateTime(record.getExitTime()) : "", dataStyle);
        createExcelCell(row, col++, getExitTimeUpdatedByName(record), dataStyle);
        createExcelCell(row, col++, hasSignature(record) ? "Y" : "N", dataStyle);
        createExcelCell(row, col++, getSignatureUrl(record), dataStyle);
    }

    private String getHostNames(Visit visit) {
        return visit.getHosts().stream()
                .map(VisitHost::getUser)
                .filter(user -> user != null)
                .map(User::getDisplayName)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String getHostDepartmentNames(Visit visit) {
        return visit.getHosts().stream()
                .map(VisitHost::getUser)
                .filter(user -> user != null && user.getDepartment() != null)
                .map(user -> user.getDepartment().getName().getDescription())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String getExitTimeUpdatedByName(VisitRecord record) {
        if (record == null || record.getExitTimeUpdatedBy() == null) {
            return "";
        }
        return record.getExitTimeUpdatedBy().getDisplayName();
    }

    private boolean hasSignature(VisitRecord record) {
        return record != null && StringUtils.hasText(record.getSignatureKey());
    }

    private String getSignatureUrl(VisitRecord record) {
        return hasSignature(record) ? s3Service.getFileUrl(record.getSignatureKey()) : "";
    }

    private String formatDate(LocalDate value) {
        return value != null ? value.toString() : "";
    }

    private String formatTime(LocalTime value) {
        return value != null ? value.toString() : "";
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
    }

    private void setExcelBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private void createExcelCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void validateManagedDepartmentAccess(User manager, Visit visit) {
        Department managerDepartment = manager.getDepartment();
        if (managerDepartment == null) {
            throw new CustomException(ErrorCode.VISIT_ACCESS_DENIED);
        }

        boolean hasAccess =
                visit.getHosts().stream()
                        .filter(h -> h.getUser() != null && h.getUser().getDepartment() != null)
                        .anyMatch(
                                h ->
                                        isSameOrAncestorDepartment(
                                                managerDepartment, h.getUser().getDepartment()));

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

    private void sendVisitAlerts(
            VisitPurpose purpose,
            NotificationMessage template,
            Long visitId,
            List<User> hosts,
            Map<String, Object> metadata,
            Object... contentArgs) {
        Set<Long> targetDeptIds =
                hosts.stream()
                        .filter(h -> h.getDepartment() != null)
                        .flatMap(h -> collectAlertTargetDepartmentIds(h.getDepartment()).stream())
                        .collect(Collectors.toCollection(java.util.HashSet::new));

        if (ENVIRONMENT_SAFETY_REQUIRED_PURPOSES.contains(purpose)) {
            departmentRepository
                    .findByName(DepartmentName.ENVIRONMENT_SAFETY)
                    .ifPresent(dept -> targetDeptIds.add(dept.getId()));
        }

        targetDeptIds.forEach(
                deptId ->
                        notificationService.sendVisitAlertToDepartment(
                                template, visitId, deptId, metadata, contentArgs));
    }

    private boolean isSameOrAncestorDepartment(
            Department ancestorCandidate, Department department) {
        if (ancestorCandidate == null || department == null) {
            return false;
        }

        Department cursor = department;
        while (cursor != null) {
            if (ancestorCandidate.getId().equals(cursor.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private Set<Long> collectAlertTargetDepartmentIds(Department hostDepartment) {
        Set<Long> departmentIds = new java.util.HashSet<>();
        Department cursor = hostDepartment;
        while (cursor != null) {
            boolean isRootDepartment = cursor.getParent() == null;
            // Root 부서는 직접 담당자로 선택된 경우에만 알림 대상으로 포함한다.
            if (!isRootDepartment || cursor.getId().equals(hostDepartment.getId())) {
                departmentIds.add(cursor.getId());
            }
            cursor = cursor.getParent();
        }
        return departmentIds;
    }
}
