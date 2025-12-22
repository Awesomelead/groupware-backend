package kr.co.awesomelead.groupware_backend.domain.checksheet.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class CheckSheet {

    @Id
    private Long id;

    @Column(nullable = false)
    private String fileKey; // 근태확인표 파일 S3 키

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_sheet_id")
    @JsonBackReference
    private User user; // 근태확인표 소유 직원
}
