package kr.co.awesomelead.groupware_backend.domain.visit.service;

import static kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit.hashValue;

import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckInRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitDetailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitUpdateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitProcessRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitRequest;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitRecord;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;
import kr.co.awesomelead.groupware_backend.domain.visit.mapper.VisitMapper;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final VisitMapper visitMapper;
    private final S3Service s3Service;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long registerOneDayPreVisit(OneDayVisitRequestDto dto) {

        User host =
                userRepository
                        .findById(dto.getHostId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        Visit visit = visitMapper.toOneDayVisit(dto, host, encodedPassword);
        syncAndValidatePermissions(visit, dto);

        return visitRepository.save(visit).getId();
    }

    @Transactional
    public Long registerLongTermPreVisit(LongTermVisitRequestDto dto) {

        validateLongTermPeriod(dto.getStartDate(), dto.getEndDate());

        User host =
                userRepository
                        .findById(dto.getHostId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        Visit visit = visitMapper.toLongTermVisit(dto, host, encodedPassword);
        syncAndValidatePermissions(visit, dto);

        return visitRepository.save(visit).getId();
    }

    @Transactional
    public Long registerOnSiteVisit(OnSiteVisitRequestDto dto) throws IOException {

        User host =
                userRepository
                        .findById(dto.getHostId())
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String signatureKey = s3Service.uploadFile(dto.getSignatureFile());

        Visit visit = visitMapper.toOnSiteVisit(dto, host, encodedPassword);
        syncAndValidatePermissions(visit, dto);

        VisitRecord record =
                VisitRecord.builder()
                        .visit(visit)
                        .visitDate(LocalDate.now())
                        .entryTime(LocalDateTime.now())
                        .exitTime(null) // 현장 방문 시점에는 퇴실 시간이 없음
                        .signatureKey(signatureKey)
                        .build();

        visit.getRecords().add(record);

        return visitRepository.save(visit).getId();
    }

    private void syncAndValidatePermissions(Visit visit, VisitRequest dto) {
        // 1. 매퍼가 놓칠 수 있는 필드 바인딩 (수정 시 null 체크 포함)
        if (dto.getPurpose() != null) {
            visit.setPurpose(dto.getPurpose());
        }
        if (dto.getPermissionType() != null) {
            visit.setPermissionType(dto.getPermissionType());
        }

        // 2. '기타 허가'가 아닐 경우 기존 상세내용 초기화 (데이터 정합성)
        if (visit.getPermissionType() != AdditionalPermissionType.OTHER_PERMISSION) {
            visit.setPermissionDetail(null);
        } else if (StringUtils.hasText(dto.getPermissionDetail())) {
            visit.setPermissionDetail(dto.getPermissionDetail());
        }

        // 3. 비즈니스 규칙 통합 검증 (엔티티의 최종 상태 기준)
        // 규칙 1: 시설공사는 보충적 허가 필수
        if (visit.getPurpose() == VisitPurpose.FACILITY_CONSTRUCTION) {
            if (visit.getPermissionType() == null
                    || visit.getPermissionType() == AdditionalPermissionType.NONE) {
                throw new CustomException(ErrorCode.ADDITIONAL_PERMISSION_REQUIRED);
            }
        }

        // 규칙 2: '기타 허가' 선택 시 상세 내용 필수
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
        // 시작일 기준 정확히 3개월 뒤 날짜 계산
        LocalDate maxEndDate = startDate.plusMonths(3);
        if (endDate.isAfter(maxEndDate)) {
            throw new CustomException(ErrorCode.LONG_TERM_PERIOD_EXCEEDED);
        }
    }

    // 방문처리
    @Transactional
    public Long checkIn(CheckInRequestDto dto) throws IOException {
        // 1. 방문 신청 건 조회
        Visit visit =
                visitRepository
                        .findById(dto.getVisitId())
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(dto.getPassword(), visit.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 날짜 검증 (하루 방문의 경우 오늘 날짜인지 확인)
        if (!visit.isLongTerm() && !visit.getStartDate().equals(LocalDate.now())) {
            throw new CustomException(ErrorCode.NOT_VISIT_DATE); // "방문 예정일이 아닙니다" 에러
        }

        if (visit.isVisited()) {
            throw new CustomException(ErrorCode.VISIT_ALREADY_CHECKED_OUT);
        }

        // 5. 서명 이미지 S3 업로드
        String signatureKey = s3Service.uploadFile(dto.getSignatureFile());

        // 6. 입실 기록(VisitRecord) 생성
        VisitRecord record =
                VisitRecord.builder()
                        .visit(visit)
                        .visitDate(LocalDate.now())
                        .entryTime(LocalDateTime.now())
                        .signatureKey(signatureKey)
                        .build();

        visit.getRecords().add(record);

        // 7. 방문 상태 업데이트
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisited(true);

        return visit.getId();
    }

    @Transactional
    public Long checkOut(Long userId, CheckOutRequestDto dto) {
        validateAdminAuthority(userId);

        Visit visit =
                visitRepository
                        .findById(dto.getVisitId())
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        VisitRecord record =
                visit.getRecords().stream()
                        .filter(r -> r.getId().equals(dto.getVisitRecordId()))
                        .findFirst()
                        .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));

        boolean isInitialCheckOut = (record.getExitTime() == null);

        // 상태 변경 로직
        if (isInitialCheckOut) {
            // 최초 퇴실 처리 시에는 입실을 한 상태인지 확인
            if (visit.getStatus() != VisitStatus.IN_PROGRESS) {
                throw new CustomException(ErrorCode.NOT_IN_PROGRESS);
            }
            if (visit.isLongTerm()) {
                visit.setStatus(VisitStatus.APPROVED); // 다음 입실을 위해 다시 승인 상태로
            } else {
                visit.setStatus(VisitStatus.COMPLETED); // 단기 방문은 최종 완료
            }
        }

        if (dto.getCheckOutTime().isBefore(record.getEntryTime())) {
            throw new CustomException(
                    ErrorCode.INVALID_CHECKOUT_TIME); // "퇴실 시간은 입실 시간보다 빠를 수 없습니다."
        }

        record.setExitTime(dto.getCheckOutTime());

        return visit.getId();
    }

    // 내 방문 목록 조회
    @Transactional(readOnly = true)
    public List<MyVisitListResponseDto> getMyVisitList(VisitSearchRequestDto dto) {
        String inputPhoneHash = hashValue(dto.getPhoneNumber());

        List<Visit> visits =
                visitRepository.findByVisitorNameAndPhoneNumberHash(dto.getName(), inputPhoneHash);

        return visits.stream()
                .filter(
                        visit ->
                                StringUtils.hasText(
                                        visit.getPassword())) // 비밀번호가 있는 건만 대상 (현장 내방 제외)
                .filter(
                        visit ->
                                passwordEncoder.matches(
                                        dto.getPassword(), visit.getPassword())) // 비번 일치 확인
                .map(visitMapper::toMyVisitListResponseDto)
                .toList();
    }

    // 내 방문 상세 조회
    @Transactional(readOnly = true)
    public MyVisitDetailResponseDto getMyVisitDetail(Long visitId, MyVisitDetailRequestDto dto) {
        // 1. 존재 여부 확인
        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(dto.getPassword(), visit.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 엔티티 -> DTO 변환 (시간 계산은 매퍼의 default 메서드가 처리)
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

        // 수정 가능 상태 검증 (하루: NOT_VISITED, 장기: PENDING)
        validateUpdateStatus(visit);

        if (visit.isLongTerm()) {

            LocalDate effectiveStart =
                    (dto.getStartDate() != null) ? dto.getStartDate() : visit.getStartDate();
            LocalDate effectiveEnd =
                    (dto.getEndDate() != null) ? dto.getEndDate() : visit.getEndDate();

            validateLongTermPeriod(effectiveStart, effectiveEnd);
            // 장기 방문은 수정 후 다시 승인 대기 상태로 변경
            visit.setStatus(VisitStatus.PENDING);
        }

        visitMapper.updateVisitFromDto(dto, visit);

        if (!visit.isLongTerm()) {
            visit.setEndDate(visit.getStartDate());
        }

        syncAndValidatePermissions(visit, dto);
    }

    private void validateUpdateStatus(Visit visit) {
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            throw new CustomException(ErrorCode.INVALID_VISIT_STATUS); // "완료된 방문은 수정할 수 없습니다."
        }

        if (visit.isVisited()) {
            throw new CustomException(ErrorCode.INVALID_VISIT_STATUS);
        }

        if (visit.isLongTerm()) {
            if (visit.getStatus() != VisitStatus.PENDING
                    && visit.getStatus() != VisitStatus.APPROVED) {
                throw new CustomException(ErrorCode.INVALID_VISIT_STATUS);
            }
        } else {
            // 하루 방문은 입실 전(NOT_VISITED) 상태여야 함
            if (visit.getStatus() != VisitStatus.NOT_VISITED) {
                throw new CustomException(ErrorCode.INVALID_VISIT_STATUS); // "방문 전 상태에서만 수정 가능합니다."
            }
        }
    }

    // 직용원 방문 목록 조회
    @Transactional(readOnly = true)
    public List<VisitListResponseDto> getVisitsForAdmin(
            Long userId, Long departmentId, VisitStatus status) {
        // 관리 권한 확인
        validateAdminAuthority(userId);

        List<Visit> visits = visitRepository.findAllByFilters(departmentId, status);

        return visitMapper.toVisitListResponseDtos(visits);
    }

    // 직원용 방문 상세 조회
    @Transactional(readOnly = true)
    public VisitDetailResponseDto getVisitDetailForAdmin(Long userId, Long visitId) {

        validateAdminAuthority(userId);

        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        VisitDetailResponseDto responseDto = visitMapper.toVisitDetailResponseDto(visit);

        if (responseDto.getRecords() != null) {
            responseDto
                    .getRecords()
                    .forEach(
                            recordDto -> {
                                if (StringUtils.hasText(recordDto.getSignatureUrl())) {
                                    recordDto.setSignatureUrl(
                                            s3Service.getFileUrl(recordDto.getSignatureUrl()));
                                }
                            });
        }

        return responseDto;
    }

    private void validateAdminAuthority(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // MANAGEMENT 직군이고 방문 관리 권한이 있는지 확인
        if (user.getJobType() != JobType.MANAGEMENT || !user.hasAuthority(Authority.ACCESS_VISIT)) {
            throw new CustomException(ErrorCode.VISIT_ACCESS_DENIED);
        }
    }

    // 직원용 사전 장기방문 승인 및 반려
    @Transactional
    public void processVisit(Long userId, Long visitId, VisitProcessRequestDto dto) {
        // 1. 관리 권한 확인 (MANAGEMENT 직군 & ACCESS_VISIT 권한)
        validateAdminAuthority(userId);

        // 2. 방문 신청 건 조회
        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        // 3. 승인 가능한 상태인지 검증
        // - 장기 방문이어야 함
        // - 현재 상태가 PENDING(승인 대기)이어야 함
        if (!visit.isLongTerm()) {
            throw new CustomException(ErrorCode.NOT_LONG_TERM_VISIT); // "장기 방문 건이 아닙니다."
        }

        if (visit.getStatus() != VisitStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_VISIT_STATUS); // "승인 가능한 상태가 아닙니다."
        }

        // 4. 반려 시 사유 필수 검증
        if (dto.getStatus() == VisitStatus.REJECTED
                && !StringUtils.hasText(dto.getRejectionReason())) {
            throw new CustomException(ErrorCode.REJECTION_REASON_REQUIRED);
        }

        // 5. 상태 변경 처리
        visit.process(dto.getStatus(), dto.getRejectionReason());
    }
}
