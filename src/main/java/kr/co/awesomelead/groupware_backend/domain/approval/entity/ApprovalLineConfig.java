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
    private List<Long> approverIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "approval_config_referrers",
            joinColumns = @JoinColumn(name = "document_type"))
    @Column(name = "referrer_ids")
    private List<Long> referrerIds = new ArrayList<>();

    public static ApprovalLineConfig of(
            DocumentType documentType, List<Long> approverIds, List<Long> referrerIds) {
        ApprovalLineConfig config = new ApprovalLineConfig();
        config.documentType = documentType;
        config.approverIds = new ArrayList<>(approverIds);
        config.referrerIds = new ArrayList<>(referrerIds);
        return config;
    }

    public void update(List<Long> approverIds, List<Long> referrerIds) {
        this.approverIds.clear();
        this.approverIds.addAll(approverIds);
        this.referrerIds.clear();
        this.referrerIds.addAll(referrerIds);
    }
}
