package com.example.quanlydetaisinhvien.service.notification;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.*;

@Component
public class NotificationEmailSchema {
    private final DataSource source;
    public NotificationEmailSchema(DataSource source) { this.source = source; }
    public boolean available() {
        Connection c = DataSourceUtils.getConnection(source);
        try (var tables = c.getMetaData().getTables(c.getCatalog(), c.getSchema(), "%", new String[]{"TABLE"})) {
            while (tables.next()) if ("emailthongbao".equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
            return false;
        } catch (SQLException ex) {
            throw new DataAccessResourceFailureException("Không thể kiểm tra schema email thông báo.", ex);
        } finally { DataSourceUtils.releaseConnection(c, source); }
    }
    public void requireAvailable() {
        if (!available()) throw new DataAccessResourceFailureException("Chưa có bảng Emailthongbao; cần duyệt và triển khai migration riêng.");
    }
}
