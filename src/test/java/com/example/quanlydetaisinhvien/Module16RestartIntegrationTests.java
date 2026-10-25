package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.mail.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;

/** Real context close/reopen, same H2 file; never claims to validate MySQL durability. */
class Module16RestartIntegrationTests {
    @TempDir Path directory;
    static final List<ResultMailSender.Message> sent = new CopyOnWriteArrayList<>();
    static volatile CountDownLatch polled = new CountDownLatch(1);
    @TestConfiguration(proxyBeanMethods=false) static class FakeTransport {
        @Bean @Primary ResultMailSender restartFakeSender() {
            return message -> { sent.add(message); polled.countDown(); };
        }
    }
    ConfigurableApplicationContext start(boolean enabled, String ddl) {
        return new SpringApplicationBuilder(QuanlydetaisinhvienApplication.class, FakeTransport.class)
                .web(WebApplicationType.SERVLET).run(
                    "--spring.profiles.active=",
                    "--server.address=127.0.0.1", "--server.port=0",
                    "--spring.datasource.url=jdbc:h2:file:"+directory.resolve("outbox").toString().replace('\\','/')+";MODE=MySQL",
                    "--spring.datasource.driver-class-name=org.h2.Driver",
                    "--spring.datasource.username=sa", "--spring.datasource.password=",
                    "--spring.jpa.hibernate.ddl-auto="+ddl, "--spring.sql.init.mode=never",
                    "--app.ketqua.email.enabled="+enabled,
                    "--app.ketqua.email.scheduling-enabled="+enabled,
                    "--app.ketqua.email.poll-ms=100", "--app.ketqua.email.test-mode=false",
                    "--app.mailjet.api-key=", "--app.mailjet.secret-key=",
                    "--spring.main.banner-mode=off", "--logging.level.root=WARN");
    }
    Emailketqua row(Long result, long student, TrangthaiEmailketqua status) {
        var row=new Emailketqua(); row.setKetquaid(result); row.setSinhvienid(student);
        row.setEmail("student"+student+"@example.invalid"); row.setTieude("Restart fixture");
        row.setNoidung("Synthetic H2 snapshot"); row.setNgaytao(LocalDateTime.now()); row.setTrangthai(status);
        if(status!=TrangthaiEmailketqua.PENDING) { row.setSolanthu(1); row.setLanthu(LocalDateTime.now().minusHours(1)); }
        return row;
    }
    @Test void committedOutboxSurvivesContextRestartAndSchedulerResumesWithoutDuplicateDelivery() throws Exception {
        sent.clear(); Long pending, alreadySent, ambiguous, resultId;
        try(var first=start(false,"create")) {
            assertThat(first.getBeanNamesForType(org.springframework.scheduling.TaskScheduler.class)).isEmpty();
            assertThat(org.springframework.util.ClassUtils.isPresent(
                    "org.springframework.mail.javamail.JavaMailSender",getClass().getClassLoader())).isFalse();
            var result=new Ketqua(); result.setDangkydetaiid(10L); result.setDacongbo(true);
            result.setNgaycongbo(LocalDateTime.now()); result=first.getBean(KetquaRepository.class).saveAndFlush(result);
            resultId=result.getId(); var emails=first.getBean(EmailketquaRepository.class);
            pending=emails.saveAndFlush(row(resultId,1,TrangthaiEmailketqua.PENDING)).getId();
            alreadySent=emails.saveAndFlush(row(resultId,2,TrangthaiEmailketqua.SENT)).getId();
            ambiguous=emails.saveAndFlush(row(resultId,3,TrangthaiEmailketqua.SENDING)).getId();
            assertThat(sent).isEmpty();
        }
        try(var second=start(true,"none")) {
            var emails=second.getBean(EmailketquaRepository.class);
            assertThat(emails.findById(pending).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
            assertThat(emails.findById(alreadySent).orElseThrow().getSolanthu()).isEqualTo(1);
            assertThat(emails.findById(ambiguous).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.UNCERTAIN);
            assertThat(sent).hasSize(1).extracting(ResultMailSender.Message::id).containsExactly(pending);
            polled=new CountDownLatch(1);
            var later=emails.saveAndFlush(row(resultId,4,TrangthaiEmailketqua.PENDING));
            assertThat(polled.await(5,TimeUnit.SECONDS)).isTrue();
            // Sender signals before complete() commits; wait for the worker's committed status.
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(emails.findById(later.getId()).orElseThrow().getTrangthai()!=TrangthaiEmailketqua.SENT
                    && System.nanoTime()<deadline) Thread.sleep(20);
            assertThat(emails.findById(later.getId()).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        }
        try(var third=start(true,"none")) {
            third.getBean(KetquaEmailWorker.class).drain();
            assertThat(sent).hasSize(2).extracting(ResultMailSender.Message::id).doesNotHaveDuplicates();
            assertThat(third.getBean(EmailketquaRepository.class).findById(ambiguous).orElseThrow().getSolanthu()).isEqualTo(1);
        }
    }
}
