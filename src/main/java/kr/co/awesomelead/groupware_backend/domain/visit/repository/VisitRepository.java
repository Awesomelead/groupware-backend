package kr.co.awesomelead.groupware_backend.domain.visit.repository;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByVisitor(Visitor visitor);

    @Query("SELECT v FROM Visit v " +
        "JOIN FETCH v.user u " +
        "JOIN FETCH u.department d " +
        "WHERE d.id = :departmentId")
    List<Visit> findAllByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT v FROM Visit v JOIN v.user u WHERE u.department.id IN :departmentIds")
    List<Visit> findAllByDepartmentIdIn(@Param("departmentIds") List<Long> departmentIds);
}
