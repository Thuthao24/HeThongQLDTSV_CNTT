package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:diem-concurrency-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
class DiemConcurrencyTest {

    @Autowired private DangkydetaiService dangkydetaiService;
    @Autowired private DiemService diemService;
    @Autowired private GiangvienphanbienService giangvienphanbienService;

    @Autowired private DangkydetaiRepository dangkydetaiRepository;
    @Autowired private DetaiRepository detaiRepository;
    @Autowired private DotdangkyRepository dotdangkyRepository;
    @Autowired private BomonRepository bomonRepository;
    @Autowired private NhomsinhvienRepository nhomsinhvienRepository;
    @Autowired private TaikhoanRepository taikhoanRepository;
    @Autowired private GiangvienRepository giangvienRepository;
    @Autowired private SinhvienRepository sinhvienRepository;
    @Autowired private GiangvienhuongdanRepository giangvienhuongdanRepository;
    @Autowired private GiangvienphanbienRepository giangvienphanbienRepository;
    @Autowired private DiemRepository diemRepository;
    @Autowired private KetquaRepository ketquaRepository;
    @Autowired private PhanconghoidongRepository phanconghoidongRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private Long dangkyId;
    private Long gv2Id;

    @BeforeAll
    static void initConstraints(@Autowired DataSource dataSource) {
        try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE Diem ADD CONSTRAINT IF NOT EXISTS Uk_diem_giangvien UNIQUE (Dangkydetaiid, Giangvienid)");
        } catch (Exception e) {
            // Already exists or created
        }
    }

    private void cleanDatabase() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            diemRepository.deleteAll();
            phanconghoidongRepository.deleteAll();
            ketquaRepository.deleteAll();
            giangvienphanbienRepository.deleteAll();
            dangkydetaiRepository.deleteAll();
            nhomsinhvienRepository.deleteAll();
            giangvienhuongdanRepository.deleteAll();
            detaiRepository.deleteAll();
            dotdangkyRepository.deleteAll();
            sinhvienRepository.deleteAll();
            giangvienRepository.deleteAll();
            taikhoanRepository.deleteAll();
            bomonRepository.deleteAll();
        });
    }

    @BeforeEach
    void setUp() {
        cleanDatabase();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            Bomon bm = new Bomon();
            bm.setMabomon("CNTT_CONC");
            bm.setTenbomon("Cong nghe thong tin Concurrency");
            bm = bomonRepository.save(bm);

            Taikhoan tkAdmin = new Taikhoan();
            tkAdmin.setTendangnhap("TK_ADMIN_C");
            tkAdmin.setMatkhau("123456");
            tkAdmin.setHoten("Trưởng Khoa Concurrency");
            tkAdmin.setVaitro(Vaitro.TRUONGKHOA);
            tkAdmin.setKichhoat(true);
            tkAdmin = taikhoanRepository.save(tkAdmin);

            Taikhoan tkGv1 = new Taikhoan();
            tkGv1.setTendangnhap("GV_CONC1");
            tkGv1.setMatkhau("123456");
            tkGv1.setHoten("GV Huong Dan Concurrency");
            tkGv1.setVaitro(Vaitro.GIANGVIEN);
            tkGv1.setKichhoat(true);
            tkGv1 = taikhoanRepository.save(tkGv1);

            Giangvien gv1 = new Giangvien();
            gv1.setTaikhoanid(tkGv1.getId());
            gv1.setMagiangvien("GV_CONC1");
            gv1.setBomonid(bm.getId());
            gv1 = giangvienRepository.save(gv1);

            Taikhoan tkGv2 = new Taikhoan();
            tkGv2.setTendangnhap("GV_CONC2");
            tkGv2.setMatkhau("123456");
            tkGv2.setHoten("GV Phan Bien Concurrency");
            tkGv2.setVaitro(Vaitro.GIANGVIEN);
            tkGv2.setKichhoat(true);
            tkGv2 = taikhoanRepository.save(tkGv2);

            Giangvien gv2 = new Giangvien();
            gv2.setTaikhoanid(tkGv2.getId());
            gv2.setMagiangvien("GV_CONC2");
            gv2.setBomonid(bm.getId());
            gv2 = giangvienRepository.save(gv2);
            gv2Id = gv2.getId();

            Taikhoan tkSv = new Taikhoan();
            tkSv.setTendangnhap("SV_CONC");
            tkSv.setMatkhau("123456");
            tkSv.setHoten("SV Concurrency");
            tkSv.setVaitro(Vaitro.SINHVIEN);
            tkSv.setKichhoat(true);
            tkSv = taikhoanRepository.save(tkSv);

            Sinhvien sv = new Sinhvien();
            sv.setTaikhoanid(tkSv.getId());
            sv.setMasinhvien("SV_CONC");
            sv.setBomonid(bm.getId());
            sv = sinhvienRepository.save(sv);

            Dotdangky dot = new Dotdangky();
            dot.setTendot("Đợt Concurrency");
            dot.setLoaidot(Loaidot.TLCN);
            dot.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
            dot.setKetthucgiangvien(LocalDateTime.now().plusDays(10));
            dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
            dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
            dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
            dot = dotdangkyRepository.save(dot);

            Detai detai = new Detai();
            detai.setDotdangkyid(dot.getId());
            detai.setBomonid(bm.getId());
            detai.setTendetai("Đề tài Concurrency Test");
            detai.setGiangviendexuatid(gv1.getId());
            detai.setTrangthai(TrangthaiDetai.CONGBO);
            detai = detaiRepository.save(detai);

            Giangvienhuongdan gvhd = new Giangvienhuongdan();
            gvhd.setDetaiid(detai.getId());
            gvhd.setGiangvienid(gv1.getId());
            giangvienhuongdanRepository.save(gvhd);

            Nhomsinhvien nhom = new Nhomsinhvien();
            nhom.setDotdangkyid(dot.getId());
            nhom.setNhomtruongid(sv.getId());
            nhom.setTennhom("Nhóm Concurrency");
            nhom = nhomsinhvienRepository.save(nhom);

            Dangkydetai dangky = new Dangkydetai();
            dangky.setNhomid(nhom.getId());
            dangky.setDetaiid(detai.getId());
            dangky.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dangky.setNgaydangky(LocalDateTime.now());
            dangky = dangkydetaiRepository.save(dangky);
            dangkyId = dangky.getId();

            // Duyệt đăng ký và phân công GVPB
            setAuth("TK_ADMIN_C", "ROLE_TRUONGKHOA");
            dangkydetaiService.duyetDangky(dangkyId, true, "TK_ADMIN_C");
            giangvienphanbienService.phanCong(dangkyId, gv2Id, "TK_ADMIN_C");

            // Phân công hình thức đánh giá (KHONGHOIDONG cho TLCN)
            Phanconghoidong pchd = new Phanconghoidong();
            pchd.setDangkydetaiid(dangkyId);
            pchd.setHinhthuc(HinhthucDanhgia.KHONGHOIDONG);
            pchd.setHoidongid(null);
            phanconghoidongRepository.save(pchd);
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        cleanDatabase();
    }

    private void setAuth(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "123456",
                        List.of(new SimpleGrantedAuthority(role)))
        );
    }

    @Test
    @DisplayName("STT 10: Kiểm thử 2 request nhập điểm song song - Chống race condition tạo 2 bản ghi trùng")
    void testHaiRequestNhapDiemSongSong() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Cả 2 luồng cùng xuất phát đồng thời tại đúng 1 thời điểm!

                    // Mỗi luồng gắn Authentication của GV_CONC2
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken("GV_CONC2", "123456",
                                    List.of(new SimpleGrantedAuthority("ROLE_GIANGVIEN")))
                    );

                    Diem d = new Diem();
                    d.setDangkydetaiid(dangkyId);
                    d.setGiangvienid(gv2Id);
                    d.setDiemso(new BigDecimal("8.50"));
                    d.setNhanxet("Điểm chấm song song luồng " + index);

                    diemService.save(d);
                    successCount.incrementAndGet();
                } catch (NghiepvuException | DataIntegrityViolationException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Phát lệnh chạy đồng thời!
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // 1. Kiểm tra: Đúng 1 request thành công và 1 request bị chặn do trùng lặp
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // 2. Kiểm tra database: Duy nhất 1 bản ghi điểm trong bảng Diem
        List<Diem> scores = diemRepository.findByDangkydetaiid(dangkyId);
        assertThat(scores).hasSize(1);
        assertThat(scores.get(0).getGiangvienid()).isEqualTo(gv2Id);
        assertThat(scores.get(0).getDiemso()).isEqualByComparingTo("8.50");

        // 3. Kiểm tra kết quả trung gian: điểm cuối được tính chính xác dựa trên đúng 1 điểm thành phần
        Ketqua kq = ketquaRepository.findByDangkydetaiid(dangkyId).orElseThrow();
        assertThat(kq.getDiemcuoi()).isEqualByComparingTo("8.50");
    }

    @Test
    @DisplayName("STT 10: Nhập điểm tuần tự lần 2 cho cùng 1 GV/đăng ký bị từ chối")
    void testNhapDiemTuanTuTrungLapBiTuChoi() {
        setAuth("GV_CONC2", "ROLE_GIANGVIEN");

        Diem d1 = new Diem();
        d1.setDangkydetaiid(dangkyId);
        d1.setGiangvienid(gv2Id);
        d1.setDiemso(new BigDecimal("8.00"));
        d1.setNhanxet("Lần 1 thành công");
        diemService.save(d1);

        // Lần 2 nhập cho cùng đề tài và cùng GV
        Diem d2 = new Diem();
        d2.setDangkydetaiid(dangkyId);
        d2.setGiangvienid(gv2Id);
        d2.setDiemso(new BigDecimal("9.00"));
        d2.setNhanxet("Lần 2 cố tình trùng");

        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> diemService.save(d2));
        assertThat(ex.getMessage()).contains("Giảng viên này đã nhập điểm cho đăng ký");

        List<Diem> scores = diemRepository.findByDangkydetaiid(dangkyId);
        assertThat(scores).hasSize(1);
        assertThat(scores.get(0).getDiemso()).isEqualByComparingTo("8.00");
    }
}
