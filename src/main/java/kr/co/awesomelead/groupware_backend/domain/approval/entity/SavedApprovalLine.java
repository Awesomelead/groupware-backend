package kr.co.awesomelead.groupware_backend.domain.approval.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saved_approval_lines")
public class SavedApprovalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title; // ex) "oo부서 승인 라인"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // DB 컬럼명: user_id
    private User user;

    @OneToMany(mappedBy = "savedLine", cascade = CascadeType.ALL)
    private List<SavedApprovalLineDetail> details = new ArrayList<>();

    public void addDetail(SavedApprovalLineDetail detail) {
        this.details.add(detail);
        detail.setSavedLine(this);
    }
}
