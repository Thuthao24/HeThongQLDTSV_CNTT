package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.ThongbaoRepository;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import com.example.quanlydetaisinhvien.service.notification.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:notification-disabled;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.mail.test-connection=false",
        "app.thongbao.email.outbox-enabled=false","app.thongbao.email.delivery-enabled=false","app.thongbao.email.schedule-enabled=false",
        "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationEmailDisabledTests {
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ThongbaoService notices;
    @Autowired ThongbaoRepository noticeRows;
    @Autowired NotificationEmailSchema schema;
    @Autowired NotificationEmailWorker worker;
    @Autowired NotificationEmailMonitorService monitor;

    @BeforeAll void removeOnlyTheNewTableInThisIsolatedH2Database() throws Exception {
        try(var c=source.getConnection()) { assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:notification-disabled"); }
        jdbc.execute("drop table emailthongbao");
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            var a=new Taikhoan(); a.setTendangnhap("disabled-chief"); a.setHoten("Trưởng khoa"); a.setMatkhau("unused-H2");
            a.setVaitro(Vaitro.TRUONGKHOA); a.setKichhoat(true); em.persist(a);
        });
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void chief() { SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "disabled-chief","unused",List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA")))); }

    @Test void absentMigrationDoesNotBreakExistingCrudWhenOutboxIsDisabled() {
        chief(); assertThat(schema.available()).isFalse();
        var f=new ThongbaoFormDTO(); f.setTieude("Thông báo không cần email"); f.setNoidung("Dữ liệu"); f.setRecipientRoles(DoituongThongbao.all());
        var created=notices.create(f); f.setTieude("Đã chỉnh sửa"); notices.update(created.id(),f);
        assertThat(notices.findById(created.id()).tieude()).isEqualTo("Đã chỉnh sửa");
        worker.drain(); notices.deleteById(created.id()); assertThat(noticeRows.findById(created.id())).isEmpty();
    }
    @Test void monitorExplicitlyReportsMissingSchemaWithoutQueryingANonexistentOutbox() {
        chief(); var result=monitor.read(new NotificationEmailFilterDTO());
        assertThat(result.schemaAvailable()).isFalse(); assertThat(result.deliveryEnabled()).isFalse();
        assertThat(result.items()).isEmpty(); assertThat(result.counts().total()).isZero();
    }
}
