package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.mail.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:result-email-scope;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "spring.mail.test-connection=false", "app.ketqua.email.enabled=true",
        "app.ketqua.email.scheduling-enabled=false", "app.ketqua.email.test-mode=true",})
@Import(KetquaEmailScopeTests.FakeMailConfig.class)
class KetquaEmailScopeTests {
    static final AtomicLong selectedId=new AtomicLong();
    static final AtomicReference<String> allowedAddress=new AtomicReference<>("approved@example.invalid");
    @DynamicPropertySource static void scope(DynamicPropertyRegistry registry) {
        registry.add("app.ketqua.email.outbox-id",selectedId::get);
        registry.add("app.ketqua.email.allowlist",allowedAddress::get);
    }
    @Autowired EmailketquaRepository emails;
    @Autowired KetquaRepository results;
    @Autowired KetquaEmailQueue queue;
    @Autowired KetquaEmailWorker worker;
    @Autowired FakeMail fake;
    Long oldAllowed, oldOther, selected;

    @TestConfiguration(proxyBeanMethods=false) static class FakeMailConfig {
        @Bean @Primary FakeMail scopeFakeMail() { return new FakeMail(); }
    }
    static class FakeMail implements ResultMailSender {
        final List<Message> sent=new CopyOnWriteArrayList<>();
        @Override public void send(Message message) { sent.add(message); }
    }

    @BeforeEach void fixture() {
        selectedId.set(0); allowedAddress.set("approved@example.invalid"); fake.sent.clear();
        emails.deleteAll(); results.deleteAll();
        var result=new Ketqua(); result.setDangkydetaiid(1L); result.setDacongbo(true);
        result.setNgaycongbo(LocalDateTime.now()); result=results.saveAndFlush(result);
        oldAllowed=row(result.getId(),1L,"approved@example.invalid").getId();
        oldOther=row(result.getId(),2L,"other@example.invalid").getId();
        selected=row(result.getId(),3L,"approved@example.invalid").getId(); selectedId.set(selected);
    }
    Emailketqua row(Long result,Long student,String address) {
        var email=new Emailketqua(); email.setKetquaid(result); email.setSinhvienid(student); email.setEmail(address);
        email.setTieude("Kết quả thử"); email.setNoidung("Nội dung thử riêng cho sinh viên");
        email.setNgaytao(LocalDateTime.now()); email.setTrangthai(TrangthaiEmailketqua.PENDING);
        return emails.saveAndFlush(email);
    }

    @Test void safeTestSendsOnlyTheSelectedOutboxRowAndLeavesOldPendingMailUntouched() {
        worker.drain(); worker.afterStartup();
        assertThat(fake.sent).hasSize(1).extracting(ResultMailSender.Message::id).containsExactly(selected);
        assertThat(emails.findById(selected).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        assertThat(emails.findAllById(List.of(oldAllowed,oldOther)))
                .allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.PENDING && e.getSolanthu()==0);
    }
    @Test void directClaimCannotBypassTheSelectedOutboxScope() {
        assertThat(queue.claim(oldAllowed)).isNull(); assertThat(queue.claim(oldOther)).isNull();
        assertThat(emails.findAll()).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.PENDING);
    }
    @Test void missingSelectionOrEmptyAllowlistNeverDrainsTheBacklog() {
        selectedId.set(0); worker.drain();
        selectedId.set(selected); allowedAddress.set(""); worker.drain();
        assertThat(fake.sent).isEmpty();
        assertThat(emails.findAll()).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.PENDING);
    }
    @Test void selectedAddressMustMatchTheAllowlist() {
        allowedAddress.set("unapproved@example.invalid"); worker.drain();
        assertThat(queue.claim(selected)).isNull(); assertThat(fake.sent).isEmpty();
        assertThat(emails.findAll()).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.PENDING);
    }
    @Test void scopedRecoveryDoesNotModifyAnotherWorkersOldSendingClaim() {
        var unrelated=emails.findById(oldAllowed).orElseThrow();
        unrelated.setTrangthai(TrangthaiEmailketqua.SENDING); unrelated.setLanthu(LocalDateTime.now().minusHours(1));
        unrelated.setSolanthu(1); emails.saveAndFlush(unrelated);
        worker.drain();
        assertThat(emails.findById(oldAllowed).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENDING);
        assertThat(fake.sent).hasSize(1).extracting(ResultMailSender.Message::id).containsExactly(selected);
    }
}
