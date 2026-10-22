package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.config.NotificationEmailSettings;
import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import com.example.quanlydetaisinhvien.service.mail.ResultMailSender;
import com.example.quanlydetaisinhvien.service.notification.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import org.springframework.web.context.WebApplicationContext;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:notification-email;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.mail.test-connection=false",
        "app.thongbao.email.outbox-enabled=true","app.thongbao.email.delivery-enabled=true",
        "app.thongbao.email.schedule-enabled=false","app.thongbao.email.mode=ALLOWLIST",
        "app.thongbao.email.allowlist=one@example.invalid,two@example.invalid","app.thongbao.email.min-interval-ms=0",
        "app.thongbao.email.base-url=https://portal.example.invalid/hcmute",
        "app.thongbao.email.max-recipients-per-notice=1000",
        "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"
})
@Import({NotificationEmailTests.MailConfig.class,ThongbaoTestSupport.TestViews.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationEmailTests {
    @Autowired ThongbaoService notices;
    @Autowired ThongbaoRepository noticeRows;
    @Autowired EmailthongbaoRepository emails;
    @Autowired NotificationEmailOutbox outbox;
    @Autowired NotificationEmailWorker worker;
    @Autowired NotificationEmailQueue queue;
    @Autowired NotificationEmailSchema schema;
    @Autowired NotificationEmailSettings settings;
    @Autowired NotificationEmailSafety safety;
    @Autowired NotificationEmailMonitorService monitor;
    @Autowired NotificationEmailMonitorRepository monitorRows;
    @Autowired NotificationRecipientRepository recipients;
    @Autowired FakeSender fake;
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired WebApplicationContext context;
    TransactionTemplate tx;
    MockMvc mvc;
    Long chiefId, firstStudent, firstAccount;

    @TestConfiguration(proxyBeanMethods=false)
    static class MailConfig {
        @Bean @Primary FakeSender notificationFake(JdbcTemplate jdbc) { return new FakeSender(jdbc); }
        @Bean @Primary ResultMailSender resultMailMustNotRun() { return message -> { throw new AssertionError("Module 16 must not be called"); }; }
    }
    static class FakeSender implements NotificationMailSender {
        enum Outcome { SUCCESS, FAIL, UNCERTAIN }
        final List<Message> sent=new CopyOnWriteArrayList<>();
        final JdbcTemplate jdbc;
        volatile Outcome outcome=Outcome.SUCCESS;
        volatile boolean observedCommitted=true;
        volatile CountDownLatch inFlight;
        FakeSender(JdbcTemplate jdbc) { this.jdbc=jdbc; }
        @Override public void send(Message message) {
            observedCommitted &= !TransactionSynchronizationManager.isActualTransactionActive()
                    && jdbc.queryForObject("select count(*) from thongbao where Id=?",Long.class,message.noticeId())==1;
            sent.add(message);
            if (inFlight!=null) {
                inFlight.countDown();
                try { if (!inFlight.await(8,TimeUnit.SECONDS)) throw new AssertionError("Concurrent sends did not leave DB locks"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new AssertionError(ex); }
            }
            if (outcome==Outcome.FAIL) throw new DefinitelyNotSentException("fake pre-accept failure");
            if (outcome==Outcome.UNCERTAIN) throw new IllegalStateException("fake lost SMTP ACK PRIVATE_SMTP_SECRET");
        }
        void reset() { sent.clear(); outcome=Outcome.SUCCESS; observedCommitted=true; inFlight=null; }
    }

    @BeforeAll void reviewedMigrationRunsOnlyInH2() throws Exception {
        try(var c=source.getConnection()) { assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:notification-email"); }
        tx=new TransactionTemplate(transactions);
        // Exercise the actual pending migration syntax after removing only MySQL storage/collation clauses.
        jdbc.execute("drop table emailthongbao");
        String ddl=Files.readString(Path.of("database/migrations/pending/V003__emailthongbao.sql"))
                .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", "");
        new ResourceDatabasePopulator(new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8))).execute(source);
    }
    @BeforeEach void fixture() {
        fake.reset();
        tx.executeWithoutResult(status -> {
            for (String type:List.of("Emailthongbao","Thongbao","Emailketqua","Ketqua","Sinhvien","Taikhoan","Bomon"))
                em.createQuery("delete from "+type).executeUpdate();
            var dept=new Bomon(); dept.setMabomon("MAIL"); dept.setTenbomon("Bộ môn kiểm thử"); em.persist(dept);
            chiefId=account("mail-chief",Vaitro.TRUONGKHOA,true,"chief@example.invalid").getId();
            var first=student("mail-one",Vaitro.SINHVIEN,true,"one@example.invalid",dept.getId());
            firstStudent=first.getId(); firstAccount=first.getTaikhoanid();
            student("mail-two",Vaitro.SINHVIEN,true,"two@example.invalid",dept.getId());
            student("mail-null",Vaitro.SINHVIEN,true,null,dept.getId());
            student("mail-invalid",Vaitro.SINHVIEN,true,"invalid email",dept.getId());
            student("mail-disabled",Vaitro.SINHVIEN,false,"disabled@example.invalid",dept.getId());
            student("mail-wrong-role",Vaitro.GIANGVIEN,true,"teacher@example.invalid",dept.getId());
            account("mail-no-profile",Vaitro.SINHVIEN,true,"no-profile@example.invalid");
            var result=new Ketqua(); result.setDangkydetaiid(700L); result.setDacongbo(true); em.persist(result);
            var resultEmail=new Emailketqua(); resultEmail.setKetquaid(result.getId()); resultEmail.setSinhvienid(firstStudent);
            resultEmail.setEmail("pending-result@example.invalid"); resultEmail.setTieude("Existing pending result");
            resultEmail.setNoidung("Must remain unchanged"); resultEmail.setTrangthai(TrangthaiEmailketqua.PENDING);
            resultEmail.setNgaytao(LocalDateTime.of(2026,10,16,0,0)); em.persist(resultEmail); em.flush();
        });
        auth("mail-chief","TRUONGKHOA");
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    Taikhoan account(String name,Vaitro role,boolean active,String email) {
        var a=new Taikhoan(); a.setTendangnhap(name); a.setMatkhau("unused-H2-password"); a.setHoten("Nguyễn Thị "+name);
        a.setEmail(email); a.setVaitro(role); a.setKichhoat(active); em.persist(a); return a;
    }
    Sinhvien student(String name,Vaitro role,boolean active,String email,Long department) {
        var account=account(name,role,active,email);
        var student=new Sinhvien(); student.setTaikhoanid(account.getId()); student.setBomonid(department);
        student.setMasinhvien(name); em.persist(student); return student;
    }
    void auth(String name,String role) { SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            name,"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)))); }
    ThongbaoFormDTO form(String title) { var f=new ThongbaoFormDTO(); f.setTieude(title); f.setNoidung("Thông tin tiếng Việt\n<script>alert('x')</script>"); return f; }
    ThongbaoViewDTO create() { return notices.create(form("Lịch báo cáo học kỳ")); }
    List<Emailthongbao> rows() { return emails.findAll(org.springframework.data.domain.Sort.by("id")); }
    NotificationEmailWorker newWorker() { return new NotificationEmailWorker(queue,fake,safety,schema,settings); }

    @Test void successfulHttpCreateEnqueuesOnlyActiveStudentProfilesWithoutSendingInRequest() throws Exception {
        mvc.perform(post("/thongbao").with(user("mail-chief").roles("TRUONGKHOA")).with(csrf())
                        .param("tieude","Lịch báo cáo học kỳ").param("noidung","Nội dung tiếng Việt"))
                .andExpect(status().is3xxRedirection());
        assertThat(noticeRows.count()).isEqualTo(1);
        assertThat(rows()).hasSize(4);
        assertThat(rows()).filteredOn(e->e.getTrangthai()==TrangthaiEmailthongbao.PENDING).hasSize(2);
        assertThat(rows()).filteredOn(e->e.getTrangthai()==TrangthaiEmailthongbao.MISSING_EMAIL).hasSize(2);
        assertThat(rows()).extracting(Emailthongbao::getNguoinhan).noneMatch(s->s.contains("disabled")||s.contains("wrong-role")||s.contains("no-profile"));
        assertThat(fake.sent).isEmpty();
    }

    @Test void repeatedEnqueueIsIdempotentAndUniqueConstraintRejectsDuplicateRecipients() {
        var notice=create();
        tx.executeWithoutResult(s->outbox.enqueue(noticeRows.findById(notice.id()).orElseThrow(),"Trưởng khoa"));
        assertThat(rows()).hasSize(4);
        assertThatThrownBy(()->tx.executeWithoutResult(s->{
            var existing=rows().getFirst();
            var duplicate=new Emailthongbao(); duplicate.setThongbaoid(existing.getThongbaoid());
            duplicate.setThongbaoSnapshotId(existing.getThongbaoSnapshotId()); duplicate.setSinhvienid(existing.getSinhvienid());
            duplicate.setNguoinhan("Duplicate"); duplicate.setTieude("Duplicate"); duplicate.setNoidung("Duplicate");
            duplicate.setTrangthai(TrangthaiEmailthongbao.PENDING); duplicate.setNgaytao(existing.getNgaytao()); emails.saveAndFlush(duplicate);
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(rows()).hasSize(4);
    }

    @Test void editsAndAccountChangesNeverRewriteEmailSnapshotsOrEnqueueAgain() {
        var notice=create(); var before=snapshot("emailthongbao");
        notices.update(notice.id(),form("Tiêu đề đã sửa"));
        tx.executeWithoutResult(s->{ var account=em.find(Taikhoan.class,firstAccount); account.setEmail("changed@example.invalid"); });
        assertThat(snapshot("emailthongbao")).isEqualTo(before);
        assertThat(rows().getFirst().getNoidung()).contains("Thông tin tiếng Việt", "<script>","https://portal.example.invalid/hcmute/thongbao/"+notice.id(),"Người đăng:");
        assertThat(rows().getFirst().getEmail()).isEqualTo("one@example.invalid");
        worker.drain(); assertThat(fake.sent).extracting(NotificationMailSender.Message::to).containsExactly("one@example.invalid","two@example.invalid");
    }

    @Test void deletionPreservesJournalAndCancelsPendingMailWithoutSending() {
        var notice=create(); notices.deleteById(notice.id());
        assertThat(noticeRows.findById(notice.id())).isEmpty();
        assertThat(rows()).hasSize(4).allMatch(e->e.getThongbaoid()==null && e.getThongbaoSnapshotId().equals(notice.id()));
        worker.drain(); assertThat(fake.sent).isEmpty();
        assertThat(rows()).filteredOn(e->e.getTrangthai()==TrangthaiEmailthongbao.FAILED).hasSize(2);
    }

    @Test void listSearchDetailAndReloadDoNotCreateOrSendMoreMail() throws Exception {
        var notice=create(); var before=snapshot("emailthongbao");
        for(String path:List.of("/thongbao","/thongbao?keyword=Lịch","/thongbao/"+notice.id(),"/thongbao/"+notice.id()))
            mvc.perform(get(path).with(user("mail-chief").roles("TRUONGKHOA"))).andExpect(status().isOk());
        assertThat(snapshot("emailthongbao")).isEqualTo(before); assertThat(fake.sent).isEmpty();
    }

    @Test void rollbackAndInvalidCreateLeaveNoNoticeNoOutboxAndNoDelivery() {
        tx.executeWithoutResult(status->{ create(); assertThat(rows()).hasSize(4); worker.drain(); status.setRollbackOnly(); });
        assertThat(noticeRows.count()).isZero(); assertThat(emails.count()).isZero(); assertThat(fake.sent).isEmpty();
        assertThatThrownBy(()->notices.create(form(" "))).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThat(noticeRows.count()).isZero(); assertThat(emails.count()).isZero();
    }

    @Test void outboxFailureRollsBackTheNoticeRatherThanLeavingAPartialRecipientSet() {
        jdbc.execute("alter table emailthongbao add constraint Reject_snapshot check (Tieude not like '%REJECT_OUTBOX%')");
        try {
            assertThatThrownBy(()->notices.create(form("REJECT_OUTBOX"))).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(noticeRows.count()).isZero(); assertThat(emails.count()).isZero(); assertThat(fake.sent).isEmpty();
        } finally { jdbc.execute("alter table emailthongbao drop constraint Reject_snapshot"); }
    }

    @Test void successfulWorkerSeesCommittedNoticeStoresSentAndNeverResendsAcrossRestarts() {
        create(); worker.drain(); newWorker().drain(); worker.drain();
        assertThat(fake.sent).hasSize(2); assertThat(fake.observedCommitted).isTrue();
        assertThat(rows()).filteredOn(e->e.getTrangthai()==TrangthaiEmailthongbao.SENT)
                .hasSize(2).allMatch(e->e.getNgaygui()!=null && e.getSolanthu()==1);
    }

    @Test void deliveryScopeAndBatchLimitAreAppliedInDatabaseBeforeSelectingPendingIds() {
        var notice=create();
        var restricted=new NotificationEmailSettings(true,true,false,NotificationEmailSettings.Mode.ALLOWLIST,
                Set.of("two@example.invalid"),0,notice.id(),0L,1,0,1000,900,"",10000);
        var restrictedSafety=new NotificationEmailSafety(restricted,new org.springframework.mock.env.MockEnvironment());
        var restrictedQueue=new NotificationEmailQueue(emails,restricted,restrictedSafety);
        var allowedIds=tx.execute(s->restrictedQueue.pending());
        assertThat(allowedIds).hasSize(1);
        assertThat(emails.findById(allowedIds.getFirst()).orElseThrow().getEmail()).isEqualTo("two@example.invalid");
        var first=rows().getFirst();
        NotificationMailSender.Message blocked=tx.execute(s->restrictedQueue.claim(first.getId()));
        assertThat(blocked).isNull();
        assertThat(emails.findById(first.getId()).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailthongbao.PENDING);
        var selected=new NotificationEmailSettings(true,true,false,NotificationEmailSettings.Mode.ALLOWLIST,
                Set.of("one@example.invalid","two@example.invalid"),firstStudent,notice.id(),0L,1,0,1000,900,"",10000);
        var selectedQueue=new NotificationEmailQueue(emails,selected,new NotificationEmailSafety(selected,new org.springframework.mock.env.MockEnvironment()));
        List<Long> selectedIds=tx.execute(s->selectedQueue.pending());
        assertThat(selectedIds).containsExactly(first.getId());
    }

    @Test void invalidSnapshotAddressIsStoppedBeforeSmtpEvenIfItWasPending() {
        create(); var id=queue.pending().getFirst();
        tx.executeWithoutResult(s->{ var row=emails.findById(id).orElseThrow(); row.setEmail("bad email"); emails.save(row); });
        assertThat(queue.claim(id)).isNull();
        var email=emails.findById(id).orElseThrow();
        assertThat(email.getTrangthai()).isEqualTo(TrangthaiEmailthongbao.MISSING_EMAIL);
        assertThat(email.getSolanthu()).isZero(); assertThat(fake.sent).isEmpty();
    }

    @Test void noStudentProfilesProducesANoticeWithNoEmailRows() {
        tx.executeWithoutResult(s->{ em.createQuery("delete from Emailketqua").executeUpdate(); em.createQuery("delete from Sinhvien").executeUpdate(); });
        create(); assertThat(noticeRows.count()).isEqualTo(1); assertThat(emails.count()).isZero();
    }

    @ParameterizedTest @ValueSource(strings={"FAIL","UNCERTAIN"})
    void smtpFailuresAreTerminalForAutomaticDelivery(String outcome) {
        create(); fake.outcome=FakeSender.Outcome.valueOf(outcome); worker.drain(); newWorker().drain();
        var state=outcome.equals("FAIL")?TrangthaiEmailthongbao.FAILED:TrangthaiEmailthongbao.UNCERTAIN;
        assertThat(rows()).filteredOn(e->e.getTrangthai()==state).hasSize(2)
                .allMatch(e->e.getSolanthu()==1 && e.getNgaygui()==null && !e.getLoi().contains("PRIVATE_SMTP_SECRET"));
        assertThat(fake.sent).hasSize(2);
    }

    @Test void expiredSendingLeaseBecomesUncertainAndIsNeverAutomaticallyRetried() {
        create(); var id=queue.pending().getFirst(); var claim=queue.claim(id);
        tx.executeWithoutResult(s->{ var email=emails.findById(id).orElseThrow(); email.setLanthu(LocalDateTime.of(2020,1,1,0,0)); emails.save(email); });
        worker.drain();
        assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailthongbao.UNCERTAIN);
        queue.complete(claim,TrangthaiEmailthongbao.SENT,null); // stale worker cannot overwrite recovered journal.
        assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailthongbao.UNCERTAIN);
        assertThat(fake.sent).hasSize(1).noneMatch(m->m.id().equals(id));
    }

    @Test void twoConcurrentWorkersClaimEachRecipientOnlyOnceAndReleaseLocksBeforeSmtp() throws Exception {
        create(); fake.inFlight=new CountDownLatch(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            var one=pool.submit(()->{ try { start.await(); } catch(InterruptedException ex){ throw new RuntimeException(ex); } newWorker().drain(); });
            var two=pool.submit(()->{ try { start.await(); } catch(InterruptedException ex){ throw new RuntimeException(ex); } newWorker().drain(); });
            start.countDown(); one.get(15,TimeUnit.SECONDS); two.get(15,TimeUnit.SECONDS);
        }
        assertThat(fake.sent).hasSize(2).extracting(NotificationMailSender.Message::id).doesNotHaveDuplicates();
        assertThat(rows()).filteredOn(e->e.getTrangthai()==TrangthaiEmailthongbao.SENT).hasSize(2);
    }

    @Test void sentJournalIsKeptAfterNoticeDeletionWithoutSendingAgain() {
        var notice=create(); worker.drain(); notices.deleteById(notice.id()); newWorker().drain();
        assertThat(fake.sent).hasSize(2);
        assertThat(rows()).filteredOn(e->e.getTrangthai()==TrangthaiEmailthongbao.SENT).hasSize(2).allMatch(e->e.getThongbaoid()==null);
    }

    @Test void resultOutboxAndAllOtherBusinessRowsAreUntouchedByNoticeEmailProcessing() {
        var resultBefore=snapshot("emailketqua"); var results=snapshot("ketqua"); var accounts=snapshot("taikhoan");
        create(); var noticesBefore=snapshot("thongbao"); worker.drain();
        assertThat(snapshot("emailketqua")).isEqualTo(resultBefore); assertThat(snapshot("ketqua")).isEqualTo(results);
        assertThat(snapshot("taikhoan")).isEqualTo(accounts); assertThat(snapshot("thongbao")).isEqualTo(noticesBefore);
    }

    @ParameterizedTest @ValueSource(strings={"SINHVIEN","GIANGVIEN","TRUONGBOMON","UNKNOWN"})
    void otherRolesCannotCreateOrTriggerMailOrReadPrivateRecipientData(String role) throws Exception {
        auth("mail-one",role);
        assertThatThrownBy(this::create).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->monitor.read(new NotificationEmailFilterDTO())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->monitorRows.items(new NotificationEmailFilterDTO())).isInstanceOf(AccessDeniedException.class);
        mvc.perform(post("/thongbao").with(user("mail-one").roles(role)).with(csrf())
                .param("tieude","Mới").param("noidung","Nội dung")).andExpect(status().isForbidden());
        mvc.perform(get("/morong/email-thongbao").with(user("mail-one").roles(role))).andExpect(status().isForbidden());
        mvc.perform(post("/morong/email-thongbao/dispatch").with(user("mail-one").roles(role)).with(csrf())).andExpect(status().isForbidden());
        assertThat(emails.count()).isZero(); assertThat(fake.sent).isEmpty();
    }

    @Test void csrfAndActiveChiefChecksRemainInForce() throws Exception {
        mvc.perform(post("/thongbao").with(user("mail-chief").roles("TRUONGKHOA"))
                .param("tieude","Mới").param("noidung","Nội dung")).andExpect(status().isForbidden());
        tx.executeWithoutResult(s->em.find(Taikhoan.class,chiefId).setKichhoat(false));
        assertThatThrownBy(this::create).isInstanceOf(AccessDeniedException.class);
        mvc.perform(get("/morong/email-thongbao").with(user("mail-chief").roles("TRUONGKHOA"))).andExpect(status().isForbidden());
    }

    @Test void privateMonitoringIsPaginatedFilteredReadOnlyAndNeverReturnsABogusTemplate() throws Exception {
        var notice=create(); var before=snapshot("emailthongbao");
        mvc.perform(get("/morong/email-thongbao").with(user("mail-chief").roles("TRUONGKHOA"))
                        .param("thongbaoid",notice.id().toString()).param("size","1"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.counts.total").value(4)).andExpect(jsonPath("$.counts.pending").value(2))
                .andExpect(jsonPath("$.counts.missingEmail").value(2)).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(4));
        assertThat(snapshot("emailthongbao")).isEqualTo(before);
        mvc.perform(get("/morong/email-thongbao").with(anonymous())).andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest @ValueSource(strings={"thongbaoid=-1","trangthai=BAD","size=101","page=-1","username=mail-chief"})
    void malformedMonitoringFiltersAre400(String parameter) throws Exception {
        var pair=parameter.split("=",2);
        mvc.perform(get("/morong/email-thongbao").with(user("mail-chief").roles("TRUONGKHOA")).param(pair[0],pair[1]))
                .andExpect(status().isBadRequest());
    }
    // Only this H2 fixture is changed; preserve the existing Module 16 pending recipient.
    void recipientFixture(int count) {
        tx.executeWithoutResult(s->{
            em.createQuery("delete from Sinhvien where id<>:id").setParameter("id",firstStudent).executeUpdate();
            Long department=em.find(Sinhvien.class,firstStudent).getBomonid();
            for(int i=1;i<count;i++) {
                student("batch-"+i,Vaitro.SINHVIEN,true,"batch-"+i+"@example.invalid",department);
                if(i%500==0) { em.flush(); em.clear(); }
            }
        });
    }

    @ParameterizedTest @ValueSource(ints={500,501,1000})
    void everyRecipientAcrossPageBoundariesIsEnqueuedExactlyOnceWithoutSmtp(int count) {
        recipientFixture(count); var resultBefore=snapshot("emailketqua");
        var notice=create();
        assertThat(emails.count()).isEqualTo(count);
        assertThat(jdbc.queryForObject("select count(distinct Sinhvienid) from emailthongbao where Thongbaosnapshotid=?",
                Long.class,notice.id())).isEqualTo(count);
        assertThat(rows()).hasSize(count).allMatch(e->e.getTrangthai()==TrangthaiEmailthongbao.PENDING);
        tx.executeWithoutResult(s->outbox.enqueue(noticeRows.findById(notice.id()).orElseThrow(),"Trưởng khoa"));
        assertThat(emails.count()).isEqualTo(count);
        assertThat(snapshot("emailketqua")).isEqualTo(resultBefore); assertThat(fake.sent).isEmpty();
    }

    @Test void failureInSecondPageRollsBackTheFirstFiveHundredAndTheNotice() {
        recipientFixture(501);
        long last=tx.execute(s->recipients.scope()).lastId();
        jdbc.execute("alter table emailthongbao add constraint Reject_last_recipient check (Sinhvienid<>"+last+")");
        try {
            assertThatThrownBy(this::create).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(emails.count()).isZero(); assertThat(noticeRows.count()).isZero(); assertThat(fake.sent).isEmpty();
        } finally { jdbc.execute("alter table emailthongbao drop constraint Reject_last_recipient"); }
    }

    @Test void tooManyRecipientsReturnsAnErrorWithoutSuccessfulRedirectOrPartialQueue() throws Exception {
        recipientFixture(1001);
        mvc.perform(post("/thongbao").with(user("mail-chief").roles("TRUONGKHOA")).with(csrf())
                        .param("tieude","Vượt giới hạn xử lý").param("noidung","Dữ liệu H2"))
                .andExpect(status().isOk()).andExpect(view().name("thongbao-form"))
                .andExpect(model().attributeHasErrors("thongbaoForm"))
                .andExpect(model().attributeExists("errorMessage"));
        assertThat(noticeRows.count()).isZero(); assertThat(emails.count()).isZero(); assertThat(fake.sent).isEmpty();
    }

    @Test void pagesAreLimitedAtDatabaseAndStopAtTheCapturedRecipientId() {
        recipientFixture(501);
        var scope=tx.execute(s->recipients.scope());
        tx.executeWithoutResult(s->student("after-snapshot",Vaitro.SINHVIEN,true,"after@example.invalid",
                em.find(Sinhvien.class,firstStudent).getBomonid()));
        var first=tx.execute(s->recipients.batchAfter(0,scope.lastId()));
        var second=tx.execute(s->recipients.batchAfter(first.getLast().id(),scope.lastId()));
        assertThat(scope.count()).isEqualTo(501); assertThat(first).hasSize(500); assertThat(second).hasSize(1);
        assertThat(second.getFirst().id()).isEqualTo(scope.lastId());
        var end=tx.execute(s->recipients.batchAfter(second.getLast().id(),scope.lastId()));
        assertThat(end).isEmpty();
    }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void studentDeletionDoesNotLoseHistoryOrAddANewForeignKeyBlock(boolean sent) {
        create(); if(sent) worker.drain();
        long other=jdbc.queryForObject("select s.Id from sinhvien s join taikhoan a on a.Id=s.Taikhoanid where a.Tendangnhap='mail-two'",Long.class);
        var before=snapshot("emailthongbao");
        tx.executeWithoutResult(s->em.remove(em.find(Sinhvien.class,other)));
        assertThat(jdbc.queryForObject("select count(*) from sinhvien where Id=?",Long.class,other)).isZero();
        assertThat(snapshot("emailthongbao")).isEqualTo(before);
        var history=rows().stream().filter(e->e.getSinhvienid()==other).findFirst().orElseThrow();
        assertThat(history.getNguoinhan()).contains("mail-two"); assertThat(history.getEmail()).isEqualTo("two@example.invalid");
        assertThat(history.getTrangthai()).isEqualTo(sent?TrangthaiEmailthongbao.SENT:TrangthaiEmailthongbao.PENDING);
    }

    @Test void demoAndGenericPendingScriptsHaveIdenticalDdlAndNoStudentForeignKey() throws Exception {
        String generic=Files.readString(Path.of("database/migrations/pending/V003__emailthongbao.sql"));
        String demo=Files.readString(Path.of("database/demo/06_emailthongbao_pending.sql"));
        assertThat(demo.substring(demo.indexOf("CREATE TABLE")).replace("quanlydetaisinhvien_demo.",""))
                .isEqualTo(generic.substring(generic.indexOf("CREATE TABLE")));
        assertThat(generic).doesNotContain("Fk_emailthongbao_student","REFERENCES sinhvien");
        assertThat(generic).contains("UNIQUE KEY Uk_emailthongbao_recipient (Thongbaosnapshotid,Sinhvienid)");
    }

    @Test void minNoticeIdFiltersOutExistingOlderNoticesFromPendingQuery() {
        var oldNotice = create();
        var newNotice = create();
        var config = new NotificationEmailSettings(true, true, false, NotificationEmailSettings.Mode.SYSTEM,
                Set.of(), 0, 0, newNotice.id(), 10, 0, 1000, 900, "", 10000);
        var q = new NotificationEmailQueue(emails, config, new NotificationEmailSafety(config, new org.springframework.mock.env.MockEnvironment()));
        List<Long> pendingIds = tx.execute(s -> q.pending());
        List<Emailthongbao> rows = emails.findAllById(pendingIds);
        assertThat(rows).isNotEmpty();
        assertThat(rows).allMatch(e -> e.getThongbaoSnapshotId() >= newNotice.id());
        assertThat(rows).noneMatch(e -> e.getThongbaoSnapshotId() < newNotice.id());
    }

    List<Map<String,Object>> snapshot(String table) { return jdbc.queryForList("select * from "+table+" order by Id"); }
}
