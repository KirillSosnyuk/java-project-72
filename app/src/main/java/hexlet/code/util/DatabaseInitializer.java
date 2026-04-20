package hexlet.code.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseInitializer {
    public static void init(javax.sql.DataSource dataSource) throws Exception {
        try (
                var inputStream = DatabaseInitializer.class.getClassLoader().getResourceAsStream("schema.sql");
                var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()
        ) {
            var sql = reader.lines().collect(Collectors.joining("\n"));
            statement.execute(sql);
        }
    }
}
