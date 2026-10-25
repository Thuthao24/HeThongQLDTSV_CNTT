package com.example.quanlydetaisinhvien.service.mailjet;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import com.example.quanlydetaisinhvien.service.mail.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:mailjet-workers;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never",
        "app.ketqua.email.scheduling-enabled=false","app.ketqua.email.test-mode=false",})
@Import(MailjetWorkerIntegrationTests.HttpFixture.class)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MailjetWorkerIntegrationTests {
    static final AtomicBoolean resultDelivery=new AtomicBoolean(true);
    @DynamicPropertySource static void flags(DynamicPropertyRegistry registry) {
        registry.add("app.ketqua.email.enabled",resultDelivery::get);
    }
    @TestConfiguration(proxyBeanMethods=false) static class HttpFixture {
        @Bean MockMailjetServer mailjetServer() { return new MockMailjetServer(); }
        @Bean @Primary MailjetClient loopbackClient(MockMailjetServer server) { return server.client(Duration.ofSeconds(2)); }
    }
    @Autowired MockMailjetServer server;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired DataSource dataSource;
    @Autowired KetquaPublicationService publication;
    @Autowired KetquaEmailWorker resultWorker;
    @Autowired KetquaEmailQueue resultQueue;
    @Autowired KetquaEmailOutbox resultOutbox;
    @Autowired KetquaSchema resultSchema;
    @Autowired MailjetResultMailSender resultSender;
    @Autowired KetquaRepository results;
    @Autowired EmailketquaRepository resultEmails;
    TransactionTemplate tx;
    @BeforeAll void prepareH2Constraints() throws Exception {
        try(var connection=dataSource.getConnection()) { assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:mailjet-workers"); }
        new ResourceDatabasePopulator(new ClassPathResource("ketqua-new-tables-h2.sql")).execute(dataSource);
        tx=new TransactionTemplate(transactions);
    }
    @BeforeEach void cleanOnlyH2() {
        server.reset(); resultDelivery.set(true);
        tx.executeWithoutResult(status->{
            for(String entity:List.of("Thongbao","Emailketqua","Phanconghoidong","Ketqua","Diem",
                    "Thanhvienhoidong","Hoidong","Giangvienphanbien","Giangvienhuongdan","Thanhviennhom",
                    "Dangkydetai","Nhomsinhvien","Detai","Dotdangky","Sinhvien","Giangvien","Taikhoan"))
                em.createQuery("delete from "+entity).executeUpdate();
        });
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "chief","unused-test-credential",List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); resultDelivery.set(true); }


    @ParameterizedTest @ValueSource(ints={3,4,5})
    void publishesToEveryGroupMemberSeparatelyAndRepeatCannotSendAgain(int size) {
        Long id=fixture(size,false); publication.publish(id);
        var snapshot=results.findById(id).orElseThrow();
        assertThat(resultEmails.findAll()).hasSize(size).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.SENT && e.getSolanthu()==1);
        assertThat(server.requests).hasSize(size);
        assertThat(server.requests).extracting(r->r.body().path("Messages").get(0).path("To").get(0).path("Email").asString()).doesNotHaveDuplicates();
        assertThat(server.requests).allSatisfy(r->{
            var message=r.body().path("Messages").get(0);
            assertThat(message.path("To").size()).isEqualTo(1);
            assertThat(message.has("Cc")).isFalse(); assertThat(message.has("Bcc")).isFalse();
            assertThat(message.path("TextPart").asString()).contains("8.00","https://hethongqldtsv-cntt.onrender.com/ketqua");
        });
        assertThatThrownBy(()->publication.publish(id)).isInstanceOf(NghiepvuException.class);
        resultWorker.afterStartup(); resultWorker.drain();
        assertThat(server.requests).hasSize(size);
        assertThat(results.findById(id).orElseThrow().getNgaycongbo()).isEqualTo(snapshot.getNgaycongbo());
        assertThat(results.findById(id).orElseThrow().getDiemcuoi()).isEqualByComparingTo("8.00");
    }
    @Test void apiUnavailableDoesNotRollbackPublicationAndUncertainIsNeverRetried() {
        Long id=fixture(3,false); server.status=503; publication.publish(id);
        assertThat(results.findById(id).orElseThrow().getDacongbo()).isTrue();
        assertThat(resultEmails.findAll()).hasSize(3).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.UNCERTAIN && e.getSolanthu()==1 && e.getNgaygui()==null);
        resultWorker.afterStartup(); resultWorker.drain();
        assertThat(server.requests).hasSize(3);
        assertThatThrownBy(()->resultOutbox.retry(resultEmails.findAll().getFirst().getId(),false))
                .isInstanceOf(NghiepvuException.class);
    }
    @ParameterizedTest @ValueSource(ints={401,403,429})
    void explicitApiRejectionNeedsAuthorizedRetryAndDoesNotRepublish(int status) {
        Long id=fixture(3,false); server.status=status; publication.publish(id);
        var publishedAt=results.findById(id).orElseThrow().getNgaycongbo();
        assertThat(resultEmails.findAll()).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.FAILED && e.getSolanthu()==1);
        resultWorker.drain(); assertThat(server.requests).hasSize(3);
        server.status=200; var selected=resultEmails.findAll().getFirst();
        resultOutbox.retry(selected.getId(),false);
        assertThat(server.requests).hasSize(4);
        var sent=resultEmails.findById(selected.getId()).orElseThrow();
        assertThat(sent.getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT); assertThat(sent.getSolanthu()).isEqualTo(2);
        assertThat(results.findById(id).orElseThrow().getNgaycongbo()).isEqualTo(publishedAt);
    }

    @Test void startupCallbackResumesPendingWorkWithoutResendingSent() {
        resultDelivery.set(false); Long id=fixture(3,false); publication.publish(id);
        assertThat(resultEmails.findAll()).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.PENDING && e.getSolanthu()==0);
        assertThat(server.requests).isEmpty();
        resultDelivery.set(true); resultWorker.afterStartup(); resultWorker.afterStartup();
        assertThat(server.requests).hasSize(3);
        assertThat(resultEmails.findAll()).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.SENT && e.getSolanthu()==1);
    }
    @Test void requestTimeoutProducesUncertainInResultOutboxAndNeverResubmits() {
        resultDelivery.set(false); publication.publish(fixture(3,false)); resultDelivery.set(true);
        server.delayMs=1000;
        var shortClient=server.client(Duration.ofMillis(250));
        var result=new KetquaEmailWorker(resultQueue,new MailjetResultMailSender(shortClient,new KetquaEmailSafety(
                new org.springframework.mock.env.MockEnvironment().withProperty("app.ketqua.email.enabled","true"))),resultSchema,true);
        Long selected=resultEmails.findAll().getFirst().getId(); result.process(selected); result.process(selected);
        assertThat(server.requests).hasSize(1);
        var uncertain=resultEmails.findById(selected).orElseThrow();
        assertThat(uncertain.getTrangthai()).isEqualTo(TrangthaiEmailketqua.UNCERTAIN); assertThat(uncertain.getSolanthu()).isEqualTo(1);

    }

    @Test void http200WithMessageErrorCannotBecomeSentInResultOutbox() {
        server.reply="{\"Messages\":[{\"Status\":\"error\",\"Errors\":[{\"StatusCode\":403,\"ErrorCode\":\"send-0008\",\"ErrorMessage\":\"PRIVATE_PROVIDER_TEXT\"}]}]}";
        publication.publish(fixture(3,false));
        assertThat(resultEmails.findAll()).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.FAILED && e.getNgaygui()==null && !e.getLoi().contains("PRIVATE_PROVIDER_TEXT"));

        assertThat(server.requests).hasSize(3);
    }
    @Test void concurrentResultWorkersUseTheExistingExclusiveClaimWithRealHttpAdapter() throws Exception {
        resultDelivery.set(false); publication.publish(fixture(3,false)); resultDelivery.set(true);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Runnable task=()->{ try { start.await(); } catch(InterruptedException ex) { throw new IllegalStateException(ex); }
                new KetquaEmailWorker(resultQueue,resultSender,resultSchema,true).drain(); };
            var a=executor.submit(task); var b=executor.submit(task); start.countDown();
            a.get(10,TimeUnit.SECONDS); b.get(10,TimeUnit.SECONDS);
        }
        assertThat(server.requests).hasSize(3).extracting(r->r.body().path("Messages").get(0).path("CustomID").asString()).doesNotHaveDuplicates();
        assertThat(resultEmails.findAll()).allMatch(e->e.getSolanthu()==1 && e.getTrangthai()==TrangthaiEmailketqua.SENT);
    }
    @Test void missingRecipientEmailsNeverReachTheHttpTransport() {
        publication.publish(fixture(3,true));
        assertThat(server.requests).isEmpty();
        assertThat(resultEmails.findAll()).hasSize(3).allMatch(e->e.getTrangthai()==TrangthaiEmailketqua.MISSING_EMAIL && e.getSolanthu()==0);

    }


    @Test void maximumTopicLengthCannotRollbackPublicationOrOverflowOutboxSubject() {
        resultDelivery.set(false); Long id=fixture(3,false);
        tx.executeWithoutResult(status->{
            var result=results.findById(id).orElseThrow();
            var registration=em.find(Dangkydetai.class,result.getDangkydetaiid());
            em.find(Detai.class,registration.getDetaiid()).setTendetai("Đ".repeat(255));
        });
        publication.publish(id);
        assertThat(results.findById(id).orElseThrow().getDacongbo()).isTrue();
        assertThat(resultEmails.findAll()).hasSize(3).allSatisfy(row->{
            assertThat(row.getTieude().length()).isLessThanOrEqualTo(200);
            assertThat(row.getNoidung()).contains("Đ".repeat(255));
        });
        resultDelivery.set(true); resultWorker.drain();
        assertThat(server.requests).hasSize(3);
        assertThat(resultEmails.findAll()).allMatch(row->row.getTrangthai()==TrangthaiEmailketqua.SENT);
    }

    // Synthetic fixtures are restricted to H2; publication still executes the complete production validator.
    Long fixture(int size,boolean missingEmail) {
        return tx.execute(status->{
            account("chief",Vaitro.TRUONGKHOA,"chief@example.invalid");
            var students=new ArrayList<Sinhvien>();
            for(int i=0;i<size;i++) {
                var a=account("student-"+i,Vaitro.SINHVIEN,missingEmail?null:"student-"+i+"@example.invalid");
                var s=new Sinhvien(); s.setTaikhoanid(a.getId()); s.setMasinhvien("S-"+a.getId()); s.setBomonid(1L); em.persist(s); students.add(s);
            }
            Long guide=lecturer("guide"), reviewer=lecturer("reviewer");
            var round=new Dotdangky(); round.setTendot("H2 round"); round.setLoaidot(Loaidot.MONHOC); round.setTrangthai(TrangthaiDotdangky.DONG);
            round.setBatdaugiangvien(LocalDateTime.now().minusMonths(3)); round.setKetthucgiangvien(LocalDateTime.now().minusMonths(2));
            round.setBatdausinhvien(LocalDateTime.now().minusMonths(1)); round.setKetthucsinhvien(LocalDateTime.now().minusWeeks(2)); em.persist(round);
            var topic=new Detai(); topic.setDotdangkyid(round.getId()); topic.setBomonid(1L); topic.setGiangviendexuatid(guide); topic.setTendetai("H2 topic"); topic.setTrangthai(TrangthaiDetai.CONGBO); em.persist(topic);
            var supervision=new Giangvienhuongdan(); supervision.setDetaiid(topic.getId()); supervision.setGiangvienid(guide); em.persist(supervision);
            var group=new Nhomsinhvien(); group.setDotdangkyid(round.getId()); group.setNhomtruongid(students.getFirst().getId()); group.setTennhom("H2 group"); em.persist(group);
            for(var s:students) em.persist(new Thanhviennhom(group.getId(),s.getId()));
            var registration=new Dangkydetai(); registration.setNhomid(group.getId()); registration.setDetaiid(topic.getId()); registration.setTrangthai(TrangthaiDangkydetai.DADUYET); registration.setNgaydangky(LocalDateTime.now()); em.persist(registration);
            var reviewerAssignment=new Giangvienphanbien(); reviewerAssignment.setDangkydetaiid(registration.getId()); reviewerAssignment.setGiangvienid(reviewer); em.persist(reviewerAssignment);
            var score=new Diem(); score.setDangkydetaiid(registration.getId()); score.setGiangvienid(reviewer); score.setDiemso(new BigDecimal("8.00")); em.persist(score);
            var assignment=new Phanconghoidong(); assignment.setDangkydetaiid(registration.getId()); assignment.setHinhthuc(HinhthucDanhgia.KHONGHOIDONG); em.persist(assignment);
            var result=new Ketqua(); result.setDangkydetaiid(registration.getId()); result.setDiemcuoi(new BigDecimal("8.00")); result.setDacongbo(false); em.persist(result);
            return result.getId();
        });
    }
    Taikhoan account(String name,Vaitro role,String address) {
        var a=new Taikhoan(); a.setTendangnhap(name); a.setMatkhau("unused-H2-password"); a.setHoten(name); a.setVaitro(role); a.setKichhoat(true); a.setEmail(address); em.persist(a); return a;
    }
    Long lecturer(String name) {
        var a=account(name,Vaitro.GIANGVIEN,name+"@example.invalid");
        var lecturer=new Giangvien(); lecturer.setTaikhoanid(a.getId()); lecturer.setBomonid(1L); lecturer.setMagiangvien("G-"+a.getId()); em.persist(lecturer); return lecturer.getId();
    }
}
