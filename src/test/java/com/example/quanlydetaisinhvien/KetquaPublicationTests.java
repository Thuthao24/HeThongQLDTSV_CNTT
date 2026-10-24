package com.example.quanlydetaisinhvien;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ketqua-publication;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "app.ketqua.email.enabled=true",
        "app.ketqua.email.scheduling-enabled=false"
})
@Import(KetquaPublicationTests.FakeMailConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KetquaPublicationTests {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired KetquaService service;
    @Autowired NghiepvuDetaiService business;
    @Autowired KetquaPublicationService publication;
    @Autowired KetquaRepository results;
    @Autowired PhanconghoidongRepository assignments;
    @Autowired EmailketquaRepository emails;
    @Autowired KetquaEmailOutbox outbox;
    @Autowired KetquaEmailQueue queue;
    @Autowired KetquaEmailWorker worker;
    @Autowired KetquaSchema schema;
    @Autowired FakeMail fake;
    @Autowired org.springframework.context.ApplicationEventPublisher events;
    @Autowired WebApplicationContext webContext;
    TransactionTemplate tx;
    MockMvc mvc;
    Fixture f;
    final AtomicInteger sequence = new AtomicInteger();

    record Fixture(Long result, Long registration, Long topic, Long group, Long round,
            Long studentA, Long studentB, Long accountA, Long accountB,
            Long reviewerA, Long reviewerB, Long supervisor) {}

    @BeforeAll
    void constraintsOnH2Only() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:ketqua-publication");
        }
        new ResourceDatabasePopulator(new ClassPathResource("ketqua-new-tables-h2.sql")).execute(dataSource);
        tx = new TransactionTemplate(transactions);
    }

    @BeforeEach
    void setup() {
        authenticate("chief", "TRUONGKHOA");
        fake.reset();
        tx.executeWithoutResult(status -> {
            for (String entity : List.of("Emailketqua", "Phanconghoidong", "Ketqua", "Diem",
                    "Thanhvienhoidong", "Hoidong", "Giangvienphanbien", "Giangvienhuongdan",
                    "Thanhviennhom", "Dangkydetai", "Nhomsinhvien", "Detai", "Dotdangky",
                    "Sinhvien", "Giangvien", "Taikhoan")) em.createQuery("delete from " + entity).executeUpdate();
            f = fixture();
        });
        mvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void publishesAndSendsEachSnapshotOnlyAfterCommit() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        service.congboKetqua(f.result());
        var result = results.findById(f.result()).orElseThrow();
        assertThat(result.getDacongbo()).isTrue();
        assertThat(result.getNgaycongbo()).isBetween(before, LocalDateTime.now().plusSeconds(1));
        assertThat(result.getDiemcuoi()).isEqualByComparingTo("8.00");
        assertThat(fake.attempts).hasSize(2);
        assertThat(fake.observedCommitted).isTrue();
        assertThat(emailRows()).allMatch(row -> row.getTrangthai() == TrangthaiEmailketqua.SENT
                && row.getNgaygui() != null && row.getSolanthu() == 1);
        assertThat(fake.attempts).allMatch(message -> message.body().contains("8.00")
                && !message.body().contains("password"));
    }

    @Test
    void repeatPreservesTimestampAndNeverEnqueuesOrSendsAgain() {
        service.congboKetqua(f.result());
        var publishedAt = results.findById(f.result()).orElseThrow().getNgaycongbo();
        var before = publicationState();
        rejected("đã được công bố");
        worker.drain();
        assertThat(publicationState()).isEqualTo(before);
        assertThat(results.findById(f.result()).orElseThrow().getNgaycongbo()).isEqualTo(publishedAt);
        assertThat(emailRows()).hasSize(2);
        assertThat(fake.attempts).hasSize(2);
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.KHONGHOIDONG, null))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("đã được công bố");
        assertThat(publicationState()).isEqualTo(before);
    }

    @Test
    void duplicatePublicationEventsDoNotCreateOrSendMoreMail() {
        service.congboKetqua(f.result());
        var before=publicationState();
        tx.executeWithoutResult(s->{
            events.publishEvent(new KetquaPublishedEvent(f.result()));
            events.publishEvent(new KetquaPublishedEvent(f.result()));
        });
        assertThat(publicationState()).isEqualTo(before);
        var publishedAt=results.findById(f.result()).orElseThrow().getNgaycongbo();
        assertThat(emailRows()).hasSize(2).allMatch(e->e.getNgaytao().equals(publishedAt));
        assertThat(fake.attempts).extracting(ResultMailSender.Message::to)
                .containsExactlyInAnyOrder("student-a@example.test", "student-b@example.test");
    }

    @Test
    void corruptPendingRecipientIsMissingEmailAndExplicitRetryUsesTheCurrentAccount() {
        Long id=queuedWithoutEvent();
        mutate(Emailketqua.class,id,e->e.setEmail("invalid email"));
        worker.drain();
        var missing=emails.findById(id).orElseThrow();
        assertThat(missing.getTrangthai()).isEqualTo(TrangthaiEmailketqua.MISSING_EMAIL);
        assertThat(missing.getSolanthu()).isZero(); assertThat(fake.attempts).isEmpty();
        outbox.retry(id,false);
        var delivered=emails.findById(id).orElseThrow();
        assertThat(delivered.getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        assertThat(delivered.getNoidung()).isEqualTo(missing.getNoidung());
        assertThat(delivered.getNgaytao()).isEqualTo(missing.getNgaytao());
        assertThat(fake.attempts).hasSize(1);
    }

    @Test
    void databaseFailureAfterSendingDoesNotAutomaticallyResendAcceptedResultMail() {
        Long id=queuedWithoutEvent();
        jdbc.execute("alter table emailketqua add constraint Reject_sent_write check (Trangthai <> 'SENT')");
        try {
            worker.drain();
            assertThat(fake.attempts).hasSize(1);
            assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENDING);
        } finally { jdbc.execute("alter table emailketqua drop constraint Reject_sent_write"); }
        mutate(Emailketqua.class,id,e->e.setLanthu(LocalDateTime.now().minusHours(1)));
        worker.drain(); worker.afterStartup();
        assertThat(fake.attempts).hasSize(1);
        assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.UNCERTAIN);
    }

    @Test
    void rollbackLeavesNoPublishedResultNoOutboxAndNoMail() {
        tx.executeWithoutResult(status -> {
            service.congboKetqua(f.result());
            assertThat(fake.attempts).isEmpty();
            status.setRollbackOnly();
        });
        assertUnpublished();
    }

    @Test
    void nonexistentResultFailsWithoutMail() {
        assertThatThrownBy(() -> service.congboKetqua(Long.MAX_VALUE))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("Không tìm thấy");
        assertUnpublished();
    }

    @ParameterizedTest
    @EnumSource(value = Loaidot.class, names = {"MONHOC", "NCKH", "TLCN"})
    void nonCouncilRoundsUseTheirActualReviewerList(Loaidot type) {
        mutate(Dotdangky.class, f.round(), round -> round.setLoaidot(type));
        service.phanCongHoiDong(f.result(), HinhthucDanhgia.KHONGHOIDONG, null);
        service.congboKetqua(f.result());
        assertThat(fake.attempts).hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(value = Loaidot.class, names = {"MONHOC", "NCKH"})
    void singleAssignedReviewerIsEnoughWhenNoCouncilRequired(Loaidot type) {
        mutate(Dotdangky.class, f.round(), round -> round.setLoaidot(type));
        tx.executeWithoutResult(status -> {
            em.createQuery("delete from Giangvienphanbien r where r.dangkydetaiid = :id and r.giangvienid = :gv")
                    .setParameter("id", f.registration()).setParameter("gv", f.reviewerB()).executeUpdate();
            em.createQuery("delete from Diem d where d.dangkydetaiid = :id and d.giangvienid = :gv")
                    .setParameter("id", f.registration()).setParameter("gv", f.reviewerB()).executeUpdate();
            em.find(Ketqua.class, f.result()).setDiemcuoi(new BigDecimal("7.99"));
        });
        service.congboKetqua(f.result());
        assertThat(results.findById(f.result()).orElseThrow().getDacongbo()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = Loaidot.class, names = {"TLCN", "KLTN"})
    void assignedCouncilNeedsAllMembersAndCountsSharedReviewerOnce(Loaidot type) {
        mutate(Dotdangky.class, f.round(), round -> round.setLoaidot(type));
        Long council = council(3, f.round());
        service.phanCongHoiDong(f.result(), HinhthucDanhgia.COHOIDONG, council);
        rejected("Thiếu điểm");
        addCouncilScores(council);
        service.congboKetqua(f.result());
        assertThat(results.findById(f.result()).orElseThrow().getDiemcuoi()).isEqualByComparingTo("8.00");
        assertThat(fake.attempts).hasSize(2);
    }

    @Test
    void kltnRejectsNoCouncilAndUnresolvedAssignment() {
        mutate(Dotdangky.class, f.round(), round -> round.setLoaidot(Loaidot.KLTN));
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.KHONGHOIDONG, null))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("KLTN");
        tx.executeWithoutResult(status -> em.remove(em.find(Phanconghoidong.class, f.registration())));
        rejected("Chưa xác nhận");
        assertUnpublished();
    }

    @ParameterizedTest
    @EnumSource(value = Loaidot.class, names = {"TLCN", "KLTN"})
    void thesisRoundsCannotSkipReviewerAssignments(Loaidot type) {
        mutate(Dotdangky.class, f.round(), round -> round.setLoaidot(type));
        tx.executeWithoutResult(status -> em.createQuery("delete from Giangvienphanbien r where r.dangkydetaiid = :id")
                .setParameter("id", f.registration()).executeUpdate());
        rejected("Chưa phân công giảng viên phản biện");
        assertUnpublished();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 6})
    void councilsOutsideThreeToFiveMembersAreRejected(int size) {
        Long council = council(size, f.round());
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.COHOIDONG, council))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("3–5");
        assertUnpublished();
    }

    @Test
    void councilMustExistAndBelongToSameRound() {
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.COHOIDONG, Long.MAX_VALUE))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("không tồn tại");
        Long otherRound = tx.execute(status -> round(Loaidot.KLTN).getId());
        Long otherCouncil = council(3, otherRound);
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.COHOIDONG, otherCouncil))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("không thuộc đợt");
        assertUnpublished();
    }

    @Test
    void assignmentRequiresExplicitConsistentChoice() {
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.COHOIDONG, null))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("chọn hội đồng");
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.KHONGHOIDONG, 1L))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("không được kèm");
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), null, null))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("không hợp lệ");
    }

    @Test
    void missingReviewerScoreNamesTheMissingLecturer() {
        tx.executeWithoutResult(status -> em.createQuery("delete from Diem d where d.dangkydetaiid = :id and d.giangvienid = :gv")
                .setParameter("id", f.registration()).setParameter("gv", f.reviewerB()).executeUpdate());
        rejected("Thiếu điểm của giảng viên: " + f.reviewerB());
        assertUnpublished();
    }

    @Test
    void supervisorCannotBeAnAssignedGrader() {
        tx.executeWithoutResult(status -> reviewer(f.registration(), f.supervisor()));
        rejected("đang hướng dẫn");
        assertUnpublished();
    }

    @Test
    void unassignedScoreCannotBeSilentlyIgnored() {
        tx.executeWithoutResult(status -> score(f.registration(), f.supervisor(), "8.00"));
        rejected("không thuộc phân công");
        assertUnpublished();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "10.01"})
    void componentScoresMustBeWithinZeroToTen(String value) {
        tx.executeWithoutResult(status -> em.createQuery("update Diem d set d.diemso = :score where d.dangkydetaiid = :id")
                .setParameter("score", new BigDecimal(value)).setParameter("id", f.registration()).executeUpdate());
        rejected("phải trong khoảng 0–10");
        assertUnpublished();
    }

    @ParameterizedTest
    @NullSource @ValueSource(strings = {"-0.01", "10.01", "7.99"})
    void finalScoreMustBePresentInRangeAndMatchHalfUpAverage(String value) {
        mutate(Ketqua.class, f.result(), result -> result.setDiemcuoi(value == null ? null : new BigDecimal(value)));
        assertThatThrownBy(() -> service.congboKetqua(f.result())).isInstanceOf(NghiepvuException.class);
        assertUnpublished();
    }

    @Test
    void duplicateComponentScoresAreRejected() {
        tx.executeWithoutResult(status -> score(f.registration(), f.reviewerA(), "7.99"));
        rejected("điểm trùng");
        assertUnpublished();
    }

    @Test
    void unapprovedRegistrationAndInconsistentPublicationDateAreRejected() {
        mutate(Dangkydetai.class, f.registration(), registration -> registration.setTrangthai(TrangthaiDangkydetai.CHODUYET));
        rejected("chưa được duyệt");
        mutate(Dangkydetai.class, f.registration(), registration -> registration.setTrangthai(TrangthaiDangkydetai.DADUYET));
        mutate(Ketqua.class, f.result(), result -> result.setNgaycongbo(LocalDateTime.of(2020, 1, 1, 0, 0)));
        rejected("cần đối soát");
        assertThat(fake.attempts).isEmpty();
    }

    @Test
    void disabledLecturerAccountCannotProvideValidRequiredScore() {
        tx.executeWithoutResult(status -> {
            var teacher = em.find(Giangvien.class, f.reviewerA());
            em.find(Taikhoan.class, teacher.getTaikhoanid()).setKichhoat(false);
        });
        rejected("tài khoản đã bị khóa");
        assertUnpublished();
    }

    @Test
    void missingEmailDoesNotUndoPublicationAndCanBeRetriedAfterCorrection() {
        mutate(Taikhoan.class, f.accountB(), account -> account.setEmail(null));
        service.congboKetqua(f.result());
        assertThat(fake.attempts).hasSize(1);
        var missing = emailRows().stream().filter(row -> row.getTrangthai() == TrangthaiEmailketqua.MISSING_EMAIL).findFirst().orElseThrow();
        assertThat(missing.getSolanthu()).isZero();
        assertThatThrownBy(() -> outbox.retry(missing.getId(), false)).isInstanceOf(NghiepvuException.class);
        mutate(Taikhoan.class, f.accountB(), account -> account.setEmail("corrected@example.test"));
        outbox.retry(missing.getId(), false);
        var delivered = emails.findById(missing.getId()).orElseThrow();
        assertThat(delivered.getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        assertThat(delivered.getNoidung()).isEqualTo(missing.getNoidung());
        assertThat(emailRows()).hasSize(2);
        assertThat(fake.attempts).hasSize(2);
    }

    @Test
    void definiteSmtpFailureIsSeparateAndRetryDoesNotDuplicateSentMail() {
        fake.outcome = FakeMail.Outcome.DEFINITE_FAILURE;
        service.congboKetqua(f.result());
        assertThat(results.findById(f.result()).orElseThrow().getDacongbo()).isTrue();
        assertThat(emailRows()).allMatch(row -> row.getTrangthai() == TrangthaiEmailketqua.FAILED);
        var retry = emailRows().getFirst();
        fake.outcome = FakeMail.Outcome.OK;
        outbox.retry(retry.getId(), false);
        assertThatThrownBy(() -> outbox.retry(retry.getId(), true)).isInstanceOf(NghiepvuException.class);
        worker.drain();
        assertThat(fake.attempts).hasSize(3);
        assertThat(emailRows()).hasSize(2);
    }

    @Test
    void uncertainOutcomeOnlyRetriesAfterExplicitManagerConfirmation() {
        fake.outcome = FakeMail.Outcome.UNCERTAIN;
        service.congboKetqua(f.result());
        var uncertain = emailRows().getFirst();
        assertThat(uncertain.getTrangthai()).isEqualTo(TrangthaiEmailketqua.UNCERTAIN);
        worker.drain(); worker.afterStartup();
        assertThat(fake.attempts).hasSize(2);
        assertThatThrownBy(() -> outbox.retry(uncertain.getId(), false)).isInstanceOf(NghiepvuException.class);
        fake.outcome = FakeMail.Outcome.OK;
        outbox.retry(uncertain.getId(), true, uncertain.getSolanthu());
        assertThat(emails.findById(uncertain.getId()).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        assertThat(fake.attempts).hasSize(3);
    }

    @Test
    void startupFindsMissedPendingWorkAndNeverResendsSent() {
        Long id = queuedWithoutEvent();
        assertThat(fake.attempts).isEmpty();
        worker.afterStartup(); worker.afterStartup();
        assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        assertThat(fake.attempts).hasSize(1);
    }

    @Test
    void uncertainConfirmationCannotBeReplayedForANewFailedAttempt() {
        fake.outcome = FakeMail.Outcome.UNCERTAIN;
        service.congboKetqua(f.result());
        var original = emailRows().getFirst();
        assertThatThrownBy(() -> outbox.retry(original.getId(), true)).isInstanceOf(NghiepvuException.class);
        outbox.retry(original.getId(), true, original.getSolanthu());
        assertThat(fake.attempts).hasSize(3);
        assertThatThrownBy(() -> outbox.retry(original.getId(), true, original.getSolanthu()))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("đã cũ");
        assertThat(fake.attempts).hasSize(3);
        fake.outcome = FakeMail.Outcome.OK;
        outbox.retry(original.getId(), true, emails.findById(original.getId()).orElseThrow().getSolanthu());
        assertThat(fake.attempts).hasSize(4);
        assertThat(emails.findById(original.getId()).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        assertThat(emailRows()).hasSize(2);
    }

    @Test
    void abandonedClaimBecomesUncertainAndLateCompletionCannotOverwriteIt() {
        Long id = queuedWithoutEvent();
        var claim = queue.claim(id);
        mutate(Emailketqua.class, id, row -> row.setLanthu(LocalDateTime.now().minusHours(1)));
        worker.drain();
        queue.complete(claim, TrangthaiEmailketqua.SENT, null);
        assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.UNCERTAIN);
        assertThat(fake.attempts).isEmpty();
    }

    @Test
    void concurrentWorkersClaimAndSendOnlyOnce() throws Exception {
        Long id = queuedWithoutEvent();
        parallel(() -> { worker.process(id); return true; }, () -> { worker.process(id); return true; });
        assertThat(fake.attempts).hasSize(1);
        assertThat(emails.findById(id).orElseThrow().getSolanthu()).isEqualTo(1);
    }

    @Test
    void concurrentPublicationCommitsOnceAndEnqueuesEachStudentOnce() throws Exception {
        var outcomes = parallel(this::publishAsChief, this::publishAsChief);
        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(emailRows()).hasSize(2);
        assertThat(fake.attempts).hasSize(2);
    }

    @Test
    void disabledTransportLeavesPendingWorkWithoutCallingSender() {
        Long id = queuedWithoutEvent();
        new KetquaEmailWorker(queue, fake, schema, false).drain();
        assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.PENDING);
        assertThat(fake.attempts).isEmpty();
    }

    @Test
    void uniqueRecipientsForeignKeysAndAssignmentCheckAreEnforcedByH2() {
        Long id = queuedWithoutEvent();
        var row = emails.findById(id).orElseThrow();
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> em.persist(email(row.getSinhvienid()))))
                .isInstanceOf(RuntimeException.class);
        assertThat(emailRows()).hasSize(1);
        assertThatThrownBy(() -> jdbc.update("insert into phanconghoidong (Dangkydetaiid,Hinhthuc,Hoidongid) values (?, 'KHONGHOIDONG', null)", Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update phanconghoidong set Hinhthuc='COHOIDONG', Hoidongid=null where Dangkydetaiid=?", f.registration()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SINHVIEN", "GIANGVIEN", "TRUONGBOMON"})
    void servicesAndAllManagementEndpointsRejectOtherRoles(String role) throws Exception {
        // Exercise both an unpublished result and an existing published result with SENT emails.
        for (boolean published : new boolean[]{false, true}) {
            if (published) {
                authenticate("chief", "TRUONGKHOA");
                service.congboKetqua(f.result());
                assertThat(emailRows()).hasSize(2).allMatch(row -> row.getTrangthai() == TrangthaiEmailketqua.SENT);
            }
            var before = publicationState();
            authenticate("unauthorized", role);
            assertThatThrownBy(() -> service.congboKetqua(f.result())).isInstanceOf(AccessDeniedException.class);
            assertThat(publicationState()).isEqualTo(before);
            assertThatThrownBy(() -> business.congboKetqua(f.result())).isInstanceOf(AccessDeniedException.class);
            assertThat(publicationState()).isEqualTo(before);
            assertThatThrownBy(() -> publication.publish(f.result())).isInstanceOf(AccessDeniedException.class);
            assertThat(publicationState()).isEqualTo(before);
            assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.KHONGHOIDONG, null)).isInstanceOf(AccessDeniedException.class);
            assertThat(publicationState()).isEqualTo(before);
            Long emailId = emailRows().isEmpty() ? 1L : emailRows().getFirst().getId();
            assertThatThrownBy(() -> outbox.retry(emailId, true)).isInstanceOf(AccessDeniedException.class);
            assertThat(publicationState()).isEqualTo(before);
            for (String url : managementUrls()) {
                mvc.perform(post(url).with(user("unauthorized").roles(role)).with(csrf()))
                        .andExpect(status().isForbidden());
                assertThat(publicationState()).as("%s POST %s, published=%s", role, url, published).isEqualTo(before);
                mvc.perform(get(url).with(user("unauthorized").roles(role)))
                        .andExpect(status().isForbidden());
                assertThat(publicationState()).as("%s GET %s, published=%s", role, url, published).isEqualTo(before);
            }
            if (!published) assertUnpublished();
        }
    }

    @Test
    void endpointsRequireCsrfAndGetCannotMutate() throws Exception {
        for (boolean published : new boolean[]{false, true}) {
            if (published) {
                authenticate("chief", "TRUONGKHOA");
                service.congboKetqua(f.result());
                assertThat(emailRows()).hasSize(2).allMatch(row -> row.getTrangthai() == TrangthaiEmailketqua.SENT);
            }
            var before = publicationState();
            for (String url : managementUrls()) {
                mvc.perform(post(url).with(user("chief").roles("TRUONGKHOA"))).andExpect(status().isForbidden());
                assertThat(publicationState()).as("POST without CSRF %s, published=%s", url, published).isEqualTo(before);
                mvc.perform(get(url).with(user("chief").roles("TRUONGKHOA"))).andExpect(status().isMethodNotAllowed());
                assertThat(publicationState()).as("GET %s, published=%s", url, published).isEqualTo(before);
            }
            if (!published) assertUnpublished();
        }
    }

    @Test
    void postIgnoresForgedDatesFlagsAndRecipientIdsAndShowsFlashFeedback() throws Exception {
        mvc.perform(post("/ketqua/" + f.result() + "/congbo").with(user("chief").roles("TRUONGKHOA")).with(csrf())
                .param("ngaycongbo", "2000-01-01T00:00:00").param("dacongbo", "false")
                .param("diemcuoi", "10.00").param("sinhvienid", "999").param("email", "attacker@example.test"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/ketqua"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(fake.attempts).extracting(ResultMailSender.Message::to)
                .containsExactlyInAnyOrder("student-a@example.test", "student-b@example.test");
        assertThat(results.findById(f.result()).orElseThrow().getNgaycongbo().getYear()).isGreaterThan(2000);
        assertThat(results.findById(f.result()).orElseThrow().getDiemcuoi()).isEqualByComparingTo("8.00");
        assertThat(emailRows()).hasSize(2);
        var beforeRepeat = publicationState();
        mvc.perform(post("/ketqua/" + f.result() + "/congbo").with(user("chief").roles("TRUONGKHOA")).with(csrf())
                .param("ngaycongbo", "2000-01-01T00:00:00").param("dacongbo", "false")
                .param("diemcuoi", "10.00").param("sinhvienid", "999").param("email", "attacker@example.test"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/ketqua"))
                .andExpect(flash().attribute("errorMessage", "Kết quả đã được công bố; giữ nguyên ngày công bố."));
        assertThat(publicationState()).isEqualTo(beforeRepeat);
    }

    @Test
    void chiefPageHasCsrfAssignmentAndPublicationFormsButStudentPageDoesNot() throws Exception {
        var chiefPage = mvc.perform(get("/ketqua").with(user("chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn();
        var forms = Pattern.compile("<form\\b[^>]*action=\"([^\"]*)\"[^>]*>(.*?)</form>", Pattern.DOTALL)
                .matcher(chiefPage.getResponse().getContentAsString());
        Set<String> actions = new HashSet<>();
        while (forms.find()) {
            if (forms.group(1).contains("/congbo") || forms.group(1).contains("/phancong")) {
                assertThat(forms.group(2)).contains("name=\"_csrf\"");
                actions.add(forms.group(1));
            }
        }
        assertThat(actions).contains("/ketqua/" + f.result() + "/congbo", "/ketqua/" + f.result() + "/phancong");
        authenticate("chief", "TRUONGKHOA");
        service.congboKetqua(f.result());
        var page = mvc.perform(get("/ketqua").with(user("student-a").roles("SINHVIEN")))
                .andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getContentAsString()).contains("8.00")
                .doesNotContain("Công bố kết quả", "Xác nhận không hội đồng", "student-b@example.test", "SENT");
        authenticate("student-a", "SINHVIEN");
        assertThat(service.findById(f.result())).isPresent();
        authenticate("other-student", "SINHVIEN");
        assertThat(service.findById(f.result())).isEmpty();
    }

    @Test
    void chiefCanAssignCouncilUsingPostAndSeeEmailStatuses() throws Exception {
        Long council = council(3, f.round());
        mvc.perform(post("/ketqua/" + f.result() + "/phancong").with(user("chief").roles("TRUONGKHOA")).with(csrf())
                .param("hinhthuc", "COHOIDONG").param("hoidongid", council.toString()))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(assignments.findById(f.registration()).orElseThrow().getHoidongid()).isEqualTo(council);
        addCouncilScores(council);
        authenticate("chief", "TRUONGKHOA");
        service.congboKetqua(f.result());
        mvc.perform(get("/ketqua").with(user("chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("student-b@example.test")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SENT")));
    }

    @Test
    void badPublicationIdReturns400AndMissingResultHasClearFlashReason() throws Exception {
        mvc.perform(post("/ketqua/not-a-number/congbo").with(user("chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/ketqua/" + Long.MAX_VALUE + "/congbo").with(user("chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(flash().attribute("errorMessage", "Không tìm thấy kết quả."));
        assertUnpublished();
    }

    @Test
    void missingNewTablesKeepStepOneListWorkingAndPublicationFailsSafely() throws Exception {
        jdbc.execute("drop table emailketqua");
        try {
            mvc.perform(get("/ketqua").with(user("chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có bảng Emailketqua")));
            authenticate("chief", "TRUONGKHOA");
            rejected("Chưa có bảng Emailketqua");
            assertThat(results.findById(f.result()).orElseThrow().getDacongbo()).isFalse();
            assertThat(results.findById(f.result()).orElseThrow().getNgaycongbo()).isNull();
            jdbc.execute("drop table phanconghoidong");
            mvc.perform(get("/ketqua").with(user("chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có bảng Phanconghoidong")));
            authenticate("chief", "TRUONGKHOA");
            rejected("Chưa có bảng Phanconghoidong");
            assertThat(fake.attempts).isEmpty();
        } finally {
            jdbc.execute("""
                    create table if not exists phanconghoidong (
                        Dangkydetaiid bigint primary key, Hinhthuc varchar(30) not null, Hoidongid bigint,
                        constraint Chk_pchd_hinhthuc check ((Hinhthuc='COHOIDONG' and Hoidongid is not null)
                            or (Hinhthuc='KHONGHOIDONG' and Hoidongid is null)))
                    """);
            jdbc.execute("""
                    create table emailketqua (Id bigint auto_increment primary key,
                        Ketquaid bigint not null, Sinhvienid bigint not null, Email varchar(150),
                        Tieude varchar(200) not null, Noidung text not null, Trangthai varchar(30) not null,
                        Solanthu integer not null, Ngaytao timestamp not null, Lanthu timestamp,
                        Ngaygui timestamp, Loi varchar(500), unique (Ketquaid,Sinhvienid))
                    """);
            new ResourceDatabasePopulator(new ClassPathResource("ketqua-new-tables-h2.sql")).execute(dataSource);
        }
    }

    @Test
    void legacyPublishedResultsAreNeverChangedOrBackfilledWithEmails() {
        LocalDateTime oldDate = LocalDateTime.of(2020, 1, 1, 8, 0);
        mutate(Ketqua.class, f.result(), row -> { row.setDacongbo(true); row.setNgaycongbo(oldDate); });
        tx.executeWithoutResult(status -> em.remove(em.find(Phanconghoidong.class, f.registration())));
        rejected("đã được công bố");
        assertThat(service.publicationInfo(List.of(results.findById(f.result()).orElseThrow())).get(f.result()).canAssign()).isFalse();
        worker.afterStartup();
        assertThat(results.findById(f.result()).orElseThrow().getNgaycongbo()).isEqualTo(oldDate);
        assertThat(emailRows()).isEmpty();
        assertThat(fake.attempts).isEmpty();
    }

    @Test
    void outboxWithUnpublishedResultCannotBeSentEvenIfQueueDataIsCorrupted() {
        Long id = tx.execute(status -> { var email = email(f.studentA()); em.persist(email); return email.getId(); });
        worker.process(id);
        assertThat(emails.findById(id).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.FAILED);
        assertThat(fake.attempts).isEmpty();
    }

    @Test
    void councilSupervisorConflictAndInvalidTeacherRoleAreRejected() {
        Long council = council(3, f.round());
        tx.executeWithoutResult(status -> {
            var member = new Thanhvienhoidong(); member.setHoidongid(council); member.setGiangvienid(f.supervisor());
            member.setVaitro(VaitroHoidong.THANHVIEN);
            var extra = em.createQuery("select m from Thanhvienhoidong m where m.hoidongid=:id and m.giangvienid<>:a and m.giangvienid<>:b", Thanhvienhoidong.class)
                    .setParameter("id", council).setParameter("a", f.reviewerA()).setParameter("b", f.reviewerB()).getSingleResult();
            em.remove(extra); em.persist(member);
        });
        assertThatThrownBy(() -> service.phanCongHoiDong(f.result(), HinhthucDanhgia.COHOIDONG, council))
                .isInstanceOf(NghiepvuException.class).hasMessageContaining("đang hướng dẫn");
        tx.executeWithoutResult(status -> em.find(Taikhoan.class, em.find(Giangvien.class, f.reviewerA()).getTaikhoanid()).setVaitro(Vaitro.SINHVIEN));
        rejected("không có quyền chấm");
        assertUnpublished();
    }

    private Fixture fixture() {
        account("chief", Vaitro.TRUONGKHOA, "chief@example.test");
        Taikhoan a = account("student-a", Vaitro.SINHVIEN, "student-a@example.test");
        Taikhoan b = account("student-b", Vaitro.SINHVIEN, "student-b@example.test");
        Sinhvien sa = student(a), sb = student(b);
        Long guide = lecturer("guide"), ra = lecturer("reviewer-a"), rb = lecturer("reviewer-b");
        Dotdangky round = round(Loaidot.MONHOC);
        Detai topic = new Detai(); topic.setDotdangkyid(round.getId()); topic.setBomonid(1L);
        topic.setTendetai("H2 test topic"); topic.setGiangviendexuatid(guide); topic.setTrangthai(TrangthaiDetai.CONGBO); em.persist(topic);
        Giangvienhuongdan supervision = new Giangvienhuongdan(); supervision.setDetaiid(topic.getId()); supervision.setGiangvienid(guide); em.persist(supervision);
        Nhomsinhvien group = new Nhomsinhvien(); group.setDotdangkyid(round.getId()); group.setNhomtruongid(sa.getId()); group.setTennhom("H2 group"); em.persist(group);
        for (Sinhvien student : List.of(sa, sb)) { Thanhviennhom member = new Thanhviennhom(); member.setNhomid(group.getId()); member.setSinhvienid(student.getId()); em.persist(member); }
        Dangkydetai registration = new Dangkydetai(); registration.setNhomid(group.getId()); registration.setDetaiid(topic.getId());
        registration.setTrangthai(TrangthaiDangkydetai.DADUYET); registration.setNgaydangky(LocalDateTime.now()); em.persist(registration);
        reviewer(registration.getId(), ra); reviewer(registration.getId(), rb);
        score(registration.getId(), ra, "7.99"); score(registration.getId(), rb, "8.00");
        Phanconghoidong assignment = new Phanconghoidong(); assignment.setDangkydetaiid(registration.getId()); assignment.setHinhthuc(HinhthucDanhgia.KHONGHOIDONG); em.persist(assignment);
        Ketqua result = new Ketqua(); result.setDangkydetaiid(registration.getId()); result.setDiemcuoi(new BigDecimal("8.00")); result.setDacongbo(false); em.persist(result);
        return new Fixture(result.getId(), registration.getId(), topic.getId(), group.getId(), round.getId(), sa.getId(), sb.getId(), a.getId(), b.getId(), ra, rb, guide);
    }

    private Taikhoan account(String name, Vaitro role, String email) {
        Taikhoan value = new Taikhoan(); value.setTendangnhap(name); value.setMatkhau("unused-test-password");
        value.setHoten(name); value.setVaitro(role); value.setKichhoat(true); value.setEmail(email); em.persist(value); return value;
    }
    private Sinhvien student(Taikhoan account) {
        Sinhvien value = new Sinhvien(); value.setTaikhoanid(account.getId()); value.setMasinhvien("MSSV-" + account.getId()); value.setBomonid(1L); em.persist(value); return value;
    }
    private Long lecturer(String name) {
        var account = account(name, Vaitro.GIANGVIEN, name + "@example.test");
        Giangvien value = new Giangvien(); value.setTaikhoanid(account.getId()); value.setMagiangvien("GV-" + account.getId()); value.setBomonid(1L); em.persist(value); return value.getId();
    }
    private Dotdangky round(Loaidot type) {
        Dotdangky value = new Dotdangky(); value.setTendot("H2 " + type); value.setLoaidot(type); value.setTrangthai(TrangthaiDotdangky.DONG);
        value.setBatdaugiangvien(LocalDateTime.now().minusMonths(2)); value.setKetthucgiangvien(LocalDateTime.now().minusMonths(1));
        value.setBatdausinhvien(LocalDateTime.now().minusWeeks(3)); value.setKetthucsinhvien(LocalDateTime.now().minusWeeks(2)); em.persist(value); return value;
    }
    private void reviewer(Long registration, Long lecturer) {
        Giangvienphanbien value = new Giangvienphanbien(); value.setDangkydetaiid(registration); value.setGiangvienid(lecturer); em.persist(value);
    }
    private void score(Long registration, Long lecturer, String score) {
        Diem value = new Diem(); value.setDangkydetaiid(registration); value.setGiangvienid(lecturer); value.setDiemso(new BigDecimal(score)); em.persist(value);
    }
    private Long council(int count, Long roundId) {
        return tx.execute(status -> {
            Hoidong value = new Hoidong(); value.setDotdangkyid(roundId); value.setTenhoidong("Council test"); em.persist(value);
            for (int i = 0; i < count; i++) {
                Thanhvienhoidong member = new Thanhvienhoidong(); member.setHoidongid(value.getId());
                member.setGiangvienid(i == 0 ? f.reviewerA() : i == 1 ? f.reviewerB() : lecturer("extra-" + sequence.incrementAndGet()));
                member.setVaitro(i == 0 ? VaitroHoidong.CHUTICH : i == 1 ? VaitroHoidong.THUKY : VaitroHoidong.THANHVIEN); em.persist(member);
            }
            return value.getId();
        });
    }
    private void addCouncilScores(Long council) {
        tx.executeWithoutResult(status -> {
            for (var member : em.createQuery("select m from Thanhvienhoidong m where m.hoidongid=:id", Thanhvienhoidong.class).setParameter("id", council).getResultList()) {
                if (!Set.of(f.reviewerA(), f.reviewerB()).contains(member.getGiangvienid())) score(f.registration(), member.getGiangvienid(), "8.00");
            }
        });
    }
    private <T> void mutate(Class<T> type, Long id, Consumer<T> mutation) { tx.executeWithoutResult(status -> mutation.accept(em.find(type, id))); }
    private List<Emailketqua> emailRows() { return emails.findByKetquaidOrderByIdAsc(f.result()); }
    private Set<String> managementUrls() {
        Long emailId = emailRows().isEmpty() ? 1L : emailRows().getFirst().getId();
        // Cover the literal URL from the acceptance requirement and an actually existing result ID.
        return new LinkedHashSet<>(List.of("/ketqua/1/congbo", "/ketqua/" + f.result() + "/congbo",
                "/ketqua/" + f.result() + "/phancong", "/ketqua/email/" + emailId + "/thulai"));
    }
    private record PublicationState(List<Map<String, Object>> results, List<Map<String, Object>> emails,
            List<ResultMailSender.Message> attempts) {}
    private PublicationState publicationState() {
        // Read fresh persisted rows, not cached mutable JPA entities. Include every column/row.
        return new PublicationState(jdbc.queryForList("select * from ketqua order by Id"),
                jdbc.queryForList("select * from emailketqua order by Id"), List.copyOf(fake.attempts));
    }
    private void rejected(String reason) { assertThatThrownBy(() -> service.congboKetqua(f.result())).isInstanceOf(NghiepvuException.class).hasMessageContaining(reason); }
    private void assertUnpublished() {
        var row = results.findById(f.result()).orElseThrow(); assertThat(row.getDacongbo()).isFalse();
        assertThat(row.getNgaycongbo()).isNull(); assertThat(emailRows()).isEmpty(); assertThat(fake.attempts).isEmpty();
    }
    private Emailketqua email(Long student) {
        Emailketqua value = new Emailketqua(); value.setKetquaid(f.result()); value.setSinhvienid(student); value.setEmail("pending@example.test");
        value.setTieude("Snapshot result"); value.setNoidung("Score 8.00"); value.setTrangthai(TrangthaiEmailketqua.PENDING); value.setNgaytao(LocalDateTime.now()); return value;
    }
    private Long queuedWithoutEvent() {
        return tx.execute(status -> { var result = em.find(Ketqua.class, f.result()); result.setDacongbo(true); result.setNgaycongbo(LocalDateTime.now());
            var email = email(f.studentA()); em.persist(email); return email.getId(); });
    }
    private boolean publishAsChief() {
        authenticate("chief", "TRUONGKHOA");
        try { service.congboKetqua(f.result()); return true; }
        catch (NghiepvuException ex) { return false; }
        finally { SecurityContextHolder.clearContext(); }
    }
    private List<Boolean> parallel(Callable<Boolean> a, Callable<Boolean> b) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { start.await(10, TimeUnit.SECONDS); return a.call(); });
            var second = pool.submit(() -> { start.await(10, TimeUnit.SECONDS); return b.call(); });
            start.countDown(); return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }
    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(username, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @TestConfiguration
    static class FakeMailConfig {
        @Bean @Primary FakeMail fakeMail(DataSource dataSource) { return new FakeMail(dataSource); }
    }
    static class FakeMail implements ResultMailSender {
        enum Outcome { OK, DEFINITE_FAILURE, UNCERTAIN }
        final DataSource dataSource;
        final List<Message> attempts = new CopyOnWriteArrayList<>();
        volatile Outcome outcome = Outcome.OK;
        volatile boolean observedCommitted = true;
        FakeMail(DataSource dataSource) { this.dataSource = dataSource; }
        void reset() { attempts.clear(); outcome = Outcome.OK; observedCommitted = true; }
        @Override public void send(Message message) {
            try (var connection = dataSource.getConnection(); var query = connection.prepareStatement(
                    "select k.Dacongbo from ketqua k join emailketqua e on k.Id=e.Ketquaid where e.Id=?")) {
                query.setLong(1, message.id());
                try (var row = query.executeQuery()) { observedCommitted &= row.next() && row.getBoolean(1); }
            } catch (Exception ex) { throw new IllegalStateException(ex); }
            attempts.add(message);
            if (outcome == Outcome.DEFINITE_FAILURE) throw new DefinitelyNotSentException("Fake SMTP rejected before delivery");
            if (outcome == Outcome.UNCERTAIN) throw new IllegalStateException("Fake lost acknowledgement");
        }
    }
}
