package com.example.quanlydetaisinhvien;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class ThongbaoReadMigrationTests {
    @Test void v004ThenV005CaptureDurableIdBoundaryAndEnforceReceiptForeignKeysAndUniqueness() throws Exception {
        try(var source=new SingleConnectionDataSource("jdbc:h2:mem:notice-read-migration;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","",true)){
            var jdbc=new JdbcTemplate(source);
            jdbc.execute("create table taikhoan(Id bigint primary key)");
            jdbc.execute("create table thongbao(Id bigint auto_increment primary key,Tieude varchar(255))");
            jdbc.execute("create table emailthongbao(Id bigint primary key,Trangthai varchar(20))");
            jdbc.execute("create table emailketqua(Id bigint primary key,Trangthai varchar(20))");
            jdbc.execute("create table phanconghoidong(Id bigint primary key)");
            jdbc.update("insert into taikhoan values (1),(2)");
            jdbc.update("insert into thongbao(Id,Tieude) values (10,'Lịch sử 10'),(50,'Lịch sử 50')");
            jdbc.update("insert into emailthongbao values (1,'PENDING')");jdbc.update("insert into emailketqua values (2,'SENT')");jdbc.update("insert into phanconghoidong values (3)");
            var old=jdbc.queryForList("select * from thongbao order by Id");
            for(var file:new String[]{"V004__thongbao_doituong.sql","V005__thongbao_dadoc.sql"}){
                var sql=Files.readString(Path.of("database/migrations/pending",file)).replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
                new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(source);
            }
            assertThat(jdbc.queryForList("select * from thongbao order by Id")).isEqualTo(old);
            assertThat(jdbc.queryForObject("select Thongbaoidmoc from thongbaodocmoc where Id=1",Long.class)).isEqualTo(50);
            assertThat(jdbc.queryForObject("select count(*) from thongbaodadoc",Long.class)).isZero();
            jdbc.update("insert into thongbao(Tieude) values ('Mới sau kích hoạt')");
            Long next=jdbc.queryForObject("select max(Id) from thongbao",Long.class);assertThat(next).isGreaterThan(50L);
            jdbc.update("insert into thongbaodadoc values (1,?,CURRENT_TIMESTAMP)",next);
            assertThatThrownBy(()->jdbc.update("insert into thongbaodadoc values (1,?,CURRENT_TIMESTAMP)",next)).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(()->jdbc.update("insert into thongbaodadoc values (999,?,CURRENT_TIMESTAMP)",next)).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(()->jdbc.update("insert into thongbaodadoc values (1,9999,CURRENT_TIMESTAMP)")).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(()->jdbc.update("insert into thongbaodocmoc values (2,0,CURRENT_TIMESTAMP)")).isInstanceOf(DataIntegrityViolationException.class);
            jdbc.update("delete from thongbao where Id=?",next);assertThat(jdbc.queryForObject("select count(*) from thongbaodadoc",Long.class)).isZero();
            jdbc.update("insert into thongbaodadoc values (2,50,CURRENT_TIMESTAMP)");jdbc.update("delete from taikhoan where Id=2");assertThat(jdbc.queryForObject("select count(*) from thongbaodadoc",Long.class)).isZero();
            assertThat(jdbc.queryForObject("select Thongbaoidmoc from thongbaodocmoc where Id=1",Long.class)).isEqualTo(50);
            assertThat(jdbc.queryForObject("select Trangthai from emailthongbao",String.class)).isEqualTo("PENDING");
            assertThat(jdbc.queryForObject("select Trangthai from emailketqua",String.class)).isEqualTo("SENT");assertThat(jdbc.queryForObject("select count(*) from phanconghoidong",Long.class)).isEqualTo(1);
        }
    }
}
