package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.NhomsinhvienService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:membership-concurrency-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MembershipConcurrencyTests {

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        if (isMySqlAvailable()) {
            registry.add("spring.datasource.url", () -> "jdbc:mysql://127.0.0.1:3306/quanlydetaisinhvien?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            registry.add("spring.datasource.username", () -> "root");
            registry.add("spring.datasource.password", () -> "123456");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
            registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        }
    }

    public static boolean isMySqlAvailable() {
        try (var socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 3306), 1000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private NhomsinhvienService nhomService;

    @Autowired
    private NhomsinhvienRepository nhomRepo;

    @Autowired
    private ThanhviennhomRepository thanhviennhomRepo;

    @Autowired
    private DangkydetaiRepository dangkyRepo;

    @Autowired
    private DetaiRepository detaiRepo;

    @Autowired
    private SinhvienRepository sinhvienRepo;

    @Autowired
    private TaikhoanRepository taikhoanRepo;

    @Autowired
    private BomonRepository bomonRepo;

    @Autowired
    private DotdangkyRepository dotdangkyRepo;

    private String uid;
    private Nhomsinhvien group;
    private Sinhvien leader;
    private List<Sinhvien> members;
    private Dangkydetai registration;

    @BeforeEach
    void setUp() {
        uid = UUID.randomUUID().toString().substring(0, 8);

        Bomon bm = new Bomon();
        bm.setMabomon("BM_" + uid);
        bm.setTenbomon("Khoa CNTT " + uid);
        bm = bomonRepo.save(bm);

        Dotdangky dot = new Dotdangky();
        dot.setTendot("Dot 2026 " + uid);
        dot.setLoaidot(Loaidot.KLTN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(20));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(20));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        dot = dotdangkyRepo.save(dot);

        // Leader
        Taikhoan tkLeader = new Taikhoan();
        tkLeader.setTendangnhap("lead_" + uid);
        tkLeader.setMatkhau("pass123");
        tkLeader.setHoten("Group Leader");
        tkLeader.setEmail("lead_" + uid + "@test.com");
        tkLeader.setVaitro(Vaitro.SINHVIEN);
        tkLeader.setKichhoat(true);
        tkLeader = taikhoanRepo.save(tkLeader);

        leader = new Sinhvien();
        leader.setMasinhvien("lead_" + uid);
        leader.setTaikhoanid(tkLeader.getId());
        leader.setBomonid(bm.getId());
        leader = sinhvienRepo.save(leader);

        // Create Group
        group = new Nhomsinhvien();
        group.setDotdangkyid(dot.getId());
        group.setTennhom("Group " + uid);
        group.setNhomtruongid(leader.getId());
        group = nhomRepo.save(group);

        thanhviennhomRepo.save(new Thanhviennhom(group.getId(), leader.getId()));

        members = new ArrayList<>();
        members.add(leader);

        // Add 3 more members -> total 4 members
        for (int i = 1; i <= 3; i++) {
            Taikhoan tk = new Taikhoan();
            tk.setTendangnhap("mem_" + i + "_" + uid);
            tk.setMatkhau("pass123");
            tk.setHoten("Member " + i);
            tk.setEmail("mem" + i + "_" + uid + "@test.com");
            tk.setVaitro(Vaitro.SINHVIEN);
            tk.setKichhoat(true);
            tk = taikhoanRepo.save(tk);

            Sinhvien sv = new Sinhvien();
            sv.setMasinhvien("mem_" + i + "_" + uid);
            sv.setTaikhoanid(tk.getId());
            sv.setBomonid(bm.getId());
            sv = sinhvienRepo.save(sv);

            thanhviennhomRepo.save(new Thanhviennhom(group.getId(), sv.getId()));
            members.add(sv);
        }

        // Register topic in CHODUYET status
        Detai dt = new Detai();
        dt.setDotdangkyid(dot.getId());
        dt.setTendetai("Topic Under Review " + uid);
        dt.setBomonid(bm.getId());
        dt.setGiangviendexuatid(1L);
        dt.setTrangthai(TrangthaiDetai.CHODUYET);
        dt = detaiRepo.save(dt);

        registration = new Dangkydetai();
        registration.setNhomid(group.getId());
        registration.setDetaiid(dt.getId());
        registration.setNgaydangky(LocalDateTime.now());
        registration.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        registration = dangkyRepo.save(registration);
    }

    @Test
    @DisplayName("CONC-01: Concurrent xoaThanhVien preserves minimum 3 members in CHODUYET")
    void testConcurrentXoaThanhVienPreservesMinMembers() throws Exception {
        assertThat(thanhviennhomRepo.findByNhomid(group.getId())).hasSize(4);

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        Sinhvien member2 = members.get(2);
        Sinhvien member3 = members.get(3);

        executor.submit(() -> {
            try {
                startGate.await();
                nhomService.xoaThanhVien(group.getId(), member2.getId(), "lead_" + uid);
                successCount.incrementAndGet();
            } catch (NghiepvuException e) {
                rejectedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startGate.await();
                nhomService.xoaThanhVien(group.getId(), member3.getId(), "lead_" + uid);
                successCount.incrementAndGet();
            } catch (NghiepvuException e) {
                rejectedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(rejectedCount.get()).isEqualTo(1);

        long finalMembers = thanhviennhomRepo.findByNhomid(group.getId()).size();
        assertThat(finalMembers).isEqualTo(3);
    }

    @Test
    @DisplayName("CONC-02: Concurrent roiNhom preserves minimum 3 members in CHODUYET")
    void testConcurrentRoiNhomPreservesMinMembers() throws Exception {
        assertThat(thanhviennhomRepo.findByNhomid(group.getId())).hasSize(4);

        int concurrency = 2;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        Sinhvien member2 = members.get(2);
        Sinhvien member3 = members.get(3);

        executor.submit(() -> {
            try {
                startGate.await();
                nhomService.roiNhom(group.getId(), member2.getMasinhvien());
                successCount.incrementAndGet();
            } catch (NghiepvuException e) {
                rejectedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startGate.await();
                nhomService.roiNhom(group.getId(), member3.getMasinhvien());
                successCount.incrementAndGet();
            } catch (NghiepvuException e) {
                rejectedCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(rejectedCount.get()).isEqualTo(1);

        long finalMembers = thanhviennhomRepo.findByNhomid(group.getId()).size();
        assertThat(finalMembers).isEqualTo(3);
    }
}
