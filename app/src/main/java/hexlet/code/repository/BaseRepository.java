package hexlet.code.repository;

import lombok.Setter;

import javax.sql.DataSource;

public abstract class BaseRepository {
    @Setter
    protected static DataSource dataSource;

}
