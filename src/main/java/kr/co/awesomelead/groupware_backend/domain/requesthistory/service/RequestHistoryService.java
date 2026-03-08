package kr.co.awesomelead.groupware_backend.domain.requesthistory.service;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.request.RequestHistoryCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.AdminRequestHistoryDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.AdminRequestHistorySummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistoryDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.dto.response.RequestHistorySummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.entity.RequestHistory;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestHistoryStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.repository.RequestHistoryRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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
        requestHistory.setApprovalStatus(RequestHistoryStatus.PENDING);

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
                        .orElseThrow(
                                () -> new CustomException(ErrorCode.REQUEST_HISTORY_NOT_FOUND));

        return RequestHistoryDetailResponseDto.from(requestHistory);
    }

    @Transactional
    public void cancelMyRequest(Long userId, Long requestId) {
        userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RequestHistory requestHistory =
                requestHistoryRepository
                        .findByIdAndUserId(requestId, userId)
                        .orElseThrow(
                                () -> new CustomException(ErrorCode.REQUEST_HISTORY_NOT_FOUND));

        if (requestHistory.getApprovalStatus() != RequestHistoryStatus.PENDING) {
            throw new CustomException(ErrorCode.REQUEST_HISTORY_NOT_CANCELABLE);
        }

        requestHistory.setApprovalStatus(RequestHistoryStatus.CANCELED);
    }

    @Transactional(readOnly = true)
    public Page<AdminRequestHistorySummaryResponseDto> getAllRequestsForAdmin(
            Long adminId, RequestHistoryStatus status, Pageable pageable) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateAdminAuthority(admin);

        return requestHistoryRepository
                .findAllWithUserAndDepartmentByStatus(status, pageable)
                .map(AdminRequestHistorySummaryResponseDto::from);
    }

    @Transactional(readOnly = true)
    public AdminRequestHistoryDetailResponseDto getRequestDetailForAdmin(Long adminId, Long requestId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateAdminAuthority(admin);

        RequestHistory requestHistory =
                requestHistoryRepository
                        .findByIdWithUserAndDepartment(requestId)
                        .orElseThrow(
                                () -> new CustomException(ErrorCode.REQUEST_HISTORY_NOT_FOUND));

        return AdminRequestHistoryDetailResponseDto.from(requestHistory);
    }

    @Transactional
    public void issueRequest(Long adminId, Long requestId) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateAdminAuthority(admin);

        RequestHistory requestHistory =
                requestHistoryRepository
                        .findByIdWithUserAndDepartment(requestId)
                        .orElseThrow(
                                () -> new CustomException(ErrorCode.REQUEST_HISTORY_NOT_FOUND));

        if (requestHistory.getApprovalStatus() != RequestHistoryStatus.PENDING) {
            throw new CustomException(ErrorCode.REQUEST_HISTORY_NOT_ISSUABLE);
        }

        requestHistory.setApprovalStatus(RequestHistoryStatus.ISSUED);
        requestHistory.setProcessedBy(admin);
        requestHistory.setProcessedDate(LocalDate.now());
        requestHistory.setRejectReason(null);
    }

    @Transactional
    public void rejectRequest(Long adminId, Long requestId, String reason) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        validateAdminAuthority(admin);

        if (!StringUtils.hasText(reason)) {
            throw new CustomException(ErrorCode.REJECTION_REASON_REQUIRED);
        }

        RequestHistory requestHistory =
                requestHistoryRepository
                        .findByIdWithUserAndDepartment(requestId)
                        .orElseThrow(
                                () -> new CustomException(ErrorCode.REQUEST_HISTORY_NOT_FOUND));

        if (requestHistory.getApprovalStatus() != RequestHistoryStatus.PENDING) {
            throw new CustomException(ErrorCode.REQUEST_HISTORY_NOT_REJECTABLE);
        }

        requestHistory.setApprovalStatus(RequestHistoryStatus.REJECTED);
        requestHistory.setProcessedBy(admin);
        requestHistory.setProcessedDate(LocalDate.now());
        requestHistory.setRejectReason(reason.trim());
    }

    private void validateAdminAuthority(User admin) {
        if (!admin.hasAuthority(Authority.MANAGE_CERTIFICATE_REQUEST)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_CERTIFICATE_REQUEST_REVIEW);
        }
    }
}
