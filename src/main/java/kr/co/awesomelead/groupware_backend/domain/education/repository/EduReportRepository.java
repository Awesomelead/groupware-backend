package kr.co.awesomelead.groupware_backend.domain.education.repository;

import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.education.entity.EduReport;
import kr.co.awesomelead.groupware_backend.domain.education.enums.EduType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EduReportRepository extends JpaRepository<EduReport, Long> {

    @Query(
        "SELECT new kr.co.awesomelead.groupware_backend.domain.education.dto.response.EduReportSummaryDto("
            + "r.id, r.title, r.eduType, r.eduDate, "
            + "COALESCE((SELECT ea.attendance FROM EduAttendance ea WHERE ea.eduReport = r AND ea.user = :user), false), "
            + "r.pinned) "
            + "FROM EduReport r "
            + "WHERE (:type IS NULL OR r.eduType = :type) "
            + "AND (r.eduType != 'DEPARTMENT' OR r.department = :dept) "
            + "ORDER BY r.pinned DESC, r.eduDate DESC")
    List<EduReportSummaryDto> findEduReportsWithFilters(
        @Param("type") EduType type,
        @Param("dept") Department dept,
        @Param("user") User user
    );

}
