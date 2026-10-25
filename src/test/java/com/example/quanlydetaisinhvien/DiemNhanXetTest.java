package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * STT 14 (P1) – Điểm và nhận xét
 *
 * Bao gồm toàn bộ các ca kiểm thử theo yêu cầu bảng phân công:
 *   - Giá trị biên: 0 (hợp lệ), 10 (hợp lệ), -0.01 (bị chặn), 10.01 (bị chặn)
 *   - Điểm null: bị chặn
 *   - Nhận xét null: được phép (nullable)
 *   - Giảng viên không được phân công cố nhập điểm: bị chặn AccessDeniedException
 *   - Giảng viên khác cố sửa điểm người khác: bị chặn
 *   - Cố sửa/xóa điểm sau khi kết quả đã công bố: bị chặn
 *   - Quyền trả lỗi đúng: TRUONGKHOA có thể xóa mọi điểm, GIANGVIEN chỉ xóa điểm của mình
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:diem-nhanxet-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
@Transactional
class DiemNhanXetTest {

    @Autowired private DiemService diemService;

    @Autowired private BomonRepository bomonRepo;
    @Autowired private TaikhoanRepository taikhoanRepo;
    @Autowired private GiangvienRepository giangvienRepo;
    @Autowired private SinhvienRepository sinhvienRepo;
    @Autowired private DotdangkyRepository dotdangkyRepo;
    @Autowired private DetaiRepository detaiRepo;
    @Autowired private GiangvienhuongdanRepository gvhdRepo;
    @Autowired private NhomsinhvienRepository nhomRepo;
    @Autowired private DangkydetaiRepository dangkyRepo;
    @Autowired private KetquaRepository ketquaRepo;
    @Autowired private PhanconghoidongRepository pchdRepo;
    @Autowired private DiemRepository diemRepo;
    @Autowired private GiangvienphanbienRepository gvpbRepo;

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    /* Shared fixtures */
    private Giangvien gvpb;      // GVPB – được phân công chấm
    private Giangvien gvhd;      // GVHD – không được tự chấm
    private Giangvien gvOther;   // GV khác – không phân công
    private Dangkydetai dangky;
    private Ketqua ketqua;

    private Giangvien newGv(Vaitro vaitro, Bomon bm) {
        int n = SEQ.incrementAndGet();
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap("gv_n" + n);
        tk.setMatkhau("x");
        tk.setHoten("GV N" + n);
        tk.setVaitro(vaitro);
        tk.setKichhoat(true);
        tk = taikhoanRepo.save(tk);
        Giangvien gv = new Giangvien();
        gv.setTaikhoanid(tk.getId());
        gv.setMagiangvien("N_GV" + n);
        gv.setBomonid(bm.getId());
        return giangvienRepo.save(gv);
    }

    private void setAuth(Giangvien gv) {
        var tk = taikhoanRepo.findById(gv.getTaikhoanid()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tk.getTendangnhap(), "x",
                        List.of(new SimpleGrantedAuthority("ROLE_" + tk.getVaitro().name()))));
    }

    private void setAuthChief(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x",
                        List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
    }

    @BeforeEach
    void setUp() {
        int n = SEQ.incrementAndGet();

        // Tạo Trưởng khoa tài khoản
        Taikhoan tkChief = new Taikhoan();
        tkChief.setTendangnhap("chief_n" + n);
        tkChief.setMatkhau("x");
        tkChief.setHoten("TK N" + n);
        tkChief.setVaitro(Vaitro.TRUONGKHOA);
        tkChief.setKichhoat(true);
        taikhoanRepo.save(tkChief);
        setAuthChief("chief_n" + n);

        Bomon bm = new Bomon();
        bm.setMabomon("BM_N" + n);
        bm.setTenbomon("BM Diem Test");
        bm = bomonRepo.save(bm);

        gvhd = newGv(Vaitro.GIANGVIEN, bm);
        gvpb = newGv(Vaitro.GIANGVIEN, bm);
        gvOther = newGv(Vaitro.GIANGVIEN, bm);

        Dotdangky dot = new Dotdangky();
        dot.setTendot("Dot TLCN N" + n);
        dot.setLoaidot(Loaidot.TLCN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(10));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        dot = dotdangkyRepo.save(dot);

        Detai detai = new Detai();
        detai.setDotdangkyid(dot.getId());
        detai.setBomonid(bm.getId());
        detai.setTendetai("De tai Diem Test " + n);
        detai.setGiangviendexuatid(gvhd.getId());
        detai.setTrangthai(TrangthaiDetai.CONGBO);
        detai = detaiRepo.save(detai);

        Giangvienhuongdan hd = new Giangvienhuongdan();
        hd.setDetaiid(detai.getId());
        hd.setGiangvienid(gvhd.getId());
        gvhdRepo.save(hd);

        Taikhoan tkSv = new Taikhoan();
        tkSv.setTendangnhap("sv_n" + n);
        tkSv.setMatkhau("x");
        tkSv.setHoten("SV N" + n);
        tkSv.setVaitro(Vaitro.SINHVIEN);
        tkSv.setKichhoat(true);
        tkSv = taikhoanRepo.save(tkSv);

        Sinhvien sv = new Sinhvien();
        sv.setTaikhoanid(tkSv.getId());
        sv.setMasinhvien("SV_N" + n);
        sv.setBomonid(bm.getId());
        sv = sinhvienRepo.save(sv);

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv.getId());
        nhom.setTennhom("Nhom N" + n);
        nhom = nhomRepo.save(nhom);

        dangky = new Dangkydetai();
        dangky.setNhomid(nhom.getId());
        dangky.setDetaiid(detai.getId());
        dangky.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dangky.setNgaydangky(LocalDateTime.now());
        dangky = dangkyRepo.save(dangky);

        // Phân công hình thức KHONGHOIDONG (TLCN)
        Phanconghoidong pchd = new Phanconghoidong();
        pchd.setDangkydetaiid(dangky.getId());
        pchd.setHinhthuc(HinhthucDanhgia.KHONGHOIDONG);
        pchd.setHoidongid(null);
        pchdRepo.save(pchd);

        // Phân công GVPB
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dangky.getId());
        pb.setGiangvienid(gvpb.getId());
        gvpbRepo.save(pb);

        // Kết quả trung gian
        ketqua = new Ketqua();
        ketqua.setDangkydetaiid(dangky.getId());
        ketqua.setDacongbo(false);
        ketqua = ketquaRepo.save(ketqua);
    }

    /* ─────────────────────────────────────────────────
     * NHÓM 1: Giá trị biên điểm số
     * ───────────────────────────────────────────────── */

    @Test
    @DisplayName("STT14-01: Điểm 0.00 (biên dưới) – hợp lệ")
    void scoreZeroIsValid() {
        setAuth(gvpb);
        Diem d = makeDiem(dangky.getId(), gvpb.getId(), "0.00", "Mức tối thiểu");
        Diem saved = diemService.save(d);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDiemso()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("STT14-02: Điểm 10.00 (biên trên) – hợp lệ")
    void scoreTenIsValid() {
        setAuth(gvpb);
        Diem d = makeDiem(dangky.getId(), gvpb.getId(), "10.00", "Điểm tuyệt đối");
        Diem saved = diemService.save(d);
        assertThat(saved.getDiemso()).isEqualByComparingTo("10.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "10.01", "10.10", "11.00", "-1.00"})
    @DisplayName("STT14-03: Điểm ngoài khoảng [0,10] – bị chặn")
    void outOfRangeScoreRejected(String score) {
        setAuth(gvpb);
        Diem d = makeDiem(dangky.getId(), gvpb.getId(), score, "Ngoài biên");
        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> diemService.save(d));
        assertThat(ex.getMessage()).contains("0–10");
    }

    @Test
    @DisplayName("STT14-04: Điểm null – bị chặn")
    void nullScoreRejected() {
        setAuth(gvpb);
        Diem d = new Diem();
        d.setDangkydetaiid(dangky.getId());
        d.setGiangvienid(gvpb.getId());
        d.setDiemso(null);
        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> diemService.save(d));
        assertThat(ex.getMessage()).contains("0–10");
    }

    @Test
    @DisplayName("STT14-05: Nhận xét null – được phép (không bắt buộc)")
    void nullNhanxetAllowed() {
        setAuth(gvpb);
        Diem d = makeDiem(dangky.getId(), gvpb.getId(), "8.00", null);
        Diem saved = diemService.save(d);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNhanxet()).isNull();
    }

    /* ─────────────────────────────────────────────────
     * NHÓM 2: Phân quyền nhập điểm
     * ───────────────────────────────────────────────── */

    @Test
    @DisplayName("STT14-06: GVHD tự chấm đề tài mình hướng dẫn – bị chặn")
    void gvhdCannotGradeOwnTopic() {
        setAuth(gvhd);
        Diem d = makeDiem(dangky.getId(), gvhd.getId(), "8.00", "GV tự chấm");
        // GVHD không nằm trong danh sách được phân công (gvpb mới là GVPB)
        // -> AccessDeniedException vì chưa được phân công
        assertThrows(AccessDeniedException.class, () -> diemService.save(d));
    }

    @Test
    @DisplayName("STT14-07: GV không được phân công cố nhập điểm – bị chặn AccessDeniedException")
    void unpermittedGvCannotGrade() {
        setAuth(gvOther);
        Diem d = makeDiem(dangky.getId(), gvOther.getId(), "7.50", "GV khác chấm");
        assertThrows(AccessDeniedException.class, () -> diemService.save(d));
    }

    @Test
    @DisplayName("STT14-08: GV nhập điểm cho người khác (sai giangvienid) – bị chặn")
    void gvCannotInputScoreForOtherGv() {
        setAuth(gvpb);
        // gvpb đăng nhập nhưng cố ghi điểm với id của gvOther
        Diem d = makeDiem(dangky.getId(), gvOther.getId(), "8.00", "GV nhập sai");
        assertThrows(AccessDeniedException.class, () -> diemService.save(d));
    }

    /* ─────────────────────────────────────────────────
     * NHÓM 3: Sửa/xóa sau công bố
     * ───────────────────────────────────────────────── */

    @Test
    @DisplayName("STT14-09: Cố sửa điểm sau khi kết quả đã công bố – bị chặn")
    void editScoreAfterPublicationRejected() {
        // Nhập điểm ban đầu
        setAuth(gvpb);
        Diem saved = diemService.save(makeDiem(dangky.getId(), gvpb.getId(), "8.00", "Lần 1"));

        // Đánh dấu công bố
        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketqua.setDacongbo(true);
        ketqua.setNgaycongbo(LocalDateTime.now());
        ketquaRepo.save(ketqua);

        // Cố sửa điểm
        setAuth(gvpb);
        Diem update = makeDiem(dangky.getId(), gvpb.getId(), "9.00", "Cố sửa");
        update.setId(saved.getId());
        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> diemService.save(update));
        assertThat(ex.getMessage()).contains("công bố");
    }

    @Test
    @DisplayName("STT14-10: GV tự xóa điểm của mình – được phép trước công bố")
    void gvCanDeleteOwnScoreBeforePublication() {
        setAuth(gvpb);
        Diem saved = diemService.save(makeDiem(dangky.getId(), gvpb.getId(), "8.00", "Xóa thử"));
        assertDoesNotThrow(() -> diemService.deleteById(saved.getId()));
        assertThat(diemRepo.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("STT14-11: GV cố xóa điểm của GV khác – bị chặn")
    void gvCannotDeleteOtherGvScore() {
        // Nhập điểm bằng gvpb
        setAuth(gvpb);
        Diem saved = diemService.save(makeDiem(dangky.getId(), gvpb.getId(), "8.00", "Điểm gvpb"));

        // Thêm gvOther vào danh sách phân công để tránh lỗi phân công
        Giangvienphanbien pbOther = new Giangvienphanbien();
        pbOther.setDangkydetaiid(dangky.getId());
        pbOther.setGiangvienid(gvOther.getId());
        gvpbRepo.save(pbOther);

        // gvOther cố xóa điểm của gvpb
        setAuth(gvOther);
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> diemService.deleteById(saved.getId()));
        assertThat(ex.getMessage()).isNotEmpty();
    }

    @Test
    @DisplayName("STT14-12: Cố xóa điểm sau khi kết quả đã công bố – bị chặn")
    void deleteScoreAfterPublicationRejected() {
        setAuth(gvpb);
        Diem saved = diemService.save(makeDiem(dangky.getId(), gvpb.getId(), "8.00", "Sắp xóa"));

        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketqua.setDacongbo(true);
        ketqua.setNgaycongbo(LocalDateTime.now());
        ketquaRepo.save(ketqua);

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> diemService.deleteById(saved.getId()));
        assertThat(ex.getMessage()).contains("công bố");
    }

    @Test
    @DisplayName("STT14-13: TRUONGKHOA có thể xóa điểm của bất kỳ GV nào trước công bố")
    void chiefCanDeleteAnyScore() {
        // Nhập điểm bằng gvpb
        setAuth(gvpb);
        Diem saved = diemService.save(makeDiem(dangky.getId(), gvpb.getId(), "8.00", "Xóa bởi TK"));

        // Trưởng khoa xóa
        // Tạo tài khoản trưởng khoa hợp lệ + liên kết giảng viên
        Giangvien gvChief = createChiefGv();
        setAuth(gvChief);
        // Override với ROLE_TRUONGKHOA
        var chiefTk = taikhoanRepo.findById(gvChief.getTaikhoanid()).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(chiefTk.getTendangnhap(), "x",
                        List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));

        assertDoesNotThrow(() -> diemService.deleteById(saved.getId()));
        assertThat(diemRepo.findById(saved.getId())).isEmpty();
    }

    /* ─────────────────────────────────────────────────
     * Helpers
     * ───────────────────────────────────────────────── */

    private Diem makeDiem(Long dangkyId, Long gvId, String score, String nhanxet) {
        Diem d = new Diem();
        d.setDangkydetaiid(dangkyId);
        d.setGiangvienid(gvId);
        if (score != null) d.setDiemso(new BigDecimal(score));
        d.setNhanxet(nhanxet);
        return d;
    }

    private Giangvien createChiefGv() {
        int n = SEQ.incrementAndGet();
        Bomon bm = bomonRepo.findAll().get(0);
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap("tk_chief_del" + n);
        tk.setMatkhau("x");
        tk.setHoten("TK Chief Del " + n);
        tk.setVaitro(Vaitro.TRUONGKHOA);
        tk.setKichhoat(true);
        tk = taikhoanRepo.save(tk);
        Giangvien gv = new Giangvien();
        gv.setTaikhoanid(tk.getId());
        gv.setMagiangvien("CHIEF_DEL" + n);
        gv.setBomonid(bm.getId());
        return giangvienRepo.save(gv);
    }
}
