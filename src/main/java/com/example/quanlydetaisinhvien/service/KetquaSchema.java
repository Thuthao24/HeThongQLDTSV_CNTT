package com.example.quanlydetaisinhvien.service;

import org.springframework.stereotype.Component;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.dao.DataAccessException;
import javax.sql.DataSource;
import java.sql.SQLException;

/** Read-only schema check: missing migrations must not break the Step 1 list. */
@Component
public class KetquaSchema {
    private final DataSource dataSource;

    public KetquaSchema(DataSource dataSource) { this.dataSource = dataSource; }

    public boolean hasTable(String name) {
        java.sql.Connection connection = null;
        try {
            connection = DataSourceUtils.getConnection(dataSource);
            try (var tables = connection.getMetaData().getTables(connection.getCatalog(), null, "%", null)) {
                while (tables.next()) {
                    String type = tables.getString("TABLE_TYPE");
                    if (name.equalsIgnoreCase(tables.getString("TABLE_NAME"))
                            && ("TABLE".equalsIgnoreCase(type) || "BASE TABLE".equalsIgnoreCase(type))) return true;
                }
            }
            return false;
        } catch (SQLException | DataAccessException ex) {
            return false;
        } finally {
            if (connection != null) DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
