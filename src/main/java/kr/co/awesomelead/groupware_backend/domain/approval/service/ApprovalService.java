package kr.co.awesomelead.groupware_backend.domain.approval.service;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto.ParticipantRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalCreateRequestDto.StepRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalAttachment;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalParticipant;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalStep;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.mapper.ApprovalMapper;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalAttachmentRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final ApprovalAttachmentRepository attachmentRepository;
    private final ApprovalMapper approvalMapper;

    public Long createApproval(ApprovalCreateRequestDto dto, Long drafterId) {
        // 1. 기안자 정보 및 부서 스냅샷 확보
        User drafter = userRepository.findById(drafterId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. MapStruct를 이용한 다형성 엔티티 생성
        Approval approval = approvalMapper.toEntity(dto);

        // 3. 공통 필수 정보 세팅 (스냅샷 포함)
        approval.setDrafter(drafter);
        approval.setDraftDepartment(drafter.getDepartment()); // 기안 당시 부서 고정
        approval.setStatus(ApprovalStatus.PENDING); // 최초 상태는 대기

        // 4. 연관관계 맵핑 (결재선, 참조자, 첨부파일)
        setupApprovalSteps(approval, dto.getApprovalSteps());
        setupParticipants(approval, dto.getParticipants());
        setupAttachments(approval, dto.getAttachmentIds());

        // 5. 최종 저장
        return approvalRepository.save(approval).getId();
    }

    private void setupApprovalSteps(Approval approval, List<StepRequestDto> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_STEP);
        }

        for (StepRequestDto stepDto : steps) {
            User approver = userRepository.findById(stepDto.getApproverId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            ApprovalStep step = ApprovalStep.builder()
                .approval(approval)
                .approver(approver)
                .sequence(stepDto.getSequence())
                .status(ApprovalStatus.PENDING) // 모든 단계의 초기 상태는 PENDING
                .build();

            approval.getSteps().add(step); // 부모 엔티티 리스트에 추가 (CascadeType.ALL 작동)
        }
    }

    private void setupParticipants(Approval approval, List<ParticipantRequestDto> participants) {
        if (participants == null) {
            return;
        }

        for (ParticipantRequestDto partDto : participants) {
            User user = userRepository.findById(partDto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            ApprovalParticipant participant = ApprovalParticipant.builder()
                .approval(approval)
                .user(user)
                .participantType(partDto.getParticipantType())
                .build();

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

    public void approveApproval(Long approvalId, Long approverId, String comment) {
        Approval approval = approvalRepository.findById(approvalId)
            .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_NOT_FOUND));

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        approval.approve(approver, comment);
    }

    public void rejectApproval(Long approvalId, Long approverId, String comment) {
        Approval approval = approvalRepository.findById(approvalId)
            .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_NOT_FOUND));

        User approver = userRepository.findById(approverId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        approval.reject(approver, comment);
    }
}
