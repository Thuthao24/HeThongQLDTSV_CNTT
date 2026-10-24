package com.example.quanlydetaisinhvien;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ByteArrayResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class ThongbaoAudienceMigrationTests {
    @Test void reviewedV004PreservesHistoryConstrainsAudienceAndCascadesOnlyRoleLinks() throws Exception {
        try(var ds=new org.springframework.jdbc.datasource.SingleConnectionDataSource("jdbc:h2:mem:module15-v004;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","",true)) {
        var jdbc=new JdbcTemplate(ds);
        jdbc.execute("create table thongbao (Id bigint primary key,Tieude varchar(255),Noidung text,Nguoidangid bigint,Ngaydang timestamp)");
        jdbc.execute("create table emailthongbao (Id bigint primary key,Thongbaoid bigint,Trangthai varchar(20))");
        jdbc.execute("create table emailketqua (Id bigint primary key,Snapshot varchar(100))");
        jdbc.execute("create table phanconghoidong (Id bigint primary key,Snapshot varchar(100))");
        jdbc.update("insert into thongbao values (1,'Lịch sử tiếng Việt','Không sửa',10,'2025-09-01 08:00:00')");
        jdbc.update("insert into emailthongbao values (7,1,'PENDING')");
        jdbc.update("insert into emailketqua values (8,'KEEP_RESULT')");jdbc.update("insert into phanconghoidong values (9,'KEEP_ASSIGNMENT')");
        var notice=jdbc.queryForList("select * from thongbao");var mail=jdbc.queryForList("select * from emailthongbao");
        var result=jdbc.queryForList("select * from emailketqua");var assignment=jdbc.queryForList("select * from phanconghoidong");
        String ddl=Files.readString(Path.of("database/migrations/pending/V004__thongbao_doituong.sql"))
                .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
        new ResourceDatabasePopulator(new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8))).execute(ds);
        assertThat(jdbc.queryForList("select * from thongbao")).isEqualTo(notice);assertThat(jdbc.queryForList("select * from emailthongbao")).isEqualTo(mail);
        assertThat(jdbc.queryForList("select * from emailketqua")).isEqualTo(result);assertThat(jdbc.queryForList("select * from phanconghoidong")).isEqualTo(assignment);
        assertThat(jdbc.queryForObject("select count(*) from thongbaodoituong",Long.class)).isZero();
        jdbc.update("insert into thongbaodoituong values (1,'SINHVIEN')");
        assertThatThrownBy(()->jdbc.update("insert into thongbaodoituong values (1,'SINHVIEN')")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("insert into thongbaodoituong values (1,'TRUONGKHOA')")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("insert into thongbaodoituong values (999,'GIANGVIEN')")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        jdbc.update("delete from thongbao where Id=1");assertThat(jdbc.queryForObject("select count(*) from thongbaodoituong",Long.class)).isZero();
        assertThat(jdbc.queryForList("select * from emailthongbao")).isEqualTo(mail);assertThat(jdbc.queryForList("select * from emailketqua")).isEqualTo(result);
        }
    }
}
