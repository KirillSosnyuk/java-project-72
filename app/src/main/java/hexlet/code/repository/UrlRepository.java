package hexlet.code.repository;

import hexlet.code.model.Url;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UrlRepository extends BaseRepository {
    public static void save(Url url) throws Exception {
        var sql = "INSERT INTO urls (name, created_at) VALUES (?, ?)";

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
        ) {
            var createdAt = LocalDateTime.now();
            statement.setString(1, url.getName());
            statement.setTimestamp(2, Timestamp.valueOf(createdAt));
            statement.executeUpdate();

            try (var generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    url.setId(generatedKeys.getLong(1));
                    url.setCreatedAt(createdAt);
                }
            }
        }
    }

    public static Optional<Url> find(Long id) throws Exception {
        var sql = "SELECT * FROM urls WHERE id = ?";

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setLong(1, id);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    var url = new Url(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    );
                    return Optional.of(url);
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<Url> findByName(String name) throws Exception {
        var sql = "SELECT * FROM urls WHERE name = ?";

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, name);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    var url = new Url(
                            resultSet.getLong("id"),
                            resultSet.getString("name"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    );
                    return Optional.of(url);
                }
            }
        }

        return Optional.empty();
    }

    public static List<Url> getEntities() throws Exception {
        var sql = "SELECT * FROM urls ORDER BY created_at DESC, id DESC";
        var urls = new ArrayList<Url>();

        try (
                var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql);
                var resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                var url = new Url(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getTimestamp("created_at").toLocalDateTime()
                );
                urls.add(url);
            }
        }

        return urls;
    }
}
