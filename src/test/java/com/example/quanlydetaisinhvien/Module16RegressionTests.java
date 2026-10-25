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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:m16-regression;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Module16RegressionTests {

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
    @Autowired private NghiepvuDetaiService nghiepvuDetaiService;

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
        r.setTendot("Đợt kiểm thử " + type);
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
    // ĐIỀU KIỆN 1: Đăng ký chưa được duyệt không được nhập điểm (kể cả khi Ketqua chưa tồn tại)
    // =========================================================================
    @Test
    @DisplayName("Điều kiện 1: Đăng ký chưa được duyệt không được nhập điểm qua nghiepvuDetaiService hoặc diemService")
    void test1_dangKyChuaDuyet_khongDuocNhapDiem() {
        Long[] ids = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("gvhd-c1");
            Long gvCham = createLecturer("gvcham-c1");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Điều kiện 1");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm Điều kiện 1");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return new Long[]{dk.getId(), gvCham};
        });

        Long regId = ids[0];
        Long gvCham = ids[1];

        // 1. Thử qua diemService.nhapDiem
        DiemFormDto form = new DiemFormDto();
        form.setDangkydetaiid(regId);
        form.setGiangvienid(gvCham);
        form.setDiemso(new BigDecimal("8.50"));
        form.setNhanxet("Chấm thử khi chưa duyệt");

        assertThatThrownBy(() -> diemService.nhapDiem(form))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Đăng ký đề tài chưa được duyệt");

        // 2. Thử trực tiếp qua nghiepvuDetaiService.chamDiem
        assertThatThrownBy(() -> nghiepvuDetaiService.chamDiem(regId, gvCham, new BigDecimal("8.50"), "Test"))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Đăng ký đề tài chưa được duyệt");

        // Xác nhận chưa có kết quả và chưa có điểm
        assertThat(ketquaRepository.findByDangkydetaiid(regId)).isEmpty();
    }

    // =========================================================================
    // ĐIỀU KIỆN 2: Duyệt đăng ký đồng thời không tạo 2 bản ghi Ketqua
    // =========================================================================
    @Test
    @DisplayName("Điều kiện 2: Duyệt đăng ký đồng thời hoặc lặp lại không tạo 2 bản ghi Ketqua")
    void test2_duyetDangKy_khongTaoHaiBanGhiKetqua() {
        Long regId = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("gvhd-c2");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Điều kiện 2");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Sinhvien sv1 = createStudent("sv-c2-1");
            Sinhvien sv2 = createStudent("sv-c2-2");
            Sinhvien sv3 = createStudent("sv-c2-3");

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm Điều kiện 2");
            grp.setNhomtruongid(sv1.getId());
            em.persist(grp);

            for (Sinhvien s : List.of(sv1, sv2, sv3)) {
                Thanhviennhom tv = new Thanhviennhom();
                tv.setNhomid(grp.getId());
                tv.setSinhvienid(s.getId());
                em.persist(tv);
            }

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return dk.getId();
        });

        // Lần 1: Duyệt thành công
        dangkydetaiService.duyetDangky(regId, true, "chief");
        assertThat(ketquaRepository.findByDangkydetaiid(regId)).isPresent();

        // Lần 2: Nếu gọi lại (giả lập request lặp lại hoặc đồng thời đã xử lý)
        assertThatThrownBy(() -> dangkydetaiService.duyetDangky(regId, true, "chief"))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Chỉ xử lý đăng ký đang chờ duyệt");

        // Xác nhận trong DB chỉ có duy nhất 1 bản ghi Ketqua cho đăng ký này
        long count = ketquaRepository.findAll().stream()
                .filter(k -> k.getDangkydetaiid().equals(regId))
                .count();
        assertThat(count).isEqualTo(1);
    }

    // =========================================================================
    // ĐIỀU KIỆN 3 & 4: Nhóm sinh viên hợp lệ có 3–5 người, khi công bố mỗi SV nhận đúng 1 Email Outbox không trùng
    // =========================================================================
    @Test
    @DisplayName("Điều kiện 3 & 4: Nhóm 4 sinh viên khi công bố tạo đúng 4 bản ghi Email Outbox PENDING, không trùng lặp")
    void test3_nhom4SinhVien_congBoTaoDung4EmailOutbox() {
        Long[] setupIds = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("gvhd-c3");
            Long gvpb = createLecturer("gvpb-c3");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Điều kiện 3 và 4");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(dt.getId());
            hd.setGiangvienid(gvhd);
            em.persist(hd);

            // Nhóm có đúng 4 sinh viên hợp lệ (nằm trong khoảng 3-5)
            Sinhvien sv1 = createStudent("sv-c3-1");
            Sinhvien sv2 = createStudent("sv-c3-2");
            Sinhvien sv3 = createStudent("sv-c3-3");
            Sinhvien sv4 = createStudent("sv-c3-4");

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm Điều kiện 3 và 4");
            grp.setNhomtruongid(sv1.getId());
            em.persist(grp);

            for (Sinhvien s : List.of(sv1, sv2, sv3, sv4)) {
                Thanhviennhom tv = new Thanhviennhom();
                tv.setNhomid(grp.getId());
                tv.setSinhvienid(s.getId());
                em.persist(tv);
            }

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return new Long[]{dk.getId(), gvpb};
        });

        Long regId = setupIds[0];
        Long gvpb = setupIds[1];

        // Duyệt đề tài
        dangkydetaiService.duyetDangky(regId, true, "chief");

        // Phân công phản biện
        PhanbienFormDto pbForm = new PhanbienFormDto();
        pbForm.setDangkydetaiid(regId);
        pbForm.setGiangvienid(gvpb);
        giangvienphanbienService.phanCong(pbForm);

        // Giảng viên phản biện nhập điểm
        DiemFormDto dForm = new DiemFormDto();
        dForm.setDangkydetaiid(regId);
        dForm.setGiangvienid(gvpb);
        dForm.setDiemso(new BigDecimal("8.75"));
        dForm.setNhanxet("Báo cáo xuất sắc");
        authenticate("gvpb-c3", "GIANGVIEN");
        diemService.nhapDiem(dForm);
        authenticate("chief", "TRUONGKHOA");

        // Lấy Ketqua
        Ketqua kq = ketquaRepository.findByDangkydetaiid(regId).orElseThrow();
        assertThat(kq.getDiemcuoi()).isEqualByComparingTo("8.75");

        // Trưởng khoa phân công hình thức đánh giá (TLCN không bắt buộc hội đồng)
        ketquaService.phanCongHoiDong(kq.getId(), HinhthucDanhgia.KHONGHOIDONG, null);

        // Trưởng khoa công bố kết quả
        Ketqua published = ketquaService.congboKetqua(kq.getId());
        assertThat(published.getDacongbo()).isTrue();

        // Kiểm tra Email Outbox: đúng 4 bản ghi cho 4 sinh viên trong nhóm
        List<Emailketqua> outbox = emailketquaRepository.findByKetquaidOrderByIdAsc(kq.getId());
        assertThat(outbox).hasSize(4);
        assertThat(outbox).allMatch(e -> e.getTrangthai() == TrangthaiEmailketqua.PENDING);
        assertThat(outbox).extracting(Emailketqua::getSinhvienid).doesNotHaveDuplicates();
    }

    // =========================================================================
    // ĐIỀU KIỆN 5: Tính lại tất cả kết quả không thay đổi kết quả đã công bố
    // =========================================================================
    @Test
    @DisplayName("Điều kiện 5: Nút 'Tính lại tất cả kết quả' và gọi endpoint tính lại trực tiếp không được sửa kết quả đã công bố")
    void test4_tinhLaiTatCa_khongThayDoiKetQuaDaCongBo() {
        Long[] setupIds = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("gvhd-c5");
            Long gvpb = createLecturer("gvpb-c5");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Điều kiện 5");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(dt.getId());
            hd.setGiangvienid(gvhd);
            em.persist(hd);

            Sinhvien sv1 = createStudent("sv-c5-1");
            Sinhvien sv2 = createStudent("sv-c5-2");
            Sinhvien sv3 = createStudent("sv-c5-3");

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm Điều kiện 5");
            grp.setNhomtruongid(sv1.getId());
            em.persist(grp);

            for (Sinhvien s : List.of(sv1, sv2, sv3)) {
                Thanhviennhom tv = new Thanhviennhom();
                tv.setNhomid(grp.getId());
                tv.setSinhvienid(s.getId());
                em.persist(tv);
            }

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return new Long[]{dk.getId(), gvpb};
        });

        Long regId = setupIds[0];
        Long gvpb = setupIds[1];

        dangkydetaiService.duyetDangky(regId, true, "chief");

        PhanbienFormDto pbForm = new PhanbienFormDto();
        pbForm.setDangkydetaiid(regId);
        pbForm.setGiangvienid(gvpb);
        giangvienphanbienService.phanCong(pbForm);

        DiemFormDto dForm = new DiemFormDto();
        dForm.setDangkydetaiid(regId);
        dForm.setGiangvienid(gvpb);
        dForm.setDiemso(new BigDecimal("9.00"));
        authenticate("gvpb-c5", "GIANGVIEN");
        diemService.nhapDiem(dForm);
        authenticate("chief", "TRUONGKHOA");

        Ketqua kq = ketquaRepository.findByDangkydetaiid(regId).orElseThrow();
        ketquaService.phanCongHoiDong(kq.getId(), HinhthucDanhgia.KHONGHOIDONG, null);
        Ketqua published = ketquaService.congboKetqua(kq.getId());

        BigDecimal diemGoc = published.getDiemcuoi();
        LocalDateTime ngayCongBoGoc = published.getNgaycongbo();
        assertThat(diemGoc).isEqualByComparingTo("9.00");
        assertThat(published.getDacongbo()).isTrue();

        // 1. Gọi endpoint / service trực tiếp tinhLaiKetQua(regId) -> Bị chặn với lỗi
        assertThatThrownBy(() -> ketquaService.tinhLaiKetQua(regId))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("không được tính lại");

        // 2. Gọi nút "Tính lại tất cả kết quả" (tinhLaiTatCaKetQua) -> Phải bỏ qua kết quả đã công bố
        ketquaService.tinhLaiTatCaKetQua();

        // 3. Xác minh kết quả không bị thay đổi điểm hoặc ngày công bố
        Ketqua afterRecalc = ketquaRepository.findById(published.getId()).orElseThrow();
        assertThat(afterRecalc.getDiemcuoi()).isEqualByComparingTo(diemGoc);
        assertThat(afterRecalc.getNgaycongbo()).isEqualTo(ngayCongBoGoc);
        assertThat(afterRecalc.getDacongbo()).isTrue();
    }

    // =========================================================================
    // ĐIỀU KIỆN 6: Bảo toàn điểm, phân công và dữ liệu kết quả đã tồn tại
    // =========================================================================
    @Test
    @DisplayName("Điều kiện 6: Bảo toàn điểm và phân công sau khi công bố kết quả (chặn sửa, chặn xóa)")
    void test5_baoToanDiemVaPhanCong_sauKhiCongBo() {
        Long[] setupIds = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("gvhd-c6");
            Long gvpb = createLecturer("gvpb-c6");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Điều kiện 6");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(dt.getId());
            hd.setGiangvienid(gvhd);
            em.persist(hd);

            Sinhvien sv1 = createStudent("sv-c6-1");
            Sinhvien sv2 = createStudent("sv-c6-2");
            Sinhvien sv3 = createStudent("sv-c6-3");

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm Điều kiện 6");
            grp.setNhomtruongid(sv1.getId());
            em.persist(grp);

            for (Sinhvien s : List.of(sv1, sv2, sv3)) {
                Thanhviennhom tv = new Thanhviennhom();
                tv.setNhomid(grp.getId());
                tv.setSinhvienid(s.getId());
                em.persist(tv);
            }

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return new Long[]{dk.getId(), gvpb};
        });

        Long regId = setupIds[0];
        Long gvpb = setupIds[1];

        dangkydetaiService.duyetDangky(regId, true, "chief");

        PhanbienFormDto pbForm = new PhanbienFormDto();
        pbForm.setDangkydetaiid(regId);
        pbForm.setGiangvienid(gvpb);
        Giangvienphanbien pbSaved = giangvienphanbienService.phanCong(pbForm);
        Long pbId = pbSaved.getId();

        DiemFormDto dForm = new DiemFormDto();
        dForm.setDangkydetaiid(regId);
        dForm.setGiangvienid(gvpb);
        dForm.setDiemso(new BigDecimal("9.20"));
        authenticate("gvpb-c6", "GIANGVIEN");
        Diem diemSaved = diemService.nhapDiem(dForm);
        authenticate("chief", "TRUONGKHOA");

        Ketqua kq = ketquaRepository.findByDangkydetaiid(regId).orElseThrow();
        ketquaService.phanCongHoiDong(kq.getId(), HinhthucDanhgia.KHONGHOIDONG, null);
        ketquaService.congboKetqua(kq.getId());

        // 1. Thử xóa điểm sau khi công bố -> Bị chặn
        assertThatThrownBy(() -> diemService.xoaDiem(diemSaved.getId()))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã công bố");

        // 2. Thử xóa phân công phản biện sau khi công bố -> Bị chặn
        assertThatThrownBy(() -> giangvienphanbienService.deleteById(pbId))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã công bố");

        // 3. Thử phân công thêm phản biện sau khi công bố -> Bị chặn
        Long gvKhac = tx.execute(st -> createLecturer("gvpb-c6-extra"));
        PhanbienFormDto extraForm = new PhanbienFormDto();
        extraForm.setDangkydetaiid(regId);
        extraForm.setGiangvienid(gvKhac);
        assertThatThrownBy(() -> giangvienphanbienService.phanCong(extraForm))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã công bố");

        // 4. Thử công bố lại -> Bị chặn và không sinh email trùng
        assertThatThrownBy(() -> ketquaService.congboKetqua(kq.getId()))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã được công bố");

        List<Emailketqua> emails = emailketquaRepository.findByKetquaidOrderByIdAsc(kq.getId());
        assertThat(emails).hasSize(3);
    }

    // =========================================================================
    // P0: LỖ HỔNG PHÂN QUYỀN NHẬP ĐIỂM (ANTI-IMPERSONATION / CHỐNG CHẤM THAY)
    // =========================================================================
    @Test
    @DisplayName("P0: Đăng nhập m16-reviewer nhưng request gửi giangvienid của m16-chair -> BẮT BUỘC BỊ TỪ CHỐI")
    void testP0_chanChamThay_m16Reviewer_chamBangId_m16Chair_biTuChoi() {
        Long[] ids = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("p0-guide");
            Long reviewer = createLecturer("m16-reviewer-test");
            Long chair = createLecturer("m16-chair-test");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Kiểm thử P0 Chấm thay");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(dt.getId());
            hd.setGiangvienid(gvhd);
            em.persist(hd);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P0");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return new Long[]{dk.getId(), reviewer, chair};
        });

        Long regId = ids[0];
        Long reviewerId = ids[1];
        Long chairId = ids[2];

        // Phân công phản biện cho reviewer
        PhanbienFormDto pbForm = new PhanbienFormDto();
        pbForm.setDangkydetaiid(regId);
        pbForm.setGiangvienid(reviewerId);
        giangvienphanbienService.phanCong(pbForm);

        // Đăng nhập tài khoản m16-reviewer-test
        authenticate("m16-reviewer-test", "GIANGVIEN");

        // Client cố tình gửi giangvienid = chairId (ID của giảng viên khác)
        DiemFormDto spoofedForm = new DiemFormDto();
        spoofedForm.setDangkydetaiid(regId);
        spoofedForm.setGiangvienid(chairId); // Impersonating chair
        spoofedForm.setDiemso(new BigDecimal("8.75"));
        spoofedForm.setNhanxet("Cố tình chấm thay m16-chair");

        // BẮT BUỘC BỊ TỪ CHỐI trước khi ghi vào database
        assertThatThrownBy(() -> diemService.nhapDiem(spoofedForm))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("chỉ được nhập điểm cho chính mình");

        // Xác nhận không có bản ghi điểm nào được tạo trong DB
        authenticate("chief", "TRUONGKHOA");
        assertThat(dangkydetaiRepository.findById(regId)).isPresent();
        assertThat(diemService.findAll().stream().noneMatch(d -> d.getDangkydetaiid().equals(regId))).isTrue();
    }

    @Test
    @DisplayName("P0: Giảng viên chưa được phân công chấm đề tài -> BỊ TỪ CHỐI")
    void testP0_giangVienKhongDuocPhanCong_biTuChoi() {
        Long[] ids = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("p0-guide-2");
            Long reviewer = createLecturer("p0-reviewer-2");
            Long outsider = createLecturer("p0-outsider");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Phân công P0");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(dt.getId());
            hd.setGiangvienid(gvhd);
            em.persist(hd);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P0-2");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return new Long[]{dk.getId(), reviewer, outsider};
        });

        Long regId = ids[0];
        Long reviewerId = ids[1];
        Long outsiderId = ids[2];

        // Chỉ phân công reviewer
        PhanbienFormDto pbForm = new PhanbienFormDto();
        pbForm.setDangkydetaiid(regId);
        pbForm.setGiangvienid(reviewerId);
        giangvienphanbienService.phanCong(pbForm);

        // Đăng nhập là outsider (không được phân công)
        authenticate("p0-outsider", "GIANGVIEN");

        DiemFormDto form = new DiemFormDto();
        form.setDangkydetaiid(regId);
        form.setGiangvienid(outsiderId);
        form.setDiemso(new BigDecimal("8.00"));
        form.setNhanxet("Chấm khi chưa phân công");

        assertThatThrownBy(() -> diemService.nhapDiem(form))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("chưa được phân công");
    }

    // =========================================================================
    // P1: ĐỒNG BỘ DANH SÁCH ĐỀ TÀI CHỜ CHẤM VÀ BIẾN MẤT SAU KHI CHẤM
    // =========================================================================
    @Test
    @DisplayName("P1: Giảng viên thấy đúng đề tài được phân công phản biện chờ chấm, sau khi chấm biến mất khỏi chờ chấm")
    void testP1_dongBoDanhSachDeTaiChoCham_vaBienMatSauKhiCham() {
        Long[] ids = tx.execute(status -> {
            Dotdangky round = createRound(Loaidot.TLCN);
            Long gvhd = createLecturer("p1-guide");
            Long reviewer = createLecturer("p1-reviewer");

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Đồng bộ P1");
            dt.setGiangviendexuatid(gvhd);
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(dt.getId());
            hd.setGiangvienid(gvhd);
            em.persist(hd);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm P1");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);

            return new Long[]{dk.getId(), reviewer};
        });

        Long regId = ids[0];
        Long reviewerId = ids[1];

        // 1. Phân công phản biện cho reviewer
        PhanbienFormDto pbForm = new PhanbienFormDto();
        pbForm.setDangkydetaiid(regId);
        pbForm.setGiangvienid(reviewerId);
        giangvienphanbienService.phanCong(pbForm);

        // 2. Đăng nhập tài khoản reviewer và kiểm tra danh sách chờ chấm
        authenticate("p1-reviewer", "GIANGVIEN");
        var gradeOptionsBefore = diemService.availableGradeOptions();
        assertThat(gradeOptionsBefore.topics()).anyMatch(t -> t.registrationId().equals(regId));

        // 3. Reviewer chấm điểm hợp lệ
        DiemFormDto dForm = new DiemFormDto();
        dForm.setDangkydetaiid(regId);
        dForm.setGiangvienid(reviewerId);
        dForm.setDiemso(new BigDecimal("8.50"));
        dForm.setNhanxet("Đã chấm điểm hoàn tất");
        diemService.nhapDiem(dForm);

        // 4. Sau khi chấm: Đề tài biến mất khỏi danh sách chờ chấm
        var gradeOptionsAfter = diemService.availableGradeOptions();
        assertThat(gradeOptionsAfter.topics()).noneMatch(t -> t.registrationId().equals(regId));

        // 5. Đề tài xuất hiện trong danh sách đã chấm của reviewer
        var myScores = diemService.findAll();
        assertThat(myScores).anyMatch(d -> d.getDangkydetaiid().equals(regId) && d.getGiangvienid().equals(reviewerId));
    }
}
