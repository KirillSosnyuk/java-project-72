package hexlet.code.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceFactory {
    private static final String DB_URL_PROPERTY = "JDBC_DATABASE_URL";

    private static final String DEFAULT_DB_URL = "jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;";

    public static DataSource getDataSource() {
        var jdbcUrl = resolveDbUrl(
                System.getProperty(DB_URL_PROPERTY),
                System.getenv(DB_URL_PROPERTY)
        );

        var config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);

        return new HikariDataSource(config);
    }

    private static String resolveDbUrl(String fromProperty, String fromEnv) {
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        return DEFAULT_DB_URL;
    }
}
