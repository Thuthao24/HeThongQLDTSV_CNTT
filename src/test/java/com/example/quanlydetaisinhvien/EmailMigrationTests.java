package com.example.quanlydetaisinhvien;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

/** Checks the actual review-only SQL against an isolated in-memory database, never MySQL. */
class EmailMigrationTests {
    @Test void resultMigrationSupportsAllStatesAndEnforcesRecipientUniqueness() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:email-migrations;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(source);
        try {
            for (String parent:new String[]{"sinhvien","ketqua","dangkydetai","hoidong"}) {
                jdbc.execute("create table "+parent+" (Id bigint primary key)");
                jdbc.update("insert into "+parent+" (Id) values (1)");
            }
            for (String file:new String[]{"V002__phanconghoidong_emailketqua.sql"}) {
                String sql=Files.readString(Path.of("database/migrations/pending",file))
                        .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
                new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(source);
            }
            String insertResult="insert into emailketqua (Ketquaid,Sinhvienid,Tieude,Noidung,Trangthai,Ngaytao) "
                    +"values (1,1,'Tiêu đề','Nội dung',?,CURRENT_TIMESTAMP)";
            jdbc.update(insertResult,"PENDING");
            assertThatThrownBy(()->jdbc.update(insertResult,"PENDING")).isInstanceOf(DataIntegrityViolationException.class);
            for (String status:new String[]{"PENDING","SENDING","SENT","FAILED","UNCERTAIN","MISSING_EMAIL"}) {
                jdbc.update("update emailketqua set Trangthai=?",status);
                assertThat(jdbc.queryForObject("select Trangthai from emailketqua",String.class)).isEqualTo(status);
            }
            assertThatThrownBy(()->jdbc.update("delete from ketqua where Id=1")).isInstanceOf(DataIntegrityViolationException.class);
        } finally { jdbc.execute("drop all objects"); }
    }
}
