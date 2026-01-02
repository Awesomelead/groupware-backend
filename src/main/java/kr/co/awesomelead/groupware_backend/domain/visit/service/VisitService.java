package kr.co.awesomelead.groupware_backend.domain.visit.service;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitSearchRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.MyVisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
import kr.co.awesomelead.groupware_backend.domain.visit.mapper.VisitMapper;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitorRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final VisitMapper visitMapper;

    @Transactional
    public VisitResponseDto createPreVisit(VisitCreateRequestDto requestDto) {
        return createVisitProcess(requestDto, VisitType.PRE_REGISTRATION);
    }

    @Transactional
    public VisitResponseDto createOnSiteVisit(VisitCreateRequestDto requestDto) {
        return createVisitProcess(requestDto, VisitType.ON_SITE);
    }

    private VisitResponseDto createVisitProcess(VisitCreateRequestDto dto, VisitType type) {
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

        // Mapper
        Visit visit = visitMapper.toVisitEntity(dto, host, visitor, type);

        if (visit.getCompanions() != null) {
            visit.getCompanions().forEach(companion -> companion.setVisit(visit));
        }

        Visit savedVisit = visitRepository.save(visit);

        return visitMapper.toResponseDto(savedVisit);
    }

    private Visitor getOrCreateVisitor(VisitCreateRequestDto dto, VisitType type) {
        return visitorRepository
                .findByPhoneNumberHash(dto.getVisitorPhone())
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
    public void checkOut(Long visitId) {
        // 차후, 해당 기능에 대해 경비원만 혹은 특정 권한을 가진 사용자만 호출할 수 있도록 처리 필요!
        Visit visit =
                visitRepository
                        .findById(visitId)
                        .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));
        if (visit.getVisitEndDate() != null) {
            throw new CustomException(ErrorCode.VISIT_ALREADY_CHECKED_OUT);
        }
        visit.checkOut();
    }
}
