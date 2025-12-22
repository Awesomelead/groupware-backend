package kr.co.awesomelead.groupware_backend.domain.department.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Company company; // 소속 회사 (AWESOME, MARUI)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonBackReference // 자식 -> 부모 순환참조 방지
    private Department parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @JsonManagedReference // 부모 -> 자식 순환참조 허용 (직렬화 방향)
    private List<Department> children = new ArrayList<>();

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<User> users = new ArrayList<>();

    public void setParent(Department parent) {
        this.parent = parent;
        if (parent != null) {
            parent.getChildren().add(this);
        }
    }

    // Service 로직 예시

    // 1. 부모가 없는 최상위 부서(Root)들만 조회
    //List<Department> roots = departmentRepository.findByParentIsNullAndCompany(Company.AWESOME);
    // 2. 프론트엔드로 리턴하면
    // roots.children -> 그 밑에 팀들이 줄줄이 딸려 나옴 (Recursive)
}
