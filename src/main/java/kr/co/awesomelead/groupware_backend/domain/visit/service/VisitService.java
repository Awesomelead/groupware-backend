package kr.co.awesomelead.groupware_backend.domain.visit.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CheckOutRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
import kr.co.awesomelead.groupware_backend.domain.visit.mapper.VisitMapper;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitorRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final VisitMapper visitMapper;
    private final S3Service s3Service;

    @Transactional
    public VisitResponseDto createPreVisit(
        VisitCreateRequestDto requestDto, MultipartFile signatureFile) throws IOException {
        return createVisitProcess(requestDto, signatureFile, VisitType.PRE_REGISTRATION);
    }

    @Transactional
    public VisitResponseDto createOnSiteVisit(
        VisitCreateRequestDto requestDto, MultipartFile signatureFile) throws IOException {
        return createVisitProcess(requestDto, signatureFile, VisitType.ON_SITE);
    }

    private VisitResponseDto createVisitProcess(
        VisitCreateRequestDto dto, MultipartFile signatureFile, VisitType type)
        throws IOException {

        if (signatureFile == null || signatureFile.isEmpty()) {
            throw new CustomException(ErrorCode.NO_SIGNATURE_PROVIDED);
        }

        // 사전 방문 예약 시, 비밀번호 필수 체크
        if (type == VisitType.PRE_REGISTRATION && !StringUtils.hasText(dto.getVisitorPassword())) {
            throw new CustomException(ErrorCode.VISITOR_PASSWORD_REQUIRED_FOR_PRE_REGISTRATION);
        }

        // 담당 직원 조회
        User host =
            userRepository
                .findById(dto.getHostUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 내방객 조회 혹은 생성
        Visitor visitor = getOrCreateVisitor(dto, type);

        String signatureKey = null;
        try {
            // 3. 서명 이미지 처리 (PNG 검증 및 S3 업로드)
            if (signatureFile != null && !signatureFile.isEmpty()) {
                validatePngFormat(signatureFile);
                signatureKey = s3Service.uploadFile(signatureFile);
            }

            // 4. 엔티티 생성 및 서명 키 설정
            Visit visit = visitMapper.toVisitEntity(dto, host, visitor, type);
            visit.setSignatureKey(signatureKey); // S3 키 저장

            if (visit.getCompanions() != null) {
                visit.getCompanions().forEach(companion -> companion.setVisit(visit));
            }

            // 5. DB 저장
            Visit savedVisit = visitRepository.save(visit);
            return visitMapper.toResponseDto(savedVisit);

        } catch (Exception e) {
            // 6. DB 저장 중 에러 발생 시 S3 파일 삭제 (고아 파일 방지)
            deleteS3FileIfExist(signatureKey);
            throw e;
        }
    }

    // PNG 형식 검증
    private void validatePngFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("image/png")) {
            throw new CustomException(ErrorCode.INVALID_SIGNATURE_FORMAT);
        }
    }

    // S3 파일 삭제 헬퍼
    private void deleteS3FileIfExist(String signatureKey) {
        if (signatureKey != null) {
            try {
                s3Service.deleteFile(signatureKey);
            } catch (Exception s3Ex) {
                log.error("롤백 중 S3 파일 삭제 실패: {}", signatureKey);
            }
        }
    }

    private Visitor getOrCreateVisitor(VisitCreateRequestDto dto, VisitType type) {

        String phoneNumberHash = Visitor.hashPhoneNumber(dto.getVisitorPhone());

        return visitorRepository
            .findByPhoneNumberHash(phoneNumberHash)
            .map(
                existingVisitor -> {
                    // 기존 방문자가 있고, 사전 예약 시 새로운 비번이 들어왔다면 갱신
                    if (type == VisitType.PRE_REGISTRATION
                        && StringUtils.hasText(dto.getVisitorPassword())) {
                        existingVisitor.setPassword(dto.getVisitorPassword());
                        return visitorRepository.save(existingVisitor);
                    }
                    return existingVisitor;
                })
            .orElseGet(() -> visitorRepository.save(visitMapper.toVisitorEntity(dto)));
    }

    @Transactional(readOnly = true)
    public List<VisitSummaryResponseDto> getMyVisits(VisitSearchRequestDto requestDto) {

        String phoneNumberHash = Visitor.hashPhoneNumber(requestDto.getPhoneNumber());
        Visitor visitor =
            visitorRepository
                .findByPhoneNumberHash(phoneNumberHash)
                .orElseThrow(() -> new CustomException(ErrorCode.VISITOR_NOT_FOUND));

        if (!visitor.getName().equals(requestDto.getName())
            || !visitor.getPassword().equals(requestDto.getPassword())) {
            throw new CustomException(ErrorCode.VISITOR_AUTHENTICATION_FAILED);
        }

        List<Visit> visits = visitRepository.findByVisitor(visitor);
        return visitMapper.toVisitSummaryResponseDtoList(visits);
    }

    @Transactional(readOnly = true)
    public MyVisitResponseDto getMyVisitDetail(Long visitId) {
        Visit visit =
            visitRepository
                .findById(visitId)
                .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));

        return visitMapper.toMyVisitResponseDto(visit);
    }

    @Transactional(readOnly = true)
    public List<VisitSummaryResponseDto> getVisitsByDepartment(Long userId, Long departmentId) {

        // 요청한 사용자 조회
        User requestingUser =
            userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 요청한 사용자의 부서명 조회
        DepartmentName requestingUserDeptName = requestingUser.getDepartment().getName();

        List<Visit> visits;

        // '경비' 부서인 경우 전체 조회
        if (requestingUserDeptName == DepartmentName.SECURITY_DEPT || departmentId == null) {
            visits = visitRepository.findAll();
        } else {
            // 특정 부서 ID가 넘어온 경우
            Department targetDept =
                departmentRepository
                    .findById(departmentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

            List<Long> allDeptIds = new ArrayList<>();
            collectDepartmentIdsRecursive(targetDept, allDeptIds);

            visits = visitRepository.findAllByDepartmentIdIn(allDeptIds);
        }

        return visitMapper.toVisitSummaryResponseDtoList(visits);
    }

    private void collectDepartmentIdsRecursive(Department dept, List<Long> ids) {
        ids.add(dept.getId()); // 내 ID 추가
        for (Department child : dept.getChildren()) {
            collectDepartmentIdsRecursive(child, ids); // 자식들의 ID도 재귀적으로 추가
        }
    }

    @Transactional(readOnly = true)
    public VisitDetailResponseDto getVisitDetailByEmployee(Long visitId) {

        Visit visit =
            visitRepository
                .findById(visitId)
                .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));
        VisitDetailResponseDto responseDto = visitMapper.toVisitDetailResponseDto(visit);
        responseDto.setSignatureUrl(s3Service.getPresignedViewUrl(visit.getSignatureKey()));

        return responseDto;
    }

    // 사전 내방객의 현장 방문처리
    // 해당 로직에서 현재시간으로 방문 시작시간을 갱신하는건 어떤지?
    @Transactional
    public void checkIn(Long visitId) {
        Visit visit =
            visitRepository
                .findById(visitId)
                .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));
        visit.checkIn();
    }

    @Transactional
    public void checkOut(CheckOutRequestDto requestDto) {
        // 차후, 해당 기능에 대해 경비원만 혹은 특정 권한을 가진 사용자만 호출할 수 있도록 처리 필요!
        Visit visit =
            visitRepository
                .findById(requestDto.getVisitId())
                .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));
        if (visit.getVisitEndDate() != null) {
            throw new CustomException(ErrorCode.VISIT_ALREADY_CHECKED_OUT);
        }
        visit.checkOut(requestDto.getCheckOutTime());
    }
}
