package kr.co.awesomelead.groupware_backend.domain.approval.service;

import kr.co.awesomelead.groupware_backend.domain.approval.dto.request.ApprovalConfigSaveRequestDto;
import kr.co.awesomelead.groupware_backend.domain.approval.dto.response.ApprovalConfigResponseDto;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.repository.ApprovalLineConfigRepository;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.repository.DepartmentRepository;
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
    private final DepartmentRepository departmentRepository;

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
            config.update(
                    safeTargetUserIds(request.getApprovers()),
                    safeTargetDepartmentIds(request.getApprovers()),
                    safeTargetUserIds(request.getViewers()),
                    safeTargetDepartmentIds(request.getViewers()),
                    safeTargetUserIds(request.getReferrers()),
                    safeTargetDepartmentIds(request.getReferrers()));
        } else {
            config =
                    ApprovalLineConfig.of(
                            request.getDocumentType(),
                            safeTargetUserIds(request.getApprovers()),
                            safeTargetDepartmentIds(request.getApprovers()),
                            safeTargetUserIds(request.getViewers()),
                            safeTargetDepartmentIds(request.getViewers()),
                            safeTargetUserIds(request.getReferrers()),
                            safeTargetDepartmentIds(request.getReferrers()));
            approvalLineConfigRepository.save(config);
        }

        return ApprovalConfigResponseDto.from(
                config, loadUsersByIds(config), loadDepartmentsByIds(config));
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
                                                Collections.emptyList(),
                                                Collections.emptyList(),
                                                Collections.emptyList(),
                                                Collections.emptyList(),
                                                Collections.emptyList())))
                        .toList();

        Map<Long, User> usersById = loadUsersByIds(resolvedConfigs);
        Map<Long, Department> departmentsById = loadDepartmentsByIds(resolvedConfigs);

        return resolvedConfigs.stream()
                .map(config -> ApprovalConfigResponseDto.from(config, usersById, departmentsById))
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
                                                java.util.Collections.emptyList(),
                                                java.util.Collections.emptyList(),
                                                java.util.Collections.emptyList(),
                                                java.util.Collections.emptyList(),
                                                java.util.Collections.emptyList()));

        return ApprovalConfigResponseDto.from(
                config, loadUsersByIds(config), loadDepartmentsByIds(config));
    }

    private Map<Long, User> loadUsersByIds(ApprovalLineConfig config) {
        Set<Long> userIds = new HashSet<>();
        userIds.addAll(config.getApproverTargetUserIds());
        userIds.addAll(config.getViewerTargetUserIds());
        userIds.addAll(config.getReferrerTargetUserIds());

        return loadUsersByIds(userIds);
    }

    private Map<Long, User> loadUsersByIds(List<ApprovalLineConfig> configs) {
        Set<Long> userIds = new HashSet<>();
        for (ApprovalLineConfig config : configs) {
            userIds.addAll(config.getApproverTargetUserIds());
            userIds.addAll(config.getViewerTargetUserIds());
            userIds.addAll(config.getReferrerTargetUserIds());
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

    private Map<Long, Department> loadDepartmentsByIds(ApprovalLineConfig config) {
        Set<Long> departmentIds = new HashSet<>();
        departmentIds.addAll(config.getApproverTargetDepartmentIds());
        departmentIds.addAll(config.getViewerTargetDepartmentIds());
        departmentIds.addAll(config.getReferrerTargetDepartmentIds());

        return loadDepartmentsByIds(departmentIds);
    }

    private Map<Long, Department> loadDepartmentsByIds(List<ApprovalLineConfig> configs) {
        Set<Long> departmentIds = new HashSet<>();
        for (ApprovalLineConfig config : configs) {
            departmentIds.addAll(config.getApproverTargetDepartmentIds());
            departmentIds.addAll(config.getViewerTargetDepartmentIds());
            departmentIds.addAll(config.getReferrerTargetDepartmentIds());
        }

        return loadDepartmentsByIds(departmentIds);
    }

    private Map<Long, Department> loadDepartmentsByIds(Set<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return departmentRepository.findAllById(departmentIds).stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));
    }

    private List<Long> safeTargetUserIds(
            ApprovalConfigSaveRequestDto.ApprovalTargetRequestDto targetRequestDto) {
        if (targetRequestDto == null || targetRequestDto.getTargetUserIds() == null) {
            return Collections.emptyList();
        }
        return targetRequestDto.getTargetUserIds();
    }

    private List<Long> safeTargetDepartmentIds(
            ApprovalConfigSaveRequestDto.ApprovalTargetRequestDto targetRequestDto) {
        if (targetRequestDto == null || targetRequestDto.getTargetDepartmentIds() == null) {
            return Collections.emptyList();
        }
        return targetRequestDto.getTargetDepartmentIds();
    }
}
