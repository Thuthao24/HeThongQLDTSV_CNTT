package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.DiemFormDto;
import com.example.quanlydetaisinhvien.dto.PhanbienFormDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:p0-verify;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class P0VerificationTests {

    @Autowired private DataSource dataSource;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactions;

    @Autowired private DangkydetaiService dangkydetaiService;
    @Autowired private DangkydetaiRepository dangkydetaiRepository;
    @Autowired private KetquaRepository ketquaRepository;
    @Autowired private GiangvienphanbienService giangvienphanbienService;
    @Autowired private DiemService diemService;
    @Autowired private KetquaService ketquaService;
    @Autowired private EmailketquaRepository emailketquaRepository;
    @Autowired private TaikhoanRepository taikhoanRepository;

    private TransactionTemplate tx;

    private void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, "N/A", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @BeforeAll
    void initConstraints() {
        new ResourceDatabasePopulator(new ClassPathResource("ketqua-new-tables-h2.sql")).execute(dataSource);
        tx = new TransactionTemplate(transactions);
    }

    @BeforeEach
    void setupAuth() {
        authenticate("chief", "TRUONGKHOA");
        tx.executeWithoutResult(status -> {
            if (taikhoanRepository.findByTendangnhap("chief").isEmpty()) {
                createAccount("chief", Vaitro.TRUONGKHOA, "chief@test.com");
            }
        });
    }

    private Dotdangky createRound(Loaidot type) {
        Dotdangky r = new Dotdangky();
        r.setTendot("Đợt test " + type);
        r.setLoaidot(type);
        r.setTrangthai(TrangthaiDotdangky.DONG);
        r.setBatdaugiangvien(LocalDateTime.now().minusMonths(2));
        r.setKetthucgiangvien(LocalDateTime.now().minusMonths(1));
        r.setBatdausinhvien(LocalDateTime.now().minusWeeks(3));
        r.setKetthucsinhvien(LocalDateTime.now().minusWeeks(2));
        em.persist(r);
        return r;
    }

    private Taikhoan createAccount(String name, Vaitro role, String email) {
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap(name);
        tk.setMatkhau("pwd");
        tk.setHoten(name);
        tk.setVaitro(role);
        tk.setEmail(email);
        tk.setKichhoat(true);
        em.persist(tk);
        return tk;
    }

    private Long createLecturer(String name) {
        Taikhoan tk = createAccount(name, Vaitro.GIANGVIEN, name + "@hcmute.edu.vn");
        Giangvien gv = new Giangvien();
        gv.setTaikhoanid(tk.getId());
        gv.setMagiangvien("GV-" + tk.getId());
        gv.setBomonid(1L);
        em.persist(gv);
        return gv.getId();
    }

    private Sinhvien createStudent(String name) {
        Taikhoan tk = createAccount(name, Vaitro.SINHVIEN, name + "@student.edu.vn");
        Sinhvien sv = new Sinhvien();
        sv.setTaikhoanid(tk.getId());
        sv.setMasinhvien("SV-" + tk.getId());
        sv.setBomonid(1L);
        em.persist(sv);
        return sv;
    }

    // =========================================================================
    // P0-01: ĐĂNG KÝ ĐƯỢC DUYỆT TỰ ĐỘNG SINH KẾT QUẢ TRUNG GIAN ĐỂ NHẬP ĐIỂM
    // =========================================================================

    @Test
    @DisplayName("P0-01 PASS: Duyệt đăng ký đề tài tự động tạo bản ghi Ketqua trung gian với dacongbo=false")
    void testDuyetDangKy_TuDongTaoKetquaTrungGian() {
        Long regId = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.KLTN);
            Long guide = createLecturer("gvhd-p01");
            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài P0-01");
            dt.setGiangviendexuatid(guide);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P0-01");
            grp.setNhomtruongid(1L);
            em.persist(grp);
            for (int i = 1; i <= 3; i++) {
                Sinhvien s = createStudent("sv-auto-" + i);
                if (i == 1) grp.setNhomtruongid(s.getId());
                em.persist(new Thanhviennhom(grp.getId(), s.getId()));
            }

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);
            return dk.getId();
        });

        // Trước khi duyệt, chưa có Ketqua
        assertThat(ketquaRepository.findByDangkydetaiid(regId)).isEmpty();

        // Trưởng khoa duyệt chấp nhận đăng ký
        dangkydetaiService.duyetDangky(regId, true, "chief");

        // Xác minh: trạng thái là DADUYET và đã tự động tạo Ketqua trung gian
        Dangkydetai dk = dangkydetaiRepository.findById(regId).orElseThrow();
        assertThat(dk.getTrangthai()).isEqualTo(TrangthaiDangkydetai.DADUYET);

        Optional<Ketqua> kqOpt = ketquaRepository.findByDangkydetaiid(regId);
        assertThat(kqOpt).isPresent();
        Ketqua kq = kqOpt.get();
        assertThat(kq.getDacongbo()).isFalse();
        assertThat(kq.getDiemcuoi()).isNull();
        assertThat(kq.getNgaycongbo()).isNull();
    }

    @Test
    @DisplayName("P0-01 PASS: Duyệt lặp lại không tạo bản ghi Ketqua trùng lặp")
    void testDuyetDangKy_KhongTaoKetquaTrungLap() {
        Long regId = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.KLTN);
            Long guide = createLecturer("gvhd-idempotent");
            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Idempotent");
            dt.setGiangviendexuatid(guide);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm Idempotent");
            grp.setNhomtruongid(1L);
            em.persist(grp);
            for (int i = 1; i <= 3; i++) {
                Sinhvien s = createStudent("sv-idempotent-" + i);
                if (i == 1) grp.setNhomtruongid(s.getId());
                em.persist(new Thanhviennhom(grp.getId(), s.getId()));
            }

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);
            return dk.getId();
        });

        dangkydetaiService.duyetDangky(regId, true, "chief");

        // Duyệt lần 2 bị chặn vì không còn ở trạng thái CHODUYET
        assertThatThrownBy(() -> dangkydetaiService.duyetDangky(regId, true, "chief"))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Chỉ xử lý đăng ký đang chờ duyệt");

        List<Ketqua> kqs = ketquaRepository.findAll().stream()
                .filter(k -> regId.equals(k.getDangkydetaiid()))
                .toList();
        assertThat(kqs).hasSize(1);
    }

    // =========================================================================
    // P0-02: PHÂN CÔNG GIẢNG VIÊN PHẢN BIỆN (RÀNG BUỘC TOÀN VẸN & BẢO VỆ ĐIỂM)
    // =========================================================================

    @Test
    @DisplayName("P0-02 FAIL: Chặn phân công GVHD làm GVPB của chính đề tài mình hướng dẫn qua phanCong()")
    void testPhanCongGVPB_ThatBai_KhiLaGVHD_QuaPhanCong() {
        record Pair(Long regId, Long guideId) {}
        Pair p = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.KLTN);
            Long guide = createLecturer("gvhd-p02-pc");
            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài P0-02 PhanCong");
            dt.setGiangviendexuatid(guide);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan gvhd = new Giangvienhuongdan();
            gvhd.setDetaiid(dt.getId());
            gvhd.setGiangvienid(guide);
            em.persist(gvhd);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P0-02 PC");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);
            return new Pair(dk.getId(), guide);
        });

        PhanbienFormDto form = new PhanbienFormDto(p.regId(), p.guideId());
        assertThatThrownBy(() -> giangvienphanbienService.phanCong(form))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Giang vien huong dan khong duoc lam giang vien phan bien");
    }

    @Test
    @DisplayName("P0-02 FAIL: Chặn gọi trực tiếp save() để lách validation phân công GVHD làm GVPB")
    void testPhanCongGVPB_ThatBai_KhiLaGVHD_QuaDirectSave() {
        record Pair(Long regId, Long guideId) {}
        Pair p = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.KLTN);
            Long guide = createLecturer("gvhd-p02-save");
            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài P0-02 Save");
            dt.setGiangviendexuatid(guide);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan gvhd = new Giangvienhuongdan();
            gvhd.setDetaiid(dt.getId());
            gvhd.setGiangvienid(guide);
            em.persist(gvhd);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P0-02 Save");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);
            return new Pair(dk.getId(), guide);
        });

        Giangvienphanbien entity = new Giangvienphanbien();
        entity.setDangkydetaiid(p.regId());
        entity.setGiangvienid(p.guideId());

        assertThatThrownBy(() -> giangvienphanbienService.save(entity))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Giang vien huong dan khong duoc lam giang vien phan bien");
    }

    @Test
    @DisplayName("P0-02 FAIL: Chặn xóa phân công GVPB khi giảng viên đã vào điểm trong bảng Diem")
    void testXoaPhancong_ThatBai_KhiDaCoDiem() {
        Long pbId = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.KLTN);
            Long guide = createLecturer("gvhd-p02-grade");
            Long reviewer = createLecturer("gvpb-p02-grade");
            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài P0-02 Grade");
            dt.setGiangviendexuatid(guide);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P0-02 Grade");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            Giangvienphanbien pb = new Giangvienphanbien();
            pb.setDangkydetaiid(dk.getId());
            pb.setGiangvienid(reviewer);
            em.persist(pb);

            Diem d = new Diem();
            d.setDangkydetaiid(dk.getId());
            d.setGiangvienid(reviewer);
            d.setDiemso(new BigDecimal("8.50"));
            em.persist(d);

            return pb.getId();
        });

        assertThatThrownBy(() -> giangvienphanbienService.deleteById(pbId))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("đã nhập điểm");
    }

    @Test
    @DisplayName("P0-02 FAIL: Chặn xóa phân công GVPB khi kết quả đề tài đã được công bố")
    void testXoaPhancong_ThatBai_KhiKetquaDaCongbo() {
        Long pbId = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.KLTN);
            Long guide = createLecturer("gvhd-p02-pub");
            Long reviewer = createLecturer("gvpb-p02-pub");
            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài P0-02 Pub");
            dt.setGiangviendexuatid(guide);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P0-02 Pub");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            Giangvienphanbien pb = new Giangvienphanbien();
            pb.setDangkydetaiid(dk.getId());
            pb.setGiangvienid(reviewer);
            em.persist(pb);

            Ketqua kq = new Ketqua();
            kq.setDangkydetaiid(dk.getId());
            kq.setDiemcuoi(new BigDecimal("9.00"));
            kq.setDacongbo(true);
            kq.setNgaycongbo(LocalDateTime.now());
            em.persist(kq);

            return pb.getId();
        });

        assertThatThrownBy(() -> giangvienphanbienService.deleteById(pbId))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã công bố");
    }

    // =========================================================================
    // P0-03: LUỒNG HOÀN CHỈNH: DUYỆT -> PHÂN CÔNG -> CHẤM ĐIỂM -> TỔNG HỢP -> CÔNG BỐ
    // =========================================================================

    @Test
    @DisplayName("P0-03 PASS: Toàn bộ chu trình từ đăng ký, phân công, chấm điểm (0-10, HALF_UP) đến công bố tạo outbox")
    void testEndToEnd_PhanCong_NhapDiem_CongBo_Outbox() {
        record FlowData(Long regId, Long councilId, Long gv1, Long gv2, Long gv3, String sv1User) {}

        FlowData flow = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.KLTN);
            Long guide = createLecturer("gvhd-p03");
            Long c1 = createLecturer("gv331");
            Long c2 = createLecturer("gv332");
            Long c3 = createLecturer("gv333");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Nghiên cứu Deep Learning P03");
            dt.setGiangviendexuatid(guide);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan supervision = new Giangvienhuongdan();
            supervision.setDetaiid(dt.getId());
            supervision.setGiangvienid(guide);
            em.persist(supervision);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm AI Pro");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Sinhvien s1 = createStudent("sv-p03-1");
            Sinhvien s2 = createStudent("sv-p03-2");
            Sinhvien s3 = createStudent("sv-p03-3");
            grp.setNhomtruongid(s1.getId());

            Thanhviennhom m1 = new Thanhviennhom();
            m1.setNhomid(grp.getId());
            m1.setSinhvienid(s1.getId());
            em.persist(m1);

            Thanhviennhom m2 = new Thanhviennhom();
            m2.setNhomid(grp.getId());
            m2.setSinhvienid(s2.getId());
            em.persist(m2);

            Thanhviennhom m3 = new Thanhviennhom();
            m3.setNhomid(grp.getId());
            m3.setSinhvienid(s3.getId());
            em.persist(m3);

            Hoidong hd = new Hoidong();
            hd.setDotdangkyid(round.getId());
            hd.setTenhoidong("Hội đồng KLTN 01");
            hd.setPhong("P.101");
            em.persist(hd);

            Thanhvienhoidong tv1 = new Thanhvienhoidong();
            tv1.setHoidongid(hd.getId());
            tv1.setGiangvienid(c1);
            tv1.setVaitro(VaitroHoidong.CHUTICH);
            em.persist(tv1);

            Thanhvienhoidong tv2 = new Thanhvienhoidong();
            tv2.setHoidongid(hd.getId());
            tv2.setGiangvienid(c2);
            tv2.setVaitro(VaitroHoidong.THUKY);
            em.persist(tv2);

            Thanhvienhoidong tv3 = new Thanhvienhoidong();
            tv3.setHoidongid(hd.getId());
            tv3.setGiangvienid(c3);
            tv3.setVaitro(VaitroHoidong.THANHVIEN);
            em.persist(tv3);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            Giangvienphanbien pb = new Giangvienphanbien();
            pb.setDangkydetaiid(dk.getId());
            pb.setGiangvienid(c3);
            em.persist(pb);

            return new FlowData(dk.getId(), hd.getId(), c1, c2, c3, "sv-p03-1");
        });

        // 1. Duyệt đề tài -> tự động sinh Ketqua trung gian
        dangkydetaiService.duyetDangky(flow.regId(), true, "chief");
        Ketqua kq = ketquaRepository.findByDangkydetaiid(flow.regId()).orElseThrow();
        assertThat(kq.getDacongbo()).isFalse();

        // 2. Phân công hội đồng
        ketquaService.phanCongHoiDong(kq.getId(), HinhthucDanhgia.COHOIDONG, flow.councilId());

        // 3. 3 Giảng viên hội đồng chấm điểm: 8.5, 9.0, 8.8 -> Trung bình: 8.7666... -> HALF_UP: 8.77
        authenticate("gv331", "GIANGVIEN");
        diemService.nhapDiem(new DiemFormDto(flow.regId(), flow.gv1(), new BigDecimal("8.5"), "Khá"));

        authenticate("gv332", "GIANGVIEN");
        diemService.nhapDiem(new DiemFormDto(flow.regId(), flow.gv2(), new BigDecimal("9.0"), "Tốt"));

        authenticate("gv333", "GIANGVIEN");
        diemService.nhapDiem(new DiemFormDto(flow.regId(), flow.gv3(), new BigDecimal("8.8"), "Xuất sắc"));

        // Kiểm tra điểm trung gian được cập nhật tự động và làm tròn HALF_UP
        Ketqua kqCapNhat = ketquaRepository.findByDangkydetaiid(flow.regId()).orElseThrow();
        assertThat(kqCapNhat.getDiemcuoi()).isEqualByComparingTo(new BigDecimal("8.77"));

        // 4. Trưởng khoa công bố kết quả
        authenticate("chief", "TRUONGKHOA");
        Ketqua published = ketquaService.congboKetqua(kq.getId());
        assertThat(published.getDacongbo()).isTrue();
        assertThat(published.getNgaycongbo()).isNotNull();

        // 5. Kiểm tra email outbox Mailjet: Phải có 2 bản ghi email cho 2 sinh viên trong nhóm với trạng thái PENDING
        List<Emailketqua> outbox = emailketquaRepository.findByKetquaidOrderByIdAsc(kq.getId());
        assertThat(outbox).hasSize(3);
        assertThat(outbox).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.PENDING);
        assertThat(outbox.stream().map(Emailketqua::getEmail).toList())
                .containsExactlyInAnyOrder("sv-p03-1@student.edu.vn", "sv-p03-2@student.edu.vn", "sv-p03-3@student.edu.vn");

        // 6. Kiểm tra sinh viên tra cứu: chỉ thấy kết quả đã công bố của nhóm mình
        authenticate(flow.sv1User(), "SINHVIEN");
        List<Ketqua> svResults = ketquaService.findVisibleToCurrentUser();
        assertThat(svResults).hasSize(1);
        assertThat(svResults.get(0).getId()).isEqualTo(kq.getId());
        assertThat(svResults.get(0).getDiemcuoi()).isEqualByComparingTo(new BigDecimal("8.77"));
    }
}
