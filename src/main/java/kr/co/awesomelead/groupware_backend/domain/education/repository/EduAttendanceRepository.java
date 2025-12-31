package kr.co.awesomelead.groupware_backend.domain.education.repository;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduAttendance;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EduAttendanceRepository extends JpaRepository<EduAttendance, Long> {

    boolean existsByEduReportAndUser(EduReport report, User user);

    @Query("SELECT ea FROM EduAttendance ea " +
        "JOIN FETCH ea.user " +
        "WHERE ea.eduReport.id = :reportId")
    List<EduAttendance> findAllByEduReportIdWithUser(@Param("reportId") Long reportId);
}
