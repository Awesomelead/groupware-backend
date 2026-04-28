package kr.co.awesomelead.groupware_backend.domain.approval.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "approval_line_configs")
public class ApprovalLineConfig {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    @ElementCollection
    @CollectionTable(
            name = "approval_config_approvers",
            joinColumns = @JoinColumn(name = "document_type"))
    @OrderColumn(name = "sequence_order")
    @Column(name = "approver_ids")
    private List<Long> approverTargetUserIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "approval_config_approver_departments",
            joinColumns = @JoinColumn(name = "document_type"))
    @Column(name = "department_ids")
    private List<Long> approverTargetDepartmentIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "approval_config_viewers",
            joinColumns = @JoinColumn(name = "document_type"))
    @Column(name = "viewer_ids")
    private List<Long> viewerTargetUserIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "approval_config_viewer_departments",
            joinColumns = @JoinColumn(name = "document_type"))
    @Column(name = "department_ids")
    private List<Long> viewerTargetDepartmentIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "approval_config_referrers",
            joinColumns = @JoinColumn(name = "document_type"))
    @Column(name = "referrer_ids")
    private List<Long> referrerTargetUserIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "approval_config_referrer_departments",
            joinColumns = @JoinColumn(name = "document_type"))
    @Column(name = "department_ids")
    private List<Long> referrerTargetDepartmentIds = new ArrayList<>();

    public static ApprovalLineConfig of(
            DocumentType documentType,
            List<Long> approverTargetUserIds,
            List<Long> approverTargetDepartmentIds,
            List<Long> viewerTargetUserIds,
            List<Long> viewerTargetDepartmentIds,
            List<Long> referrerTargetUserIds,
            List<Long> referrerTargetDepartmentIds) {
        ApprovalLineConfig config = new ApprovalLineConfig();
        config.documentType = documentType;
        config.approverTargetUserIds = new ArrayList<>(approverTargetUserIds);
        config.approverTargetDepartmentIds = new ArrayList<>(approverTargetDepartmentIds);
        config.viewerTargetUserIds = new ArrayList<>(viewerTargetUserIds);
        config.viewerTargetDepartmentIds = new ArrayList<>(viewerTargetDepartmentIds);
        config.referrerTargetUserIds = new ArrayList<>(referrerTargetUserIds);
        config.referrerTargetDepartmentIds = new ArrayList<>(referrerTargetDepartmentIds);
        return config;
    }

    public void update(
            List<Long> approverTargetUserIds,
            List<Long> approverTargetDepartmentIds,
            List<Long> viewerTargetUserIds,
            List<Long> viewerTargetDepartmentIds,
            List<Long> referrerTargetUserIds,
            List<Long> referrerTargetDepartmentIds) {
        this.approverTargetUserIds.clear();
        this.approverTargetUserIds.addAll(approverTargetUserIds);

        this.approverTargetDepartmentIds.clear();
        this.approverTargetDepartmentIds.addAll(approverTargetDepartmentIds);

        this.viewerTargetUserIds.clear();
        this.viewerTargetUserIds.addAll(viewerTargetUserIds);

        this.viewerTargetDepartmentIds.clear();
        this.viewerTargetDepartmentIds.addAll(viewerTargetDepartmentIds);

        this.referrerTargetUserIds.clear();
        this.referrerTargetUserIds.addAll(referrerTargetUserIds);

        this.referrerTargetDepartmentIds.clear();
        this.referrerTargetDepartmentIds.addAll(referrerTargetDepartmentIds);
    }
}
