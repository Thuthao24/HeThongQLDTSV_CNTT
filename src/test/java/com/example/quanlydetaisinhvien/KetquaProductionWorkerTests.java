package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.KetquaPublicationService;
import com.example.quanlydetaisinhvien.service.mail.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * Kiểm thử toàn diện 15 yêu cầu bắt buộc của Module 16 khi chạy ở chế độ VẬN HÀNH THẬT (test-mode=false, Mailjet duy nhất).
 * 1. Công bố nhóm 3 sinh viên tạo đủ 3 Outbox.
 * 2. Worker xử lý đủ 3 Outbox.
 * 3. Nhóm 4 sinh viên tạo và xử lý đủ 4 Outbox.
 * 4. Nhóm 5 sinh viên tạo và xử lý đủ 5 Outbox.
 * 5. Mỗi Outbox có đúng người nhận từ dữ liệu sinh viên tương ứng.
 * 6. Nội dung email cá nhân hóa đúng sinh viên, tên đề tài và điểm.
 * 7. Không gửi email trước khi công bố.
 * 8. Công bố lặp không tạo Outbox trùng.
 * 9. Worker không gửi lại bản ghi SENT.
 * 10. FAILED được xử lý đúng.
 * 11. UNCERTAIN không bị retry mù quáng.
 * 12. PENDING tiếp tục xử lý an toàn sau restart/recovery.
 * 13. Hoàn toàn không phụ thuộc Gmail SMTP.
 * 14. Không phát sinh email thông báo thường Module 15.
 * 15. Không mất điểm, kết quả hoặc trạng thái công bố khi Mailjet gặp lỗi.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:prod-worker-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=true",
        "app.ketqua.email.scheduling-enabled=false",
        "app.ketqua.email.test-mode=false", // PRODUCTION MODE: Không giới hạn outbox-id, không recipient override!
        "app.ketqua.email.outbox-id=0",
        "app.ketqua.email.allowlist=",
        "app.ketqua.email.recipient-override="
})
@Import(KetquaProductionWorkerTests.FakeMailConfig.class)
class KetquaProductionWorkerTests {

    @Autowired EmailketquaRepository emails;
    @Autowired KetquaRepository results;
    @Autowired DangkydetaiRepository registrations;
    @Autowired DetaiRepository topics;
    @Autowired NhomsinhvienRepository groups;
    @Autowired ThanhviennhomRepository groupMembers;
    @Autowired SinhvienRepository students;
    @Autowired TaikhoanRepository accounts;
    @Autowired DotdangkyRepository rounds;
    @Autowired BomonRepository departments;
    @Autowired HoidongRepository councils;
    @Autowired ThanhvienhoidongRepository councilMembers;
    @Autowired GiangvienRepository lecturers;
    @Autowired GiangvienhuongdanRepository supervisors;
    @Autowired GiangvienphanbienRepository reviewers;
    @Autowired PhanconghoidongRepository assignments;
    @Autowired DiemRepository scores;
    @Autowired KetquaPublicationService publicationService;
    @Autowired KetquaEmailOutbox outbox;
    @Autowired KetquaEmailQueue queue;
    @Autowired KetquaEmailWorker worker;
    @Autowired FakeMail fakeMail;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager tm;

    private TransactionTemplate tx;

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeMailConfig {
        @Bean
        @Primary
        FakeMail prodFakeMail() {
            return new FakeMail();
        }
    }

    static class FakeMail implements ResultMailSender {
        final List<Message> sent = new CopyOnWriteArrayList<>();
        boolean failNext = false;
        boolean uncertainNext = false;

        @Override
        public void send(Message message) {
            if (failNext) {
                failNext = false;
                throw new ResultMailSender.DefinitelyNotSentException("Mailjet API 401 Unauthorized: Invalid API Key");
            }
            if (uncertainNext) {
                uncertainNext = false;
                throw new RuntimeException("Connection reset by peer during HTTPS response read");
            }
            sent.add(message);
        }

        void reset() {
            sent.clear();
            failNext = false;
            uncertainNext = false;
        }
    }

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(tm);
        fakeMail.reset();
        authenticateAsChief();

        tx.executeWithoutResult(status -> {
            em.createQuery("delete from Emailketqua").executeUpdate();
            em.createQuery("delete from Phanconghoidong").executeUpdate();
            em.createQuery("delete from Diem").executeUpdate();
            em.createQuery("delete from Ketqua").executeUpdate();
            em.createQuery("delete from Giangvienphanbien").executeUpdate();
            em.createQuery("delete from Giangvienhuongdan").executeUpdate();
            em.createQuery("delete from Thanhvienhoidong").executeUpdate();
            em.createQuery("delete from Hoidong").executeUpdate();
            em.createQuery("delete from Dangkydetai").executeUpdate();
            em.createQuery("delete from Thanhviennhom").executeUpdate();
            em.createQuery("delete from Nhomsinhvien").executeUpdate();
            em.createQuery("delete from Detai").executeUpdate();
            em.createQuery("delete from Sinhvien").executeUpdate();
            em.createQuery("delete from Giangvien").executeUpdate();
            em.createQuery("delete from Taikhoan").executeUpdate();
            em.createQuery("delete from Dotdangky").executeUpdate();
            em.createQuery("delete from Bomon").executeUpdate();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 1. & 2. CÔNG BỐ NHÓM 3 SINH VIÊN TẠO ĐỦ 3 OUTBOX VÀ WORKER XỬ LÝ ĐỦ 3 OUTBOX
    @Test
    void groupOf3StudentsGeneratesAndDrainsAll3Outboxes() {
        var fixture = createPublicationFixture("Đề tài KLTN Nhóm 3", 3, new BigDecimal("8.75"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });
        worker.drain();

        List<Emailketqua> outboxList = emails.findByKetquaidOrderByIdAsc(fixture.ketquaId);
        assertThat(outboxList).hasSize(3);
        assertThat(outboxList).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.SENT && e.getSolanthu() == 1);
        assertThat(fakeMail.sent).hasSize(3);
    }

    // 3. NHÓM 4 SINH VIÊN TẠO VÀ XỬ LÝ ĐỦ 4 OUTBOX
    @Test
    void groupOf4StudentsGeneratesAndDrainsAll4Outboxes() {
        var fixture = createPublicationFixture("Đề tài KLTN Nhóm 4", 4, new BigDecimal("8.80"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });
        worker.drain();

        List<Emailketqua> outboxList = emails.findByKetquaidOrderByIdAsc(fixture.ketquaId);
        assertThat(outboxList).hasSize(4);
        assertThat(outboxList).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.SENT);
        assertThat(fakeMail.sent).hasSize(4);
    }

    // 4. NHÓM 5 SINH VIÊN TẠO VÀ XỬ LÝ ĐỦ 5 OUTBOX (KÍCH THƯỚC TỐI ĐA)
    @Test
    void groupOf5StudentsGeneratesAndDrainsAll5Outboxes() {
        var fixture = createPublicationFixture("Đề tài KLTN Nhóm 5", 5, new BigDecimal("9.20"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });
        worker.drain();

        List<Emailketqua> outboxList = emails.findByKetquaidOrderByIdAsc(fixture.ketquaId);
        assertThat(outboxList).hasSize(5);
        assertThat(outboxList).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.SENT);
        assertThat(fakeMail.sent).hasSize(5);
    }

    // 5. & 6. MỖI OUTBOX ĐÚNG NGƯỜI NHẬN, NỘI DUNG CÁ NHÂN HÓA ĐÚNG SINH VIÊN, ĐỀ TÀI, ĐIỂM
    @Test
    void eachOutboxHasExactRecipientAndPersonalizedContent() {
        var fixture = createPublicationFixture("Xây dựng Hệ thống AI Phân tán", 3, new BigDecimal("9.50"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });
        worker.drain();

        assertThat(fakeMail.sent).hasSize(3);
        List<String> recipients = fakeMail.sent.stream().map(ResultMailSender.Message::to).toList();
        assertThat(recipients).containsExactlyInAnyOrder(
                "stu-1@student.hcmute.edu.vn",
                "stu-2@student.hcmute.edu.vn",
                "stu-3@student.hcmute.edu.vn"
        );

        for (var msg : fakeMail.sent) {
            assertThat(msg.subject()).contains("Xây dựng Hệ thống AI Phân tán");
            assertThat(msg.body()).contains("9.50");
            assertThat(msg.body()).doesNotContain("[CHẾ ĐỘ THỬ NGHIỆM");
        }
    }

    // 7. KHÔNG GỬI EMAIL TRƯỚC KHI CÔNG BỐ
    @Test
    void noEmailIsSentBeforePublication() {
        createPublicationFixture("Đề tài Chưa Công Bố", 3, new BigDecimal("8.00"));

        // Gọi worker.drain() khi chưa công bố
        worker.drain();
        worker.afterStartup();

        assertThat(emails.count()).isEqualTo(0);
        assertThat(fakeMail.sent).isEmpty();
    }

    // 8. CÔNG BỐ LẶP KHÔNG TẠO OUTBOX TRÙNG
    @Test
    void duplicatePublicationDoesNotCreateDuplicateOutbox() {
        var fixture = createPublicationFixture("Đề tài Chống Trùng Công Bố", 3, new BigDecimal("8.50"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });

        // Cố tình gọi publish lần 2
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> publicationService.publish(fixture.ketquaId)))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã được công bố");

        worker.drain();
        assertThat(emails.findByKetquaidOrderByIdAsc(fixture.ketquaId)).hasSize(3);
        assertThat(fakeMail.sent).hasSize(3);
    }

    // 9. WORKER KHÔNG GỬI LẠI BẢN GHI SENT (CHỐNG GỬI TRÙNG)
    @Test
    void workerNeverResendsAlreadySentRecords() {
        var fixture = createPublicationFixture("Đề tài Chống Gửi Lại", 3, new BigDecimal("8.00"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });
        worker.drain();
        assertThat(fakeMail.sent).hasSize(3);

        // Chạy lại drain() nhiều lần
        fakeMail.reset();
        worker.drain();
        worker.drain();
        worker.afterStartup();

        assertThat(fakeMail.sent).isEmpty();
        assertThat(emails.findByKetquaidOrderByIdAsc(fixture.ketquaId)).allMatch(e -> e.getSolanthu() == 1);
    }

    // 10. & 15. FAILED ĐƯỢC XỬ LÝ ĐÚNG, KHÔNG LÀM MẤT ĐIỂM HOẶC TRẠNG THÁI CÔNG BỐ KHI MAILJET LỖI
    @Test
    void failedMailjetDeliveryMarksOutboxAsFailedWithoutAffectingPublishedResult() {
        var fixture = createPublicationFixture("Đề tài Mailjet Lỗi", 3, new BigDecimal("8.50"));

        // Giả lập Mailjet API từ chối gửi bản ghi đầu tiên khi publish kích hoạt sau commit
        fakeMail.failNext = true;

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });

        worker.drain();

        // 1 bản ghi bị FAILED, 2 bản ghi còn lại thành công SENT
        var outboxList = emails.findByKetquaidOrderByIdAsc(fixture.ketquaId);
        assertThat(outboxList.get(0).getTrangthai()).isEqualTo(TrangthaiEmailketqua.FAILED);
        assertThat(outboxList.get(0).getLoi()).contains("Mailjet");
        assertThat(outboxList.get(1).getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);
        assertThat(outboxList.get(2).getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);

        // Điểm số và trạng thái công bố trong bảng Ketqua vẫn được bảo toàn nguyên vẹn
        Ketqua kq = results.findById(fixture.ketquaId).orElseThrow();
        assertThat(kq.getDacongbo()).isTrue();
        assertThat(kq.getDiemcuoi()).isEqualByComparingTo("8.50");
        assertThat(kq.getNgaycongbo()).isNotNull();
    }

    // 11. UNCERTAIN KHÔNG BỊ RETRY MÙ QUÁNG
    @Test
    void uncertainLeaseExpiryNeverRetriedBlindly() {
        var fixture = createPublicationFixture("Đề tài Treo Gửi", 3, new BigDecimal("8.00"));

        tx.executeWithoutResult(status -> {
            Ketqua kq = results.findById(fixture.ketquaId).orElseThrow();
            kq.setDacongbo(true);
            kq.setNgaycongbo(LocalDateTime.now());
            results.saveAndFlush(kq);
            outbox.enqueue(kq);
        });

        var pendingEmails = emails.findByKetquaidOrderByIdAsc(fixture.ketquaId);
        var stuckEmail = pendingEmails.get(0);
        stuckEmail.setTrangthai(TrangthaiEmailketqua.SENDING);
        stuckEmail.setSolanthu(1);
        stuckEmail.setLanthu(LocalDateTime.now().minusMinutes(20));
        emails.saveAndFlush(stuckEmail);

        worker.drain();

        var recheckStuck = emails.findById(stuckEmail.getId()).orElseThrow();
        assertThat(recheckStuck.getTrangthai()).isEqualTo(TrangthaiEmailketqua.UNCERTAIN);
        assertThat(recheckStuck.getLoi()).contains("đối soát");

        fakeMail.reset();
        worker.drain();
        worker.drain();
        assertThat(fakeMail.sent).isEmpty();
        assertThat(emails.findById(stuckEmail.getId()).orElseThrow().getTrangthai()).isEqualTo(TrangthaiEmailketqua.UNCERTAIN);
    }

    // 12. PENDING TIẾP TỤC XỬ LÝ AN TOÀN SAU RESTART/RECOVERY
    @Test
    void pendingOutboxResumesSafelyOnRestart() {
        var fixture = createPublicationFixture("Đề tài Khởi Động Lại", 3, new BigDecimal("8.00"));

        tx.executeWithoutResult(status -> {
            Ketqua kq = results.findById(fixture.ketquaId).orElseThrow();
            kq.setDacongbo(true);
            kq.setNgaycongbo(LocalDateTime.now().minusHours(1));
            results.saveAndFlush(kq);
            outbox.enqueue(kq);
        });

        assertThat(emails.count()).isEqualTo(3);
        assertThat(emails.findAll()).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.PENDING);
        assertThat(fakeMail.sent).isEmpty();

        // Mô phỏng sự kiện khởi động lại sau sleep của Render Free
        worker.afterStartup();

        assertThat(fakeMail.sent).hasSize(3);
        assertThat(emails.findAll()).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.SENT);
    }

    // 13. & 14. HOÀN TOÀN KHÔNG PHỤ THUỘC GMAIL SMTP, KHÔNG PHÁT SINH EMAIL THÔNG BÁO MODULE 15
    @Test
    void noOrdinaryNotificationEmailModule15Generated() {
        var fixture = createPublicationFixture("Đề tài Cách Ly Module 15", 3, new BigDecimal("8.50"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });
        worker.drain();

        // 1. Chỉ có đúng 3 bản ghi trong bảng emailketqua
        assertThat(emails.count()).isEqualTo(3);
        // 2. Tiêu đề email hoàn toàn là kết quả đề tài HCMUTE, không phải thông báo thường Module 15
        assertThat(fakeMail.sent).allMatch(msg -> msg.subject().startsWith("Kết quả đề tài sinh viên HCMUTE"));
    }

    // BẢN GHI ĐÃ SENT BỊ KHÓA, CHỐNG RETRY
    @Test
    void sentRecordCannotBeRetriedByOperator() {
        var fixture = createPublicationFixture("Đề tài Chặn Retry Sent", 3, new BigDecimal("8.50"));

        tx.executeWithoutResult(status -> {
            publicationService.publish(fixture.ketquaId);
        });
        worker.drain();

        var sentEmail = emails.findByKetquaidOrderByIdAsc(fixture.ketquaId).get(0);
        assertThat(sentEmail.getTrangthai()).isEqualTo(TrangthaiEmailketqua.SENT);

        assertThatThrownBy(() -> outbox.retry(sentEmail.getId(), false))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Không được gửi lại email ở trạng thái SENT");
    }

    // NHIỀU NHÓM CÔNG BỐ ĐỒNG THỜI (3 SV + 4 SV = 7 SV)
    @Test
    void multipleGroupsPublishedConcurrentlyAllDrainedToRespectiveRecipients() {
        var groupA = createPublicationFixture("Đề tài AI Logistic", 3, new BigDecimal("8.50"), "A");
        var groupB = createPublicationFixture("Đề tài Blockchain IoT", 4, new BigDecimal("9.00"), "B");

        tx.executeWithoutResult(status -> {
            publicationService.publish(groupA.ketquaId);
            publicationService.publish(groupB.ketquaId);
        });
        worker.drain();

        assertThat(fakeMail.sent).hasSize(7);
        assertThat(emails.count()).isEqualTo(7);
        assertThat(emails.findAll()).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.SENT);

        var sentA = fakeMail.sent.stream().filter(m -> m.to().contains("groupa")).toList();
        assertThat(sentA).hasSize(3);
        assertThat(sentA).allMatch(m -> m.subject().contains("Đề tài AI Logistic") && m.body().contains("8.50"));

        var sentB = fakeMail.sent.stream().filter(m -> m.to().contains("groupb")).toList();
        assertThat(sentB).hasSize(4);
        assertThat(sentB).allMatch(m -> m.subject().contains("Đề tài Blockchain IoT") && m.body().contains("9.00"));
    }

    // =========================================================================
    // HELPER TẠO DỮ LIỆU MẪU
    // =========================================================================

    private record FixtureData(Long ketquaId, Long dangkyId, Long topicId, Long groupId) {}

    private FixtureData createPublicationFixture(String topicName, int studentCount, BigDecimal finalScore) {
        return createPublicationFixture(topicName, studentCount, finalScore, "");
    }

    private FixtureData createPublicationFixture(String topicName, int studentCount, BigDecimal finalScore, String prefix) {
        final Long[] out = new Long[4];

        tx.executeWithoutResult(status -> {
            Bomon bm = new Bomon();
            bm.setMabomon("CNTT" + prefix);
            bm.setTenbomon("Khoa CNTT");
            em.persist(bm);

            Dotdangky round = new Dotdangky();
            round.setTendot("KLTN 2026 " + prefix);
            round.setLoaidot(Loaidot.KLTN);
            round.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
            round.setBatdaugiangvien(LocalDateTime.now().minusMonths(2));
            round.setKetthucgiangvien(LocalDateTime.now().minusMonths(1));
            round.setBatdausinhvien(LocalDateTime.now().minusWeeks(3));
            round.setKetthucsinhvien(LocalDateTime.now().plusWeeks(3));
            em.persist(round);

            Giangvien gvGuide = createLecturer("guide" + prefix, "GV Hướng Dẫn " + prefix, bm.getId());
            Giangvien gvReviewer = createLecturer("rev" + prefix, "GV Phản Biện " + prefix, bm.getId());

            Giangvien gvChair = createLecturer("chair" + prefix, "Chủ Tịch " + prefix, bm.getId());
            Giangvien gvSec = createLecturer("sec" + prefix, "Thư Ký " + prefix, bm.getId());
            Giangvien gvMem = createLecturer("mem" + prefix, "Ủy Viên " + prefix, bm.getId());

            Hoidong council = new Hoidong();
            council.setDotdangkyid(round.getId());
            council.setTenhoidong("Hội đồng " + prefix);
            council.setNgaybaocao(LocalDateTime.now().plusDays(5));
            council.setPhong("P301");
            em.persist(council);

            createCouncilMember(council.getId(), gvChair.getId(), VaitroHoidong.CHUTICH);
            createCouncilMember(council.getId(), gvSec.getId(), VaitroHoidong.THUKY);
            createCouncilMember(council.getId(), gvMem.getId(), VaitroHoidong.THANHVIEN);

            List<Sinhvien> studentList = new ArrayList<>();
            for (int i = 1; i <= studentCount; i++) {
                String tag = prefix.isEmpty() ? String.valueOf(i) : (prefix.toLowerCase(Locale.ROOT) + "-" + i);
                String email = prefix.isEmpty() ? ("stu-" + i + "@student.hcmute.edu.vn") : ("stu-" + i + "@group" + prefix.toLowerCase(Locale.ROOT) + ".test");
                studentList.add(createStudent("sv-" + tag, "Sinh viên " + tag, email, bm.getId()));
            }

            Nhomsinhvien group = new Nhomsinhvien();
            group.setDotdangkyid(round.getId());
            group.setTennhom("Nhóm " + prefix);
            group.setNhomtruongid(studentList.get(0).getId());
            em.persist(group);
            out[3] = group.getId();

            for (Sinhvien s : studentList) {
                Thanhviennhom tv = new Thanhviennhom();
                tv.setNhomid(group.getId());
                tv.setSinhvienid(s.getId());
                em.persist(tv);
            }

            Detai topic = new Detai();
            topic.setDotdangkyid(round.getId());
            topic.setBomonid(bm.getId());
            topic.setTendetai(topicName);
            topic.setGiangviendexuatid(gvGuide.getId());
            topic.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(topic);
            out[2] = topic.getId();

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(topic.getId());
            hd.setGiangvienid(gvGuide.getId());
            em.persist(hd);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(topic.getId());
            dk.setNhomid(group.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now().minusDays(5));
            em.persist(dk);
            out[1] = dk.getId();

            Giangvienphanbien pb = new Giangvienphanbien();
            pb.setDangkydetaiid(dk.getId());
            pb.setGiangvienid(gvReviewer.getId());
            em.persist(pb);

            Phanconghoidong pchd = new Phanconghoidong();
            pchd.setDangkydetaiid(dk.getId());
            pchd.setHinhthuc(HinhthucDanhgia.COHOIDONG);
            pchd.setHoidongid(council.getId());
            em.persist(pchd);

            Long[] graders = {gvReviewer.getId(), gvChair.getId(), gvSec.getId(), gvMem.getId()};
            for (Long graderId : graders) {
                Diem d = new Diem();
                d.setDangkydetaiid(dk.getId());
                d.setGiangvienid(graderId);
                d.setDiemso(finalScore);
                d.setNhanxet("Đạt yêu cầu");
                em.persist(d);
            }

            Ketqua kq = new Ketqua();
            kq.setDangkydetaiid(dk.getId());
            kq.setDacongbo(false);
            kq.setDiemcuoi(finalScore);
            em.persist(kq);
            out[0] = kq.getId();

            em.flush();
        });

        return new FixtureData(out[0], out[1], out[2], out[3]);
    }

    private Giangvien createLecturer(String username, String name, Long deptId) {
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap(username);
        tk.setMatkhau("secret");
        tk.setHoten(name);
        tk.setVaitro(Vaitro.GIANGVIEN);
        tk.setEmail(username + "@lecturer.hcmute.edu.vn");
        tk.setKichhoat(true);
        em.persist(tk);

        Giangvien gv = new Giangvien();
        gv.setTaikhoanid(tk.getId());
        gv.setMagiangvien("GV-" + username.toUpperCase());
        gv.setBomonid(deptId);
        em.persist(gv);
        return gv;
    }

    private Sinhvien createStudent(String username, String name, String email, Long deptId) {
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap(username);
        tk.setMatkhau("secret");
        tk.setHoten(name);
        tk.setVaitro(Vaitro.SINHVIEN);
        tk.setEmail(email);
        tk.setKichhoat(true);
        em.persist(tk);

        Sinhvien sv = new Sinhvien();
        sv.setTaikhoanid(tk.getId());
        sv.setMasinhvien("MSV-" + username.toUpperCase());
        sv.setBomonid(deptId);
        em.persist(sv);
        return sv;
    }

    private void createCouncilMember(Long councilId, Long lecturerId, VaitroHoidong role) {
        Thanhvienhoidong tv = new Thanhvienhoidong();
        tv.setHoidongid(councilId);
        tv.setGiangvienid(lecturerId);
        tv.setVaitro(role);
        em.persist(tv);
    }

    private void authenticateAsChief() {
        var auth = new UsernamePasswordAuthenticationToken("chief", "password",
                List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
