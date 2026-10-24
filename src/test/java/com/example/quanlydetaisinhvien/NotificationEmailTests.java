package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import com.example.quanlydetaisinhvien.service.mail.ResultMailSender;
import com.example.quanlydetaisinhvien.service.notification.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** New website-only policy, including protection of every historical outbox state with old flags ON. */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:notification-retired;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never",
    "app.thongbao.email.outbox-enabled=true","app.thongbao.email.delivery-enabled=true",
    "app.thongbao.email.schedule-enabled=true","app.thongbao.email.mode=SYSTEM",
    "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"})
@Import(NotificationEmailTests.MailConfig.class)
class NotificationEmailTests {
    @TestConfiguration(proxyBeanMethods=false) static class MailConfig {
        @Bean @Primary FakeSender notificationFake(JdbcTemplate jdbc) { return new FakeSender(jdbc); }
        @Bean @Primary ResultMailSender resultMailMustNotRun() { return message->{throw new AssertionError("No real result delivery");}; }
    }
    static class FakeSender implements NotificationMailSender {
        final List<Message> sent=new java.util.concurrent.CopyOnWriteArrayList<>();
        FakeSender(JdbcTemplate jdbc) {}
        public void send(Message message) { sent.add(message); throw new AssertionError("Retired notification transport was invoked"); }
        void reset() { sent.clear(); }
    }
    @Autowired ThongbaoService notices;
    @Autowired EmailthongbaoRepository emails;
    @Autowired NotificationEmailQueue queue;
    @Autowired NotificationEmailWorker worker;
    @Autowired NotificationEmailScheduler scheduler;
    @Autowired NotificationEmailOutbox outbox;
    @Autowired NotificationEmailMonitorService monitor;
    @Autowired FakeSender fake;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired DataSource data;
    @Autowired PlatformTransactionManager transactions;
    TransactionTemplate tx;
    @BeforeEach void fixture() throws Exception {
        try(var c=data.getConnection()){assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:notification-retired");}
        tx=new TransactionTemplate(transactions);fake.reset();
        tx.executeWithoutResult(s->{
            em.createQuery("delete from Emailthongbao").executeUpdate();em.createQuery("delete from Thongbao").executeUpdate();em.createQuery("delete from Taikhoan").executeUpdate();
            var a=new Taikhoan();a.setTendangnhap("retired-chief");a.setMatkhau("H2-only");a.setHoten("Trưởng khoa H2");a.setVaitro(Vaitro.TRUONGKHOA);a.setKichhoat(true);em.persist(a);
        });
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("retired-chief",null,List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
    }
    @AfterEach void clear(){scheduler.stop();SecurityContextHolder.clearContext();}
    Long create(){var f=new ThongbaoFormDTO();f.setTieude("Chỉ trên website");f.setNoidung("Nội dung");f.setRecipientRoles(DoituongThongbao.all());return notices.create(f).id();}
    @Test void createAndLegacyEnqueueNeverCreateAnEmailEvenWithAllOldFlagsOn(){
        Long id=create();tx.executeWithoutResult(s->outbox.enqueue(em.find(Thongbao.class,id),"Trưởng khoa"));
        assertThat(emails.count()).isZero();assertThat(fake.sent).isEmpty();
        var result=monitor.read(new NotificationEmailFilterDTO());assertThat(result.outboxEnabled()).isFalse();assertThat(result.deliveryEnabled()).isFalse();
    }
    @ParameterizedTest @EnumSource(TrangthaiEmailthongbao.class)
    void workerRecoverySchedulerAndClaimCannotTouchAnyHistoricalState(TrangthaiEmailthongbao state){
        Long noticeId=create();
        Long id=tx.execute(s->{
            var row=new Emailthongbao();row.setThongbaoid(noticeId);row.setThongbaoSnapshotId(noticeId);row.setSinhvienid(123L);
            row.setNguoinhan("Historical snapshot");row.setEmail("never-send@example.invalid");row.setTieude("Lịch sử");row.setNoidung("Không được sửa");
            row.setTrangthai(state);row.setSolanthu(state==TrangthaiEmailthongbao.PENDING?0:2);row.setNgaytao(LocalDateTime.of(2025,1,1,8,0));
            row.setLanthu(LocalDateTime.of(2025,1,1,9,0));row.setLoi("Historical reason");if(state==TrangthaiEmailthongbao.SENT)row.setNgaygui(LocalDateTime.of(2025,1,1,9,0));
            return emails.saveAndFlush(row).getId();
        });
        var before=jdbc.queryForList("select * from emailthongbao order by Id");
        scheduler.start();assertThat(scheduler.isRunning()).isFalse();worker.drain();worker.drain();queue.recover();
        assertThat(queue.pending()).isEmpty();assertThat(queue.claim(id)).isNull();
        assertThat(jdbc.queryForList("select * from emailthongbao order by Id")).isEqualTo(before);assertThat(fake.sent).isEmpty();
        assertThat(monitor.read(new NotificationEmailFilterDTO()).counts().total()).isEqualTo(1);
    }
}
