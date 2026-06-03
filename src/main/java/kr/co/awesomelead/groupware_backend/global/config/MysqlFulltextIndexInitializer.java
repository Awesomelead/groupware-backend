package kr.co.awesomelead.groupware_backend.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlFulltextIndexInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (!isMysql()) {
            log.debug("MySQL 데이터베이스가 아니어서 FULLTEXT 인덱스 초기화를 건너뜁니다.");
            return;
        }

        createFulltextIndexIfAbsent(
                "safety_training_sessions",
                "ft_sts_title",
                "ALTER TABLE safety_training_sessions ADD FULLTEXT INDEX ft_sts_title (title)");
        createFulltextIndexIfAbsent(
                "users",
                "ft_users_name_kor",
                "ALTER TABLE users ADD FULLTEXT INDEX ft_users_name_kor (name_kor)");
    }

    private boolean isMysql() {
        try (Connection connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            return databaseProductName != null
                    && databaseProductName.toLowerCase(Locale.ROOT).contains("mysql");
        } catch (SQLException e) {
            log.warn("데이터베이스 종류 확인 중 오류가 발생해 FULLTEXT 인덱스 초기화를 건너뜁니다.", e);
            return false;
        }
    }

    private void createFulltextIndexIfAbsent(String tableName, String indexName, String ddl) {
        if (!existsTable(tableName)) {
            log.debug("테이블이 없어 FULLTEXT 인덱스 생성을 건너뜁니다. table: {}", tableName);
            return;
        }
        if (existsIndex(tableName, indexName)) {
            return;
        }

        jdbcTemplate.execute(ddl);
        log.info("FULLTEXT 인덱스를 생성했습니다. table: {}, index: {}", tableName, indexName);
    }

    private boolean existsTable(String tableName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = DATABASE()
                        AND table_name = ?
                        """,
                        Integer.class,
                        tableName);
        return count != null && count > 0;
    }

    private boolean existsIndex(String tableName, String indexName) {
        Integer count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                        AND table_name = ?
                        AND index_name = ?
                        """,
                        Integer.class,
                        tableName,
                        indexName);
        return count != null && count > 0;
    }
}
