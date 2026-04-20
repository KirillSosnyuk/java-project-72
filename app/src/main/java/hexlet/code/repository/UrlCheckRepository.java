package hexlet.code.repository;

import hexlet.code.model.UrlCheck;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UrlCheckRepository extends BaseRepository {
    public static void save(UrlCheck urlCheck) throws Exception {
        var sql = """
                INSERT INTO url_checks (url_id, status_code, h1, title, description, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
        ) {
            var createdAt = LocalDateTime.now();

            statement.setLong(1, urlCheck.getUrlId());
            statement.setInt(2, urlCheck.getStatusCode());
            statement.setString(3, urlCheck.getH1());
            statement.setString(4, urlCheck.getTitle());
            statement.setString(5, urlCheck.getDescription());
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));

            statement.executeUpdate();

            try (var generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    urlCheck.setId(generatedKeys.getLong(1));
                    urlCheck.setCreatedAt(createdAt);
                }
            }
        }
    }

    public static List<UrlCheck> findByUrlId(Long urlId) throws Exception {
        var sql = "SELECT * FROM url_checks WHERE url_id = ? ORDER BY created_at DESC, id DESC";
        var checks = new ArrayList<UrlCheck>();

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, urlId);

            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    checks.add(buildUrlCheck(resultSet));
                }
            }
        }

        return checks;
    }

    public static Optional<UrlCheck> findLatestByUrlId(Long urlId) throws Exception {
        var sql = """
                SELECT * FROM url_checks
                WHERE url_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """;

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, urlId);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(buildUrlCheck(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    public static Map<Long, UrlCheck> findLatestChecks() throws Exception {
        var sql = """
                SELECT id, url_id, status_code, h1, title, description, created_at
                FROM (
                    SELECT *,
                           ROW_NUMBER() OVER (
                               PARTITION BY url_id
                               ORDER BY created_at DESC, id DESC
                           ) AS row_num
                    FROM url_checks
                ) AS ranked_checks
                WHERE row_num = 1
                """;

        var latestChecks = new HashMap<Long, UrlCheck>();

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql);
                var resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                var urlCheck = buildUrlCheck(resultSet);
                latestChecks.put(urlCheck.getUrlId(), urlCheck);
            }
        }

        return latestChecks;
    }

    private static UrlCheck buildUrlCheck(java.sql.ResultSet resultSet) throws Exception {
        return new UrlCheck(
                resultSet.getLong("id"),
                resultSet.getLong("url_id"),
                resultSet.getInt("status_code"),
                resultSet.getString("h1"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
