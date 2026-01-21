package kr.co.awesomelead.groupware_backend.domain.visit.service;

import static kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit.hashValue;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckInRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.LongTermVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.MyVisitDetailRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OneDayVisitRequestDto;
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

        validateVisitPermissions(dto);

        User host = userRepository.findById(dto.getHostId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        Visit visit = visitMapper.toOneDayVisit(dto, host, encodedPassword);
        applyAndValidateVisitPermissions(visit, dto);

        return visitRepository.save(visit).getId();
    }

    @Transactional
    public Long registerLongTermPreVisit(LongTermVisitRequestDto dto) {

        validateLongTermPeriod(dto.getStartDate(), dto.getEndDate());
        validateVisitPermissions(dto);

        User host = userRepository.findById(dto.getHostId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        Visit visit = visitMapper.toLongTermVisit(dto, host, encodedPassword);
        applyAndValidateVisitPermissions(visit, dto);

        return visitRepository.save(visit).getId();
    }

    @Transactional
    public Long registerOnSiteVisit(OnSiteVisitRequestDto dto) throws IOException {
        validateVisitPermissions(dto);

        User host = userRepository.findById(dto.getHostId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        String signatureKey = s3Service.uploadFile(dto.getSignatureFile());

        Visit visit = visitMapper.toOnSiteVisit(dto, host, encodedPassword);
        visit.setStartDate(LocalDate.now());
        visit.setEndDate(LocalDate.now());
        visit.setPlannedEntryTime(LocalTime.now());

        applyAndValidateVisitPermissions(visit, dto);

        VisitRecord record = VisitRecord.builder()
            .visit(visit)
            .visitDate(LocalDate.now())
            .entryTime(LocalDateTime.now())
            .exitTime(null) // 현장 방문 시점에는 퇴실 시간이 없음
            .signatureKey(signatureKey)
            .build();

        visit.getRecords().add(record);

        return visitRepository.save(visit).getId();
    }

    private void validateVisitPermissions(VisitRequest dto) {
        // 1. null일 경우 NONE으로 간주하거나 변수에 할당
        AdditionalPermissionType type = dto.getPermissionType();

        // 2. 시설공사 시 허가 필수 체크
        if (dto.getPurpose() == VisitPurpose.FACILITY_CONSTRUCTION) {
            // type이 null이거나 NONE인 경우 모두 예외 처리
            if (type == null || type == AdditionalPermissionType.NONE) {
                throw new CustomException(ErrorCode.ADDITIONAL_PERMISSION_REQUIRED);
            }
        }

        // 3. '기타 허가' 선택 시 상세 내용 필수 체크
        if (type == AdditionalPermissionType.OTHER_PERMISSION) {
            // StringUtils.hasText()를 사용하면 null, 공백, 빈 문자열을 한 번에 체크 가능
            if (!StringUtils.hasText(dto.getPermissionDetail())) {
                throw new CustomException(ErrorCode.PERMISSION_DETAIL_REQUIRED);
            }
        }
    }

    private void applyAndValidateVisitPermissions(Visit visit, VisitRequest dto) {
        // 1. 목적 매핑 (이미 매퍼에서 되었을 수 있지만 명시적으로 관리 가능)
        visit.setPurpose(dto.getPurpose());

        // 2. 목적이 시설 공사이면 보충적 허가 필수 여부 체크 및 매핑
        if (visit.getPurpose() == VisitPurpose.FACILITY_CONSTRUCTION) {
            if (dto.getPermissionType() == null
                || dto.getPermissionType() == AdditionalPermissionType.NONE) {
                throw new CustomException(ErrorCode.ADDITIONAL_PERMISSION_REQUIRED);
            }
        }
        visit.setPermissionType(dto.getPermissionType());

        // 3. 보충적 허가가 '기타 허가'이면 상세 내용 체크 및 매핑
        if (visit.getPermissionType() == AdditionalPermissionType.OTHER_PERMISSION) {
            if (!StringUtils.hasText(dto.getPermissionDetail())) {
                throw new CustomException(ErrorCode.PERMISSION_DETAIL_REQUIRED);
            }
            visit.setPermissionDetail(dto.getPermissionDetail());
        } else {
            // 기타 허가가 아니면 기존에 들어있을지 모를 상세 내용을 null로 비워줌 (데이터 정합성)
            visit.setPermissionDetail(null);
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
        Visit visit = visitRepository.findById(dto.getVisitId())
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
        VisitRecord record = VisitRecord.builder()
            .visit(visit)
            .visitDate(LocalDate.now())
            .entryTime(LocalDateTime.now())
            .signatureKey(signatureKey)
            .build();

        visit.getRecords().add(record);

        // 7. 방문 상태 업데이트
        visit.setVisited(true);

        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisited(true);

        return visit.getId();
    }

    @Transactional
    public Long checkOut(Long userId, CheckOutRequestDto dto) {
        validateAdminAuthority(userId);

        Visit visit = visitRepository.findById(dto.getVisitId())
            .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        // 1. 현재 입실 중인지 확인
        if (visit.getStatus() != VisitStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.NOT_IN_PROGRESS);
        }

        // 2. 퇴실 시간이 찍히지 않은 가장 최근 기록 찾기
        VisitRecord record = visit.getRecords().stream()
            .filter(r -> r.getExitTime() == null)
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.RECORD_NOT_FOUND));

        // 3. 퇴실 시간 기록
        record.setExitTime(dto.getCheckOutTime());

        // 4. 상태 변경 로직
        if (visit.isLongTerm()) {
            // 장기 방문자는 다시 '승인 완료(대기)' 상태로
            visit.setStatus(VisitStatus.APPROVED);
        } else {
            // 하루/현장 방문자는 '방문 완료'로 종료
            visit.setStatus(VisitStatus.COMPLETED);
        }

        return visit.getId();
    }

    // 내 방문 목록 조회
    @Transactional(readOnly = true)
    public List<MyVisitListResponseDto> getMyVisitList(VisitSearchRequestDto dto) {
        String inputPhoneHash = hashValue(dto.getPhoneNumber());

        List<Visit> visits = visitRepository.findByVisitorNameAndPhoneNumberHash(
            dto.getName(), inputPhoneHash);

        return visits.stream()
            .filter(visit -> StringUtils.hasText(visit.getPassword())) // 비밀번호가 있는 건만 대상 (현장 내방 제외)
            .filter(visit -> passwordEncoder.matches(dto.getPassword(),
                visit.getPassword())) // 비번 일치 확인
            .map(visitMapper::toMyVisitListResponseDto)
            .toList();
    }

    // 내 방문 상세 조회
    @Transactional(readOnly = true)
    public MyVisitDetailResponseDto getMyVisitDetail(Long visitId, MyVisitDetailRequestDto dto) {
        // 1. 존재 여부 확인
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new RuntimeException("해당 방문 기록이 존재하지 않습니다."));

        // 2. 비밀번호 검증 (IDOR 방어 핵심 로직)
        if (!passwordEncoder.matches(dto.getPassword(), visit.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않아 정보를 조회할 수 없습니다.");
        }

        // 3. 엔티티 -> DTO 변환 (시간 계산은 매퍼의 default 메서드가 처리)
        return visitMapper.toMyVisitDetailResponseDto(visit);
    }

    // 직용원 방문 목록 조회
    @Transactional(readOnly = true)
    public List<VisitListResponseDto> getVisitsForAdmin(Long userId, Long departmentId,
        VisitStatus status) {
        //관리 권한 확인
        validateAdminAuthority(userId);

        List<Visit> visits = visitRepository.findAllByFilters(departmentId, status);

        return visitMapper.toVisitListResponseDtos(visits);
    }

    // 직원용 방문 상세 조회
    public VisitDetailResponseDto getVisitDetailForAdmin(Long userId, Long visitId) {
        // 1. 관리 권한 확인
        validateAdminAuthority(userId);

        // 2. 방문 건 조회
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        // 3. DTO 변환
        VisitDetailResponseDto responseDto = visitMapper.toVisitDetailResponseDto(visit);

        // 4. 서명 이미지 키를 S3 URL로 치환 (필요 시)
        // 만약 Mapper에서 처리하지 못했다면 여기서 서명 URL들을 세팅해줍니다.
        responseDto.getRecords().forEach(recordDto -> {
            if (recordDto.getSignatureUrl() != null) {
                String fullUrl = s3Service.getFileUrl(recordDto.getSignatureUrl());
                // recordDto의 필드가 final이 아니라면 여기서 세팅
            }
        });

        return responseDto;
    }

    private void validateAdminAuthority(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // MANAGEMENT 직군이고 방문 관리 권한이 있는지 확인
        if (user.getJobType() != JobType.MANAGEMENT || !user.hasAuthority(Authority.ACCESS_VISIT)) {
            throw new CustomException(ErrorCode.VISIT_ACCESS_DENIED);
        }
    }

    // 직원용 사전 장기방문 승인
    @Transactional
    public void approveVisit(Long userId, Long visitId) {
        // 1. 관리 권한 확인 (MANAGEMENT 직군 & ACCESS_VISIT 권한)
        validateAdminAuthority(userId);

        // 2. 방문 신청 건 조회
        Visit visit = visitRepository.findById(visitId)
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

        // 4. 상태 업데이트: PENDING -> APPROVED
        visit.setStatus(VisitStatus.APPROVED);
    }
}
