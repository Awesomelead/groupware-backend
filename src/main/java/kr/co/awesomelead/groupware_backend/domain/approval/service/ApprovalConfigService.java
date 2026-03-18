package kr.co.awesomelead.groupware_backend.domain.approval.service;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalConfigSaveRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalConfigResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalLineConfigRepository;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.repository.UserRepository;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApprovalConfigService {

    private final ApprovalLineConfigRepository approvalLineConfigRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApprovalConfigResponseDto saveConfig(ApprovalConfigSaveRequestDto request, Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.hasAuthority(Authority.MANAGE_APPROVAL_CONFIG)) {
            throw new CustomException(ErrorCode.NO_AUTHORITY_FOR_APPROVAL_CONFIG);
        }

        Optional<ApprovalLineConfig> existing =
                approvalLineConfigRepository.findById(request.getDocumentType());

        ApprovalLineConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.update(request.getApproverIds(), request.getReferrerIds());
        } else {
            config =
                    ApprovalLineConfig.of(
                            request.getDocumentType(),
                            request.getApproverIds(),
                            request.getReferrerIds());
            approvalLineConfigRepository.save(config);
        }

        return ApprovalConfigResponseDto.from(config);
    }

    @Transactional(readOnly = true)
    public ApprovalConfigResponseDto getConfig(DocumentType documentType) {
        ApprovalLineConfig config =
                approvalLineConfigRepository
                        .findById(documentType)
                        .orElseGet(
                                () ->
                                        ApprovalLineConfig.of(
                                                documentType,
                                                java.util.Collections.emptyList(),
                                                java.util.Collections.emptyList()));

        return ApprovalConfigResponseDto.from(config);
    }
}
