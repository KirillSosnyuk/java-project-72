package hexlet.code.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DataSourceFactory {
    private static final String DB_URL_PROPERTY = "JDBC_DATABASE_URL";

    private static final String DEFAULT_DB_URL = "jdbc:postgresql://admin:"
            + "WTALZycWW7UNmpISKsQJOsoIUsg6F6xK@dpg-d7ch1ekp3tds739vggk0-a.oregon-postgres.render.com/db_uip0";

    public static DataSource getDataSource() {
        var databaseUrlFromProperty = System.getProperty(DB_URL_PROPERTY);
        var databaseUrlFromEnv = System.getenv(DB_URL_PROPERTY);

        var jdbcUrl = databaseUrlFromProperty != null && !databaseUrlFromProperty.isBlank()
                ? databaseUrlFromProperty
                : databaseUrlFromEnv != null && !databaseUrlFromEnv.isBlank()
                ? databaseUrlFromEnv
                : DEFAULT_DB_URL;

        var config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);

        if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            config.setDriverClassName("org.postgresql.Driver");
        } else if (jdbcUrl.startsWith("jdbc:h2:")) {
            config.setDriverClassName("org.h2.Driver");
        }

        return new HikariDataSource(config);
    }
}
