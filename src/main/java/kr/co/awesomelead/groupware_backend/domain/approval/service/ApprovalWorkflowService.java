package kr.co.awesomelead.groupware_backend.domain.approval.service;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDirectSubmitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalDraftUpsertRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalLineRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalSubmitRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalDraftResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalInboxAllResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalSubmitResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalTemplateListResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalActionHistory;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalDocument;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalDocumentLine;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplate;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateCategory;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateLine;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalActionType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalLineStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalRouteRole;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalActionHistoryRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalDocumentLineRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalDocumentRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalTemplateCategoryRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalTemplateLineRepository;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalTemplateRepository;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private final ApprovalTemplateCategoryRepository approvalTemplateCategoryRepository;
    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final ApprovalTemplateLineRepository approvalTemplateLineRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalDocumentLineRepository approvalDocumentLineRepository;
    private final ApprovalActionHistoryRepository approvalActionHistoryRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public ApprovalTemplateListResponseDto getTemplateList() {
        List<ApprovalTemplateCategory> categories =
                approvalTemplateCategoryRepository.findByIsActiveTrueOrderBySortOrderAscIdAsc();

        List<ApprovalTemplateListResponseDto.CategoryDto> categoryDtos =
                categories.stream()
                        .map(
                                category -> {
                                    List<ApprovalTemplate> templates =
                                            approvalTemplateRepository
                                                    .findByCategoryIdAndIsActiveTrueOrderByIdAsc(
                                                            category.getId());

                                    List<ApprovalTemplateListResponseDto.TemplateDto> templateDtos =
                                            templates.stream().map(this::toTemplateDto).toList();

                                    return ApprovalTemplateListResponseDto.CategoryDto.builder()
                                            .id(category.getId())
                                            .code(category.getCode())
                                            .name(category.getName())
                                            .sortOrder(category.getSortOrder())
                                            .templates(templateDtos)
                                            .build();
                                })
                        .toList();

        return ApprovalTemplateListResponseDto.builder().categories(categoryDtos).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getInboxAll(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isInInProgressAll(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getInboxToApprove(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isToApproveDocument(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getInboxBeforeMyTurn(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isBeforeMyTurnDocument(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getInboxProcessedByMe(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isProcessedByMeDocument(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getInboxRejectedOrRecalled(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(
                                document ->
                                        isRejectedOrRecalledDocument(
                                                document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getInboxDraftBox(Long userId) {
        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isDraftBoxDocument(document, userId))
                        .map(document -> toInboxDocumentDto(document, userId, null))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getAllMyDrafted(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isDraftedByMe(document, userId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getAllMyApprovals(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isMyApprovalDocument(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getAllAll(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isAllTabDocument(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getReferenceDocuments(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isReferenceDocument(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getViewerAcquiredDocuments(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isViewerAcquiredDocument(document, userId, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getViewerGrantedDocuments(Long userId) {
        User user = getUser(userId);
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isViewerGrantedDocument(document, userId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder().documents(documents).build();
    }

    @Transactional(readOnly = true)
    public ApprovalInboxAllResponseDto getDepartmentBox(Long userId) {
        User user = getUser(userId);
        if (user.getDepartment() == null) {
            throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        Department myDepartment = user.getDepartment();
        Long departmentId = myDepartment.getId();

        List<ApprovalInboxAllResponseDto.DocumentDto> documents =
                approvalDocumentRepository.findAllWithLinesOrderByIdDesc().stream()
                        .filter(document -> isDepartmentBoxDocument(document, departmentId))
                        .map(document -> toInboxDocumentDto(document, userId, departmentId))
                        .toList();

        return ApprovalInboxAllResponseDto.builder()
                .myDepartmentId(myDepartment.getId())
                .myDepartmentName(myDepartment.getName().getDescription())
                .documents(documents)
                .build();
    }

    @Transactional
    public ApprovalDraftResponseDto upsertDraft(
            Long userId, ApprovalDraftUpsertRequestDto request) {
        User drafter = getUser(userId);
        ApprovalTemplate template = getActiveTemplate(request.getTemplateId());

        ApprovalDocument document;
        boolean isCreate = request.getDocumentId() == null;

        if (isCreate) {
            if (drafter.getDepartment() == null) {
                throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
            }
            document = new ApprovalDocument();
            document.setDrafterUser(drafter);
            document.setDrafterDepartment(drafter.getDepartment());
            document.setStatus(ApprovalStatus.DRAFT);
        } else {
            document =
                    approvalDocumentRepository
                            .findByIdAndDrafterUserIdWithLines(request.getDocumentId(), userId)
                            .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_NOT_FOUND));
            if (document.getStatus() != ApprovalStatus.DRAFT) {
                throw new CustomException(ErrorCode.INVALID_ARGUMENT);
            }
        }

        applyDocumentBaseFields(
                document,
                template,
                request.getTitle(),
                request.getContentDelta(),
                request.getContentHtml(),
                request.getApprovalType(),
                request.getReceiverDepartmentId(),
                false);

        approvalDocumentRepository.save(document);

        if (request.getLines() != null) {
            replaceDocumentLines(document, request.getLines(), false);
        } else if (isCreate) {
            replaceDocumentLines(document, toLineRequestsFromTemplate(template), false);
        }

        return ApprovalDraftResponseDto.builder()
                .documentId(document.getId())
                .status(document.getStatus())
                .updatedAt(document.getModifiedAt())
                .build();
    }

    @Transactional
    public ApprovalSubmitResponseDto submit(
            Long userId, Long documentId, ApprovalSubmitRequestDto request) {
        User actor = getUser(userId);
        ApprovalDocument document =
                approvalDocumentRepository
                        .findByIdAndDrafterUserIdWithLines(documentId, userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.APPROVAL_NOT_FOUND));

        ApprovalStatus fromStatus = document.getStatus();
        if (fromStatus != ApprovalStatus.DRAFT
                && fromStatus != ApprovalStatus.RECALLED
                && fromStatus != ApprovalStatus.REJECTED) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        ApprovalTemplate template = document.getTemplate();
        applyDocumentBaseFields(
                document,
                template,
                StringUtils.hasText(request.getTitle()) ? request.getTitle() : document.getTitle(),
                StringUtils.hasText(request.getContentDelta())
                        ? request.getContentDelta()
                        : document.getContentDelta(),
                request.getContentHtml() != null
                        ? request.getContentHtml()
                        : document.getContentHtml(),
                request.getApprovalType() != null
                        ? request.getApprovalType()
                        : document.getApprovalType(),
                request.getReceiverDepartmentId(),
                true);

        List<ApprovalDocumentLine> lines;
        if (request.getLines() != null) {
            lines = replaceDocumentLines(document, request.getLines(), true);
        } else {
            lines =
                    approvalDocumentLineRepository.findByDocumentIdOrderBySequenceNoAscIdAsc(
                            documentId);
            if (lines.isEmpty()) {
                lines = replaceDocumentLines(document, toLineRequestsFromTemplate(template), true);
            } else {
                lines = normalizeAndActivateLines(lines, true);
            }
        }

        if (lines.stream().noneMatch(line -> line.getRole() == ApprovalRouteRole.APPROVAL_LINE)) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_STEP);
        }

        document.setStatus(ApprovalStatus.IN_PROGRESS);
        document.setSubmittedAt(LocalDateTime.now());
        document.setCompletedAt(null);
        if (!StringUtils.hasText(document.getDocumentNo())) {
            document.setDocumentNo(buildSubmittedDocumentNo(document));
        }
        approvalDocumentRepository.save(document);

        approvalActionHistoryRepository.save(
                ApprovalActionHistory.builder()
                        .document(document)
                        .actionType(
                                fromStatus == ApprovalStatus.DRAFT
                                        ? ApprovalActionType.SUBMIT
                                        : ApprovalActionType.RESUBMIT)
                        .fromStatus(fromStatus)
                        .toStatus(ApprovalStatus.IN_PROGRESS)
                        .actorUser(actor)
                        .build());

        return ApprovalSubmitResponseDto.builder()
                .documentId(document.getId())
                .documentNo(document.getDocumentNo())
                .status(document.getStatus())
                .drafterUserId(
                        document.getDrafterUser() != null
                                ? document.getDrafterUser().getId()
                                : null)
                .drafterUserName(
                        document.getDrafterUser() != null
                                ? document.getDrafterUser().getDisplayName()
                                : null)
                .title(document.getTitle())
                .approvalLines(toSubmitApprovalLines(lines))
                .draftedAt(document.getSubmittedAt())
                .submittedAt(document.getSubmittedAt())
                .completedAt(document.getCompletedAt())
                .build();
    }

    @Transactional
    public ApprovalSubmitResponseDto submitDirect(
            Long userId, ApprovalDirectSubmitRequestDto request) {
        ApprovalDraftUpsertRequestDto draftRequest = new ApprovalDraftUpsertRequestDto();
        draftRequest.setDocumentId(null);
        draftRequest.setTemplateId(request.getTemplateId());
        draftRequest.setTitle(request.getTitle());
        draftRequest.setContentDelta(request.getContentDelta());
        draftRequest.setContentHtml(request.getContentHtml());
        draftRequest.setApprovalType(request.getApprovalType());
        draftRequest.setReceiverDepartmentId(request.getReceiverDepartmentId());
        draftRequest.setLines(request.getLines());

        ApprovalDraftResponseDto draftResult = upsertDraft(userId, draftRequest);

        ApprovalSubmitRequestDto submitRequest = new ApprovalSubmitRequestDto();
        submitRequest.setTitle(request.getTitle());
        submitRequest.setContentDelta(request.getContentDelta());
        submitRequest.setContentHtml(request.getContentHtml());
        submitRequest.setApprovalType(request.getApprovalType());
        submitRequest.setReceiverDepartmentId(request.getReceiverDepartmentId());
        submitRequest.setLines(request.getLines());

        return submit(userId, draftResult.getDocumentId(), submitRequest);
    }

    private ApprovalTemplateListResponseDto.TemplateDto toTemplateDto(ApprovalTemplate template) {
        List<ApprovalTemplateLine> lines =
                approvalTemplateLineRepository.findByTemplateIdOrderBySequenceNoAscIdAsc(
                        template.getId());

        List<ApprovalTemplateListResponseDto.LineDto> lineDtos =
                lines.stream()
                        .map(
                                line ->
                                        ApprovalTemplateListResponseDto.LineDto.builder()
                                                .role(line.getRole())
                                                .targetType(line.getTargetType())
                                                .targetUserId(
                                                        line.getTargetUser() != null
                                                                ? line.getTargetUser().getId()
                                                                : null)
                                                .targetDepartmentId(
                                                        line.getTargetDepartment() != null
                                                                ? line.getTargetDepartment().getId()
                                                                : null)
                                                .targetName(
                                                        toTargetName(
                                                                line.getTargetType(),
                                                                line.getTargetUser(),
                                                                line.getTargetDepartment()))
                                                .sequenceNo(line.getSequenceNo())
                                                .required(line.getIsRequired())
                                                .build())
                        .toList();

        return ApprovalTemplateListResponseDto.TemplateDto.builder()
                .id(template.getId())
                .code(template.getCode())
                .name(template.getName())
                .description(template.getDescription())
                .editorType(template.getEditorType())
                .approvalType(template.getApprovalType())
                .linePolicy(template.getLinePolicy())
                .defaultContentDelta(template.getDefaultContentDelta())
                .defaultLines(lineDtos)
                .build();
    }

    private boolean isInInProgressAll(ApprovalDocument document, Long userId, Long departmentId) {
        return isDraftBoxDocument(document, userId)
                || isToApproveDocument(document, userId, departmentId)
                || isBeforeMyTurnDocument(document, userId, departmentId)
                || isProcessedByMeDocument(document, userId, departmentId)
                || isRejectedOrRecalledDocument(document, userId, departmentId);
    }

    private boolean isDraftBoxDocument(ApprovalDocument document, Long userId) {
        return document.getStatus() == ApprovalStatus.DRAFT
                && document.getDrafterUser() != null
                && userId.equals(document.getDrafterUser().getId());
    }

    private boolean isDraftedByMe(ApprovalDocument document, Long userId) {
        return document.getDrafterUser() != null
                && userId.equals(document.getDrafterUser().getId());
    }

    private boolean isAllTabDocument(ApprovalDocument document, Long userId, Long departmentId) {
        return isDraftedByMe(document, userId)
                || isMyApprovalDocument(document, userId, departmentId);
    }

    private boolean isReferenceDocument(
            ApprovalDocument document, Long userId, Long departmentId) {
        if (document.getStatus() == ApprovalStatus.DRAFT) {
            return false;
        }
        return document.getLines().stream()
                .anyMatch(line -> isMyReferenceLine(line, userId, departmentId));
    }

    private boolean isViewerAcquiredDocument(
            ApprovalDocument document, Long userId, Long departmentId) {
        if (document.getStatus() != ApprovalStatus.APPROVED) {
            return false;
        }
        if (isReferenceDocument(document, userId, departmentId)) {
            return false;
        }
        return document.getLines().stream()
                .anyMatch(line -> isMyViewerLine(line, userId, departmentId));
    }

    private boolean isViewerGrantedDocument(ApprovalDocument document, Long userId) {
        if (document.getStatus() == ApprovalStatus.DRAFT) {
            return false;
        }
        if (document.getDrafterUser() == null || !userId.equals(document.getDrafterUser().getId())) {
            return false;
        }
        return document.getLines().stream().anyMatch(line -> line.getRole() == ApprovalRouteRole.VIEWER);
    }

    private boolean isDepartmentBoxDocument(ApprovalDocument document, Long departmentId) {
        if (document.getStatus() == ApprovalStatus.DRAFT) {
            return false;
        }

        if (document.getDrafterDepartment() != null
                && departmentId.equals(document.getDrafterDepartment().getId())) {
            return true;
        }

        return document.getLines().stream()
                .anyMatch(line -> isDepartmentBoxTargetLine(line, departmentId));
    }

    private boolean isDepartmentBoxTargetLine(ApprovalDocumentLine line, Long departmentId) {
        if (line.getTargetType() != ApprovalTargetType.DEPARTMENT) {
            return false;
        }
        if (line.getTargetDepartment() == null
                || !departmentId.equals(line.getTargetDepartment().getId())) {
            return false;
        }
        return line.getRole() == ApprovalRouteRole.APPROVAL_LINE
                || line.getRole() == ApprovalRouteRole.AGREEMENT_REQUIRED
                || line.getRole() == ApprovalRouteRole.AGREEMENT_OPTIONAL
                || line.getRole() == ApprovalRouteRole.RECEIVER_DEPARTMENT;
    }

    private boolean isMyApprovalDocument(
            ApprovalDocument document, Long userId, Long departmentId) {
        if (document.getStatus() == ApprovalStatus.DRAFT) {
            return false;
        }
        return document.getLines().stream()
                .anyMatch(line -> isMyProcessingLine(line, userId, departmentId));
    }

    private boolean isToApproveDocument(ApprovalDocument document, Long userId, Long departmentId) {
        if (document.getStatus() != ApprovalStatus.IN_PROGRESS) {
            return false;
        }
        return document.getLines().stream()
                .anyMatch(line -> isMyPendingProcessingLine(line, userId, departmentId));
    }

    private boolean isBeforeMyTurnDocument(
            ApprovalDocument document, Long userId, Long departmentId) {
        if (document.getStatus() != ApprovalStatus.IN_PROGRESS) {
            return false;
        }
        return document.getLines().stream()
                .anyMatch(line -> isMyWaitingProcessingLine(line, userId, departmentId));
    }

    private boolean isProcessedByMeDocument(
            ApprovalDocument document, Long userId, Long departmentId) {
        return document.getLines().stream()
                .anyMatch(line -> isMyApprovedProcessingLine(line, userId, departmentId));
    }

    private boolean isRejectedOrRecalledDocument(
            ApprovalDocument document, Long userId, Long departmentId) {
        if (document.getStatus() == ApprovalStatus.REJECTED) {
            return document.getLines().stream()
                    .anyMatch(line -> isMyRejectedProcessingLine(line, userId, departmentId));
        }
        if (document.getStatus() == ApprovalStatus.RECALLED) {
            return document.getDrafterUser() != null
                    && userId.equals(document.getDrafterUser().getId());
        }
        return false;
    }

    private boolean isMyPendingProcessingLine(
            ApprovalDocumentLine line, Long userId, Long departmentId) {
        return isMyProcessingLine(line, userId, departmentId)
                && line.getLineStatus() == ApprovalLineStatus.PENDING;
    }

    private boolean isMyWaitingProcessingLine(
            ApprovalDocumentLine line, Long userId, Long departmentId) {
        return isMyProcessingLine(line, userId, departmentId)
                && line.getLineStatus() == ApprovalLineStatus.WAITING;
    }

    private boolean isMyApprovedProcessingLine(
            ApprovalDocumentLine line, Long userId, Long departmentId) {
        return isMyProcessingLine(line, userId, departmentId)
                && line.getLineStatus() == ApprovalLineStatus.APPROVED
                && line.getProcessedByUser() != null
                && userId.equals(line.getProcessedByUser().getId());
    }

    private boolean isMyRejectedProcessingLine(
            ApprovalDocumentLine line, Long userId, Long departmentId) {
        return isMyProcessingLine(line, userId, departmentId)
                && line.getLineStatus() == ApprovalLineStatus.REJECTED
                && line.getProcessedByUser() != null;
    }

    private boolean isMyProcessingLine(ApprovalDocumentLine line, Long userId, Long departmentId) {
        if (!isProcessingRole(line.getRole())) {
            return false;
        }
        if (line.getTargetType() == ApprovalTargetType.USER) {
            return line.getTargetUser() != null && userId.equals(line.getTargetUser().getId());
        }
        return departmentId != null
                && line.getTargetDepartment() != null
                && departmentId.equals(line.getTargetDepartment().getId());
    }

    private boolean isMyReferenceLine(ApprovalDocumentLine line, Long userId, Long departmentId) {
        if (line.getRole() != ApprovalRouteRole.REFERENCE) {
            return false;
        }
        if (line.getTargetType() == ApprovalTargetType.USER) {
            return line.getTargetUser() != null
                    && userId.equals(line.getTargetUser().getId());
        }
        return departmentId != null
                && line.getTargetDepartment() != null
                && departmentId.equals(line.getTargetDepartment().getId());
    }

    private boolean isMyViewerLine(ApprovalDocumentLine line, Long userId, Long departmentId) {
        if (line.getRole() != ApprovalRouteRole.VIEWER) {
            return false;
        }
        if (line.getTargetType() == ApprovalTargetType.USER) {
            return line.getTargetUser() != null
                    && userId.equals(line.getTargetUser().getId());
        }
        return departmentId != null
                && line.getTargetDepartment() != null
                && departmentId.equals(line.getTargetDepartment().getId());
    }

    private boolean isProcessingRole(ApprovalRouteRole role) {
        return role != ApprovalRouteRole.REFERENCE && role != ApprovalRouteRole.VIEWER;
    }

    private ApprovalInboxAllResponseDto.DocumentDto toInboxDocumentDto(
            ApprovalDocument document, Long userId, Long departmentId) {
        User drafterUser = document.getDrafterUser();
        Department drafterDepartment = document.getDrafterDepartment();

        String drafterUserName = drafterUser != null ? drafterUser.getDisplayName() : null;
        String drafterDepartmentName =
                drafterDepartment != null ? drafterDepartment.getName().getDescription() : null;

        List<ApprovalInboxAllResponseDto.MyLineDto> myLines =
                document.getLines().stream()
                        .filter(line -> isMyProcessingLine(line, userId, departmentId))
                        .sorted(
                                Comparator.comparing(
                                                ApprovalDocumentLine::getSequenceNo,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(ApprovalDocumentLine::getId))
                        .map(this::toMyLineDto)
                        .toList();

        List<ApprovalInboxAllResponseDto.ApprovalLineDto> approvalLines =
                document.getLines().stream()
                        .filter(line -> isProcessingRole(line.getRole()))
                        .sorted(
                                Comparator.comparing(
                                                ApprovalDocumentLine::getSequenceNo,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(ApprovalDocumentLine::getId))
                        .map(this::toInboxApprovalLineDto)
                        .toList();

        return ApprovalInboxAllResponseDto.DocumentDto.builder()
                .documentNo(document.getDocumentNo())
                .documentId(document.getId())
                .templateId(document.getTemplate() != null ? document.getTemplate().getId() : null)
                .templateCode(document.getTemplateCodeSnapshot())
                .templateName(document.getTemplateNameSnapshot())
                .title(document.getTitle())
                .approvalType(document.getApprovalType())
                .approvalTypeLabel(
                        document.getApprovalType() != null
                                ? document.getApprovalType().getDescription()
                                : null)
                .status(document.getStatus())
                .statusLabel(
                        document.getStatus() != null ? document.getStatus().getDescription() : null)
                .drafterUserId(drafterUser != null ? drafterUser.getId() : null)
                .drafterUserName(drafterUserName)
                .drafterName(drafterUserName)
                .drafterDepartmentId(drafterDepartment != null ? drafterDepartment.getId() : null)
                .drafterDepartmentName(drafterDepartmentName)
                .mine(drafterUser != null && userId.equals(drafterUser.getId()))
                .draftedAt(document.getSubmittedAt())
                .submittedAt(document.getSubmittedAt())
                .completedAt(document.getCompletedAt())
                .createdAt(document.getCreatedAt())
                .modifiedAt(document.getModifiedAt())
                .approvalLines(approvalLines)
                .myLines(myLines)
                .build();
    }

    private ApprovalInboxAllResponseDto.ApprovalLineDto toInboxApprovalLineDto(
            ApprovalDocumentLine line) {
        return ApprovalInboxAllResponseDto.ApprovalLineDto.builder()
                .lineId(line.getId())
                .role(line.getRole())
                .roleLabel(line.getRole() != null ? line.getRole().getDescription() : null)
                .targetType(line.getTargetType())
                .targetUserId(line.getTargetUser() != null ? line.getTargetUser().getId() : null)
                .targetDepartmentId(
                        line.getTargetDepartment() != null
                                ? line.getTargetDepartment().getId()
                                : null)
                .targetName(line.getTargetNameSnapshot())
                .sequenceNo(line.getSequenceNo())
                .lineStatus(line.getLineStatus())
                .lineStatusLabel(
                        line.getLineStatus() != null ? line.getLineStatus().getDescription() : null)
                .build();
    }

    private ApprovalInboxAllResponseDto.MyLineDto toMyLineDto(ApprovalDocumentLine line) {
        return ApprovalInboxAllResponseDto.MyLineDto.builder()
                .lineId(line.getId())
                .role(line.getRole())
                .roleLabel(line.getRole() != null ? line.getRole().getDescription() : null)
                .targetType(line.getTargetType())
                .targetUserId(line.getTargetUser() != null ? line.getTargetUser().getId() : null)
                .targetDepartmentId(
                        line.getTargetDepartment() != null
                                ? line.getTargetDepartment().getId()
                                : null)
                .targetName(line.getTargetNameSnapshot())
                .sequenceNo(line.getSequenceNo())
                .required(line.getIsRequired())
                .lineStatus(line.getLineStatus())
                .lineStatusLabel(
                        line.getLineStatus() != null ? line.getLineStatus().getDescription() : null)
                .build();
    }

    private String buildSubmittedDocumentNo(ApprovalDocument document) {
        ApprovalTemplate template = document.getTemplate();
        if (template == null || template.getId() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (document.getDrafterDepartment() == null
                || document.getDrafterDepartment().getName() == null) {
            throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        long sequence =
                approvalDocumentRepository.countByTemplateIdAndStatusNot(
                                template.getId(), ApprovalStatus.DRAFT)
                        + 1;
        String sequencePart = String.format("%02d", sequence);
        String datePart =
                (document.getSubmittedAt() != null
                                ? document.getSubmittedAt()
                                : LocalDateTime.now())
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String departmentName = document.getDrafterDepartment().getName().getDescription();
        String templateName =
                StringUtils.hasText(document.getTemplateNameSnapshot())
                        ? document.getTemplateNameSnapshot()
                        : template.getName();
        String templateCode =
                StringUtils.hasText(document.getTemplateCodeSnapshot())
                        ? document.getTemplateCodeSnapshot()
                        : template.getCode();

        if (isBasicTemplate(templateCode, templateName)) {
            return departmentName + " " + datePart + "-" + sequencePart;
        }
        return templateName + " " + departmentName + " " + datePart + "-" + sequencePart;
    }

    private boolean isBasicTemplate(String templateCode, String templateName) {
        if ("BASIC".equalsIgnoreCase(templateCode)) {
            return true;
        }
        return "기본양식".equals(templateName);
    }

    private List<ApprovalSubmitResponseDto.ApprovalLineDto> toSubmitApprovalLines(
            List<ApprovalDocumentLine> lines) {
        return lines.stream()
                .filter(line -> isProcessingRole(line.getRole()))
                .sorted(
                        Comparator.comparing(
                                        ApprovalDocumentLine::getSequenceNo,
                                        Comparator.nullsLast(Integer::compareTo))
                                .thenComparing(ApprovalDocumentLine::getId))
                .map(this::toSubmitApprovalLine)
                .toList();
    }

    private ApprovalSubmitResponseDto.ApprovalLineDto toSubmitApprovalLine(
            ApprovalDocumentLine line) {
        return ApprovalSubmitResponseDto.ApprovalLineDto.builder()
                .lineId(line.getId())
                .role(line.getRole())
                .roleLabel(line.getRole() != null ? line.getRole().getDescription() : null)
                .targetType(line.getTargetType())
                .targetUserId(line.getTargetUser() != null ? line.getTargetUser().getId() : null)
                .targetDepartmentId(
                        line.getTargetDepartment() != null
                                ? line.getTargetDepartment().getId()
                                : null)
                .targetName(line.getTargetNameSnapshot())
                .sequenceNo(line.getSequenceNo())
                .build();
    }

    private void applyDocumentBaseFields(
            ApprovalDocument document,
            ApprovalTemplate template,
            String title,
            String contentDelta,
            String contentHtml,
            ApprovalType approvalType,
            Long receiverDepartmentId,
            boolean strictSubmit) {
        document.setTemplate(template);
        document.setTemplateNameSnapshot(template.getName());
        document.setTemplateCodeSnapshot(template.getCode());

        if (strictSubmit && !StringUtils.hasText(title)) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
        if (strictSubmit && !StringUtils.hasText(contentDelta)) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        String resolvedTitle =
                strictSubmit
                        ? title
                        : (title != null
                                ? title
                                : (document.getTitle() != null ? document.getTitle() : ""));
        String resolvedContentDelta =
                strictSubmit
                        ? contentDelta
                        : (contentDelta != null
                                ? contentDelta
                                : (document.getContentDelta() != null
                                        ? document.getContentDelta()
                                        : ""));

        document.setTitle(resolvedTitle);
        document.setContentDelta(resolvedContentDelta);
        document.setContentHtml(contentHtml);

        ApprovalType resolvedType =
                approvalType != null
                        ? approvalType
                        : (document.getApprovalType() != null
                                ? document.getApprovalType()
                                : template.getApprovalType());
        document.setApprovalType(resolvedType);

        if (resolvedType == ApprovalType.COOPERATIVE) {
            if (strictSubmit && receiverDepartmentId == null) {
                throw new CustomException(ErrorCode.INVALID_ARGUMENT);
            }
            if (receiverDepartmentId != null) {
                Department receiverDepartment =
                        departmentRepository
                                .findById(receiverDepartmentId)
                                .orElseThrow(
                                        () -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
                document.setReceiverDepartment(receiverDepartment);
            }
        } else {
            document.setReceiverDepartment(null);
        }
    }

    private List<ApprovalLineRequestDto> toLineRequestsFromTemplate(ApprovalTemplate template) {
        List<ApprovalTemplateLine> templateLines =
                approvalTemplateLineRepository.findByTemplateIdOrderBySequenceNoAscIdAsc(
                        template.getId());

        List<ApprovalLineRequestDto> requests = new ArrayList<>();
        for (ApprovalTemplateLine templateLine : templateLines) {
            ApprovalLineRequestDto lineRequest = new ApprovalLineRequestDto();
            lineRequest.setRole(templateLine.getRole());
            lineRequest.setTargetType(templateLine.getTargetType());
            lineRequest.setTargetUserId(
                    templateLine.getTargetUser() != null
                            ? templateLine.getTargetUser().getId()
                            : null);
            lineRequest.setTargetDepartmentId(
                    templateLine.getTargetDepartment() != null
                            ? templateLine.getTargetDepartment().getId()
                            : null);
            lineRequest.setSequenceNo(templateLine.getSequenceNo());
            lineRequest.setRequired(templateLine.getIsRequired());
            requests.add(lineRequest);
        }
        return requests;
    }

    private List<ApprovalDocumentLine> replaceDocumentLines(
            ApprovalDocument document,
            List<ApprovalLineRequestDto> lineRequests,
            boolean forSubmit) {
        if (document.getId() == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        approvalDocumentLineRepository.deleteByDocumentId(document.getId());
        if (lineRequests == null || lineRequests.isEmpty()) {
            return List.of();
        }

        List<ApprovalDocumentLine> newLines = new ArrayList<>();
        for (ApprovalLineRequestDto lineRequest : lineRequests) {
            ApprovalDocumentLine line = new ApprovalDocumentLine();
            line.setDocument(document);
            line.setRole(lineRequest.getRole());
            line.setTargetType(lineRequest.getTargetType());
            line.setSequenceNo(lineRequest.getSequenceNo());
            line.setIsRequired(
                    lineRequest.getRequired() != null
                            ? lineRequest.getRequired()
                            : lineRequest.getRole() != ApprovalRouteRole.AGREEMENT_OPTIONAL);
            line.setLineStatus(ApprovalLineStatus.WAITING);
            line.setProcessedAt(null);
            line.setProcessedByUser(null);
            line.setProcessedComment(null);

            if (lineRequest.getTargetType() == ApprovalTargetType.USER) {
                if (lineRequest.getTargetUserId() == null) {
                    throw new CustomException(ErrorCode.INVALID_ARGUMENT);
                }
                User targetUser = getUser(lineRequest.getTargetUserId());
                line.setTargetUser(targetUser);
                line.setTargetDepartment(null);
                line.setTargetNameSnapshot(toTargetName(ApprovalTargetType.USER, targetUser, null));
            } else {
                if (lineRequest.getTargetDepartmentId() == null) {
                    throw new CustomException(ErrorCode.INVALID_ARGUMENT);
                }
                Department targetDepartment =
                        departmentRepository
                                .findById(lineRequest.getTargetDepartmentId())
                                .orElseThrow(
                                        () -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
                line.setTargetDepartment(targetDepartment);
                line.setTargetUser(null);
                line.setTargetNameSnapshot(
                        toTargetName(ApprovalTargetType.DEPARTMENT, null, targetDepartment));
            }

            newLines.add(line);
        }

        List<ApprovalDocumentLine> savedLines = approvalDocumentLineRepository.saveAll(newLines);
        return normalizeAndActivateLines(savedLines, forSubmit);
    }

    private List<ApprovalDocumentLine> normalizeAndActivateLines(
            List<ApprovalDocumentLine> lines, boolean forSubmit) {
        List<ApprovalDocumentLine> approvalLines =
                lines.stream()
                        .filter(line -> line.getRole() == ApprovalRouteRole.APPROVAL_LINE)
                        .sorted(
                                Comparator.comparing(
                                                ApprovalDocumentLine::getSequenceNo,
                                                Comparator.nullsLast(Integer::compareTo))
                                        .thenComparing(ApprovalDocumentLine::getId))
                        .toList();

        for (int i = 0; i < approvalLines.size(); i++) {
            ApprovalDocumentLine line = approvalLines.get(i);
            line.setSequenceNo(i + 1);
            line.setLineStatus(
                    i == 0 && forSubmit ? ApprovalLineStatus.PENDING : ApprovalLineStatus.WAITING);
            line.setProcessedAt(null);
            line.setProcessedByUser(null);
            line.setProcessedComment(null);
        }

        lines.stream()
                .filter(line -> line.getRole() != ApprovalRouteRole.APPROVAL_LINE)
                .forEach(
                        line -> {
                            line.setLineStatus(ApprovalLineStatus.WAITING);
                            line.setProcessedAt(null);
                            line.setProcessedByUser(null);
                            line.setProcessedComment(null);
                        });

        if (forSubmit && approvalLines.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_STEP);
        }

        return approvalDocumentLineRepository.saveAll(lines);
    }

    private String toTargetName(
            ApprovalTargetType targetType, User targetUser, Department targetDepartment) {
        if (targetType == ApprovalTargetType.DEPARTMENT) {
            if (targetDepartment == null) {
                return null;
            }
            return "[" + targetDepartment.getName().getDescription() + "]";
        }

        if (targetUser == null) {
            return null;
        }

        String departmentName =
                targetUser.getDepartment() != null
                        ? targetUser.getDepartment().getName().getDescription()
                        : "소속없음";
        String name =
                StringUtils.hasText(targetUser.getNameKor())
                        ? targetUser.getNameKor()
                        : targetUser.getNameEng();
        String position =
                targetUser.getPosition() != null ? targetUser.getPosition().getDescription() : null;

        if (StringUtils.hasText(position)) {
            return "[" + departmentName + "] " + name + " (" + position + ")";
        }
        return "[" + departmentName + "] " + name;
    }

    private User getUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private ApprovalTemplate getActiveTemplate(Long templateId) {
        ApprovalTemplate template =
                approvalTemplateRepository
                        .findById(templateId)
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_ARGUMENT));
        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }
        return template;
    }
}
