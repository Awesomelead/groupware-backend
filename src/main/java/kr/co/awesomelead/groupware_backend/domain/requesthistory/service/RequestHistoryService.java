package kr.co.awesomelead.groupware_backend.domain.requesthistory.service;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.request.RequestHistoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistoryDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistorySummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.repository.RequestHistoryRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestHistoryService {

    private final RequestHistoryRepository requestHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createRequest(Long userId, RequestHistoryCreateRequestDto requestDto) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!StringUtils.hasText(user.getNameKor()) || user.getPosition() == null) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        RequestHistory requestHistory = new RequestHistory();
        requestHistory.setUser(user);
        requestHistory.setRequestType(requestDto.getRequestType());
        requestHistory.setName(user.getNameKor());
        requestHistory.setPosition(user.getPosition().getDescription());
        requestHistory.setPurpose(requestDto.getPurpose());
        requestHistory.setCopies(requestDto.getCopies());
        requestHistory.setWishDate(requestDto.getWishDate());
        requestHistory.setApprovalStatus(ApprovalStatus.WAITING);

        return requestHistoryRepository.save(requestHistory).getId();
    }

    @Transactional(readOnly = true)
    public List<RequestHistorySummaryResponseDto> getMyRequests(Long userId) {
        userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return requestHistoryRepository.findByUserIdOrderByRequestDateDescIdDesc(userId).stream()
                .map(RequestHistorySummaryResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestHistoryDetailResponseDto getMyRequestDetail(Long userId, Long requestId) {
        userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RequestHistory requestHistory =
                requestHistoryRepository
                        .findByIdAndUserId(requestId, userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.REQUEST_HISTORY_NOT_FOUND));

        return RequestHistoryDetailResponseDto.from(requestHistory);
    }
}
