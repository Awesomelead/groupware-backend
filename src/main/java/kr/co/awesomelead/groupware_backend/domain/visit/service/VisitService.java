package kr.co.awesomelead.groupware_backend.domain.visit.service;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.CompanionRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.OnSiteVisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.PreVisitCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.request.VisitorRequestDto;
import kr.co.awesomelead.groupware_backend.domain.visit.dto.response.VisitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Companion;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitRepository;
import kr.co.awesomelead.groupware_backend.domain.visit.repository.VisitorRepository;
import kr.co.awesomelead.groupware_backend.global.CustomException;
import kr.co.awesomelead.groupware_backend.global.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;

    @Transactional
    public VisitResponseDto createPreVisit(PreVisitCreateRequestDto requestDto) {
        // 담당자 조회
        User host = userRepository.findById(requestDto.getHostUserId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 방문객 조회 혹은 생성
        Visitor visitor = getOrCreateVisitor(requestDto.getVisitor());

        // 방문 기록 생성
        Visit visit = Visit.createPreVisit(host, visitor, requestDto);

        Visit savedVisit = visitRepository.save(visit);
        return VisitResponseDto.from(savedVisit);
    }

    @Transactional
    public VisitResponseDto createOnSiteVisit(OnSiteVisitCreateRequestDto requestDto) {
        // 담당자 조회
        User host = userRepository.findById(requestDto.getHostUserId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 방문객 조회 혹은 생성
        Visitor visitor = getOrCreateVisitor(requestDto.getVisitor());

        // 방문 기록 생성
        Visit visit = Visit.createOnSiteVisit(host, visitor, requestDto);
        // 동행자 저장
        saveCompanions(visit, requestDto.getCompanions());

        Visit savedVisit = visitRepository.save(visit);
        return VisitResponseDto.from(savedVisit);
    }

    private Visitor getOrCreateVisitor(VisitorRequestDto dto) {
        return visitorRepository.findByPhoneNumber(dto.getPhoneNumber())
            .orElseGet(() -> {
                Visitor newVisitor = new Visitor();
                newVisitor.setName(dto.getName());
                newVisitor.setPhoneNumber(dto.getPhoneNumber());
                newVisitor.setPassword(dto.getPassword());
                return visitorRepository.save(newVisitor);
            });
    }

    private void saveCompanions(Visit visit, List<CompanionRequestDto> companionDtos) {
        if (companionDtos == null || companionDtos.isEmpty()) {
            return;
        }

        for (CompanionRequestDto dto : companionDtos) {
            Companion companion = new Companion();
            companion.setName(dto.getName());
            companion.setPhoneNumber(dto.getPhoneNumber());
            companion.setVisitorCompany(dto.getVisitorCompany());
            visit.addCompanion(companion);
        }
    }

    @Transactional
    public void checkOut(Long visitId) {
        // 차후, 해당 기능에 대해 경비원만 혹은 특정 권한을 가진 사용자만 호출할 수 있도록 처리 필요!
        Visit visit = visitRepository.findById(visitId)
            .orElseThrow(() -> new CustomException(ErrorCode.VISIT_NOT_FOUND));
        if (visit.getVisitEndDate() != null) {
            throw new CustomException(ErrorCode.VISIT_ALREADY_CHECKED_OUT);
        }
        visit.checkOut();
    }
}
