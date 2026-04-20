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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        if (!user.hasAuthority(Authority.MANAGE_APPROVAL_LINE)) {
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

        return ApprovalConfigResponseDto.from(config, loadUsersByIds(config));
    }

    @Transactional(readOnly = true)
    public List<ApprovalConfigResponseDto> getAllConfigs() {
        List<ApprovalLineConfig> allConfigs = approvalLineConfigRepository.findAll();
        Map<DocumentType, ApprovalLineConfig> configMap =
                allConfigs.stream()
                        .collect(
                                Collectors.toMap(
                                        ApprovalLineConfig::getDocumentType, Function.identity()));

        List<ApprovalLineConfig> resolvedConfigs =
                Arrays.stream(DocumentType.values())
                        .map(
                                type ->
                                        configMap.getOrDefault(
                                                type,
                                                ApprovalLineConfig.of(
                                                        type,
                                                        Collections.emptyList(),
                                                        Collections.emptyList())))
                        .toList();

        Map<Long, User> usersById = loadUsersByIds(resolvedConfigs);

        return resolvedConfigs.stream()
                .map(config -> ApprovalConfigResponseDto.from(config, usersById))
                .toList();
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

        return ApprovalConfigResponseDto.from(config, loadUsersByIds(config));
    }

    private Map<Long, User> loadUsersByIds(ApprovalLineConfig config) {
        Set<Long> userIds = new HashSet<>();
        userIds.addAll(config.getApproverIds());
        userIds.addAll(config.getReferrerIds());

        return loadUsersByIds(userIds);
    }

    private Map<Long, User> loadUsersByIds(List<ApprovalLineConfig> configs) {
        Set<Long> userIds = new HashSet<>();
        for (ApprovalLineConfig config : configs) {
            userIds.addAll(config.getApproverIds());
            userIds.addAll(config.getReferrerIds());
        }

        return loadUsersByIds(userIds);
    }

    private Map<Long, User> loadUsersByIds(Set<Long> userIds) {

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
