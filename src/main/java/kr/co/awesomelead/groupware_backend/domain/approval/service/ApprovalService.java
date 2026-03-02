package kr.co.awesomelead.groupware_backend.domain.approval.service;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto.ParticipantRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto.StepRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalListRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.LeaveApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalDetailResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalSummaryResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalAttachment;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalParticipant;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalStep;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.CarFuelApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.ExpenseDraftApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.document.OverseasTripApproval;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ParticipantType;
import kr.co.awesomelead.groupware_backend.domain.approval.mapper.ApprovalMapper;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import kr.co.awesomelead.groupware_backend.global.infra.s3.service.S3Service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final ApprovalAttachmentRepository attachmentRepository;
    private final kr.co.awesomelead.groupware_backend.domain.approval.repository.querydsl
                    .ApprovalQueryRepository
            approvalQueryRepository;
    private final ApprovalMapper approvalMapper;
    private final S3Service s3Service;

    @Transactional
    public Long createApproval(ApprovalCreateRequestDto dto, Long drafterId) {
        // 1. 기안자 정보 및 부서 스냅샷 확보
        User drafter =
                userRepository
                        .findById(drafterId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 근태신청서인 경우 LeaveType-LeaveDetailType 검증
        if (dto instanceof LeaveApprovalCreateRequestDto leaveDto) {
            try {
                leaveDto.getLeaveType().validateDetailType(leaveDto.getLeaveDetailType());
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_LEAVE_DETAIL_TYPE);
            }
        }

        // 3. MapStruct를 이용한 다형성 엔티티 생성
        Approval approval = approvalMapper.toEntity(dto);

        // 3. 공통 필수 정보 세팅 (스냅샷 포함)
        approval.setDrafter(drafter);
        approval.setDraftDepartment(drafter.getDepartment()); // 기안 당시 부서 고정
        approval.setStatus(ApprovalStatus.PENDING); // 최초 상태는 대기
        approval.setRetentionPeriod(approval.getDocumentType().getRetentionPeriod()); // 문서 종류별 고정

        // 4. 상세 내역의 양방향 관계 설정 (details → approval)
        setupDetails(approval);

        // 5. 연관관계 맵핑 (결재선, 참조자, 첨부파일)
        setupApprovalSteps(approval, dto.getApprovalSteps());
        setupParticipants(approval, dto.getParticipants());
        setupAttachments(approval, dto.getAttachmentIds());

        // 6. DB에 먼저 저장하여 PK(id) 채번
        approvalRepository.save(approval);

        // 7. 채번된 PK를 이용하여 문서 번호 생성 및 업데이트
        generateDocumentNumber(approval);

        return approval.getId();
    }

    private void generateDocumentNumber(Approval approval) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String docTypeName = approval.getDocumentType().getDescription();
        String deptName = approval.getDraftDepartment().getName().getDescription();
        Long pkId = approval.getId(); // 방금 저장하여 발급된 PK

        // BASIC(기본양식)인 경우 문서종류 문자열을 생략
        String documentNumber;
        if (kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType.BASIC.equals(
                approval.getDocumentType())) {
            documentNumber = String.format("%s %s-%03d", deptName, today, pkId);
        } else {
            documentNumber = String.format("%s %s %s-%03d", docTypeName, deptName, today, pkId);
        }
        approval.setDocumentNumber(documentNumber);
    }

    private void setupDetails(Approval approval) {
        if (approval instanceof CarFuelApproval carFuel && carFuel.getDetails() != null) {
            carFuel.getDetails().forEach(d -> d.setApproval(carFuel));
        } else if (approval instanceof OverseasTripApproval overseas
                && overseas.getDetails() != null) {
            overseas.getDetails().forEach(d -> d.setApproval(overseas));
        } else if (approval instanceof ExpenseDraftApproval expense
                && expense.getDetails() != null) {
            // WelfareExpenseApproval도 ExpenseDraftApproval을 상속하므로 여기서 처리됨
            expense.getDetails().forEach(d -> d.setApproval(expense));
        }
    }

    private void setupApprovalSteps(Approval approval, List<StepRequestDto> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_STEP);
        }

        // 동일 결재자 중복 검증
        long distinctCount = steps.stream().map(StepRequestDto::getApproverId).distinct().count();
        if (distinctCount < steps.size()) {
            throw new CustomException(ErrorCode.DUPLICATE_APPROVER);
        }

        for (StepRequestDto stepDto : steps) {
            User approver =
                    userRepository
                            .findById(stepDto.getApproverId())
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            ApprovalStep step =
                    new ApprovalStep(
                            null,
                            approval,
                            approver,
                            stepDto.getSequence(),
                            ApprovalStatus.PENDING,
                            null,
                            null);

            approval.getSteps().add(step); // 부모 엔티티 리스트에 추가 (CascadeType.ALL 작동)
        }
    }

    private void setupParticipants(Approval approval, List<ParticipantRequestDto> participants) {
        if (participants == null) {
            return;
        }

        for (ParticipantRequestDto partDto : participants) {
            User user =
                    userRepository
                            .findById(partDto.getUserId())
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            ApprovalParticipant participant =
                    new ApprovalParticipant(null, approval, user, partDto.getParticipantType());

            approval.getParticipants().add(participant);
        }
    }

    private void setupAttachments(Approval approval, List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }

        // AttachmentRepository에서 해당 ID들을 조회하여 관계 맺어줌
        List<ApprovalAttachment> attachments = attachmentRepository.findAllById(attachmentIds);
        for (ApprovalAttachment attachment : attachments) {
            attachment.setApproval(approval); // 외래키 연결
            approval.getAttachments().add(attachment);
        }
    }

    @Transactional
    public void approveApproval(Long approvalId, Long approverId, String comment) {
        Approval approval =
                approvalRepository
                        .findById(approvalId)
                        .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_NOT_FOUND));

        User approver =
                userRepository
                        .findById(approverId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        approval.approve(approver, comment);
    }

    @Transactional
    public void rejectApproval(Long approvalId, Long approverId, String comment) {
        Approval approval =
                approvalRepository
                        .findById(approvalId)
                        .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_NOT_FOUND));

        User approver =
                userRepository
                        .findById(approverId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        approval.reject(approver, comment);
    }

    public Page<ApprovalSummaryResponseDto> getApprovalList(
            ApprovalListRequestDto condition, Long userId) {

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return approvalQueryRepository.findApprovalsByCondition(
                condition, userId, user.getRole().name());
    }

    public ApprovalDetailResponseDto getApprovalDetail(Long approvalId, Long userId) {

        Approval approval =
                approvalRepository
                        .findById(approvalId)
                        .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_NOT_FOUND));

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 권한 검증: 관리자이거나, 해당 문서의 기안자/결재선/참조자에 포함되어 있어야 조회 가능
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.MASTER_ADMIN;
        boolean isDrafter = approval.getDrafter().getId().equals(userId);
        boolean isApprover =
                approval.getSteps().stream()
                        .anyMatch(step -> step.getApprover().getId().equals(userId));
        boolean isParticipant =
                approval.getParticipants().stream()
                        .anyMatch(
                                part -> {
                                    if (part.getUser().getId().equals(userId)) {
                                        // 참조자(REFERRER)는 상신 직후 바로 조회 가능
                                        if (part.getParticipantType() == ParticipantType.REFERRER) {
                                            return true;
                                        }
                                        // 열람권자(VIEWER)는 최종 승인(APPROVED)된 문서만 조회 가능
                                        if (part.getParticipantType() == ParticipantType.VIEWER) {
                                            return approval.getStatus() == ApprovalStatus.APPROVED;
                                        }
                                    }
                                    return false;
                                });

        if (!isAdmin && !isDrafter && !isApprover && !isParticipant) {
            throw new CustomException(ErrorCode.NOT_APPROVER);
        }

        // Entity -> DTO 매핑
        return approvalMapper.toDetailResponseDto(approval, userId, s3Service);
    }
}
