package kr.co.awesomelead.groupware_backend.domain.notice.respository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.request.NoticeSearchConditionDto;
import kr.co.awesomelead.groupware_backend.domain.notice.dto.response.NoticeSummaryDto;
import kr.co.awesomelead.groupware_backend.domain.notice.entity.QNotice;
import kr.co.awesomelead.groupware_backend.domain.notice.enums.NoticeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class NoticeQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<NoticeSummaryDto> findNoticesWithFilters(NoticeSearchConditionDto conditionDto,
        Company company,
        Pageable pageable) {
        QNotice notice = QNotice.notice;

        List<NoticeSummaryDto> result = queryFactory
            .select(Projections.constructor(NoticeSummaryDto.class,
                notice.id,
                notice.type,
                notice.title,
                notice.pinned,
                notice.updatedDate
            ))
            .from(notice)
            .where(
                typeEq(conditionDto.getType()),
                searchKeyword(conditionDto.getKeyword(), conditionDto.getSearchType()),
                companyContains(company)
            )
            .orderBy(notice.pinned.desc(), notice.updatedDate.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        return PageableExecutionUtils.getPage(
            result,
            pageable,
            () -> {
                Long totalCount = queryFactory
                    .select(notice.count())
                    .from(notice)
                    .where(
                        typeEq(conditionDto.getType()),
                        searchKeyword(conditionDto.getKeyword(), conditionDto.getSearchType()),
                        companyContains(company)
                    )
                    .fetchOne();

                return totalCount != null ? totalCount : 0L;
            }
        );
    }

    public List<NoticeSummaryDto> findTop3Notices(Company company) {
        QNotice notice = QNotice.notice;

        return queryFactory
            .select(Projections.constructor(NoticeSummaryDto.class,
                notice.id,
                notice.type,
                notice.title,
                notice.pinned,
                notice.updatedDate
            ))
            .from(notice)
            .where(
                companyContains(company)
            )
            .orderBy(notice.pinned.desc(), notice.updatedDate.desc())
            .limit(3)
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
