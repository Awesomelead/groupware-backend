package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.QNotice;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class NoticeQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<NoticeSummaryDto> findNoticesWithFilters(NoticeType type, String keyword,
        String searchType,
        Company company) {
        QNotice notice = QNotice.notice;

        return queryFactory
            .select(Projections.constructor(NoticeSummaryDto.class,
                notice.id,
                notice.type,
                notice.title,
                notice.updatedDate
            ))
            .from(notice)
            .where(
                typeEq(type),
                searchKeyword(keyword, searchType),
                companyContains(company)
            )
            .orderBy(notice.pinned.desc(), notice.updatedDate.desc())
            .fetch();
    }

    private BooleanExpression typeEq(NoticeType type) {
        return type != null ? QNotice.notice.type.eq(type) : null;
    }

    private BooleanExpression companyContains(Company company) {
        if (company == null) {
            return null;
        }

        return Expressions.stringTemplate("{0}", QNotice.notice.targetCompanies)
            .contains(company.name());
    }

    private BooleanExpression searchKeyword(String keyword, String searchType) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        QNotice n = QNotice.notice;

        return switch (searchType != null ? searchType : "ALL") {
            case "TITLE" -> n.title.contains(keyword);
            case "CONTENT" -> n.content.contains(keyword);
            case "AUTHOR" -> n.author.nameKor.contains(keyword);
            default -> n.title.contains(keyword).or(n.content.contains(keyword));
        };
    }

}
