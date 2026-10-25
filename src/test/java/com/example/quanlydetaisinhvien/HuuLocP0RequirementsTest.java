package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:huuloc-p0-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
@Transactional
class HuuLocP0RequirementsTest {

    @Autowired private DangkydetaiService dangkydetaiService;
    @Autowired private DiemService diemService;
    @Autowired private GiangvienphanbienService giangvienphanbienService;
    @Autowired private NghiepvuDetaiService nghiepvuDetaiService;

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

    private Taikhoan tkAdmin;
    private Taikhoan tkGv1;
    private Taikhoan tkGv2;
    private Taikhoan tkSv;
    private Giangvien gv1; // GVHD
    private Giangvien gv2; // GVPB
    private Sinhvien sv;
    private Dotdangky dot;
    private Detai detai;
    private Nhomsinhvien nhom;
    private Dangkydetai dangky;

    @BeforeEach
    void setUp() {
        Bomon bm = new Bomon();
        bm.setMabomon("CNTT");
        bm.setTenbomon("Cong nghe thong tin");
        bm = bomonRepository.save(bm);

        // Trưởng khoa
        tkAdmin = new Taikhoan();
        tkAdmin.setTendangnhap("TK_ADMIN");
        tkAdmin.setMatkhau("123456");
        tkAdmin.setHoten("Trưởng Khoa");
        tkAdmin.setVaitro(Vaitro.TRUONGKHOA);
        tkAdmin.setKichhoat(true);
        tkAdmin = taikhoanRepository.save(tkAdmin);

        // GV 1 (GVHD)
        tkGv1 = new Taikhoan();
        tkGv1.setTendangnhap("GV001");
        tkGv1.setMatkhau("123456");
        tkGv1.setHoten("Giảng Viên Một");
        tkGv1.setVaitro(Vaitro.GIANGVIEN);
        tkGv1.setKichhoat(true);
        tkGv1 = taikhoanRepository.save(tkGv1);

        gv1 = new Giangvien();
        gv1.setTaikhoanid(tkGv1.getId());
        gv1.setMagiangvien("GV001");
        gv1.setBomonid(bm.getId());
        gv1 = giangvienRepository.save(gv1);

        // GV 2 (GVPB)
        tkGv2 = new Taikhoan();
        tkGv2.setTendangnhap("GV002");
        tkGv2.setMatkhau("123456");
        tkGv2.setHoten("Giảng Viên Hai");
        tkGv2.setVaitro(Vaitro.GIANGVIEN);
        tkGv2.setKichhoat(true);
        tkGv2 = taikhoanRepository.save(tkGv2);

        gv2 = new Giangvien();
        gv2.setTaikhoanid(tkGv2.getId());
        gv2.setMagiangvien("GV002");
        gv2.setBomonid(bm.getId());
        gv2 = giangvienRepository.save(gv2);

        // Sinh viên (Trưởng nhóm)
        tkSv = new Taikhoan();
        tkSv.setTendangnhap("23110001");
        tkSv.setMatkhau("123456");
        tkSv.setHoten("Nguyễn Văn A");
        tkSv.setVaitro(Vaitro.SINHVIEN);
        tkSv.setKichhoat(true);
        tkSv = taikhoanRepository.save(tkSv);

        sv = new Sinhvien();
        sv.setTaikhoanid(tkSv.getId());
        sv.setMasinhvien("23110001");
        sv.setBomonid(bm.getId());
        sv.setLop("23110CLC");
        sv = sinhvienRepository.save(sv);

        // Đợt đăng ký
        dot = new Dotdangky();
        dot.setTendot("Đợt KLTN 2026");
        dot.setLoaidot(Loaidot.KLTN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(10));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        dot = dotdangkyRepository.save(dot);

        // Đề tài
        detai = new Detai();
        detai.setDotdangkyid(dot.getId());
        detai.setBomonid(bm.getId());
        detai.setTendetai("Đề tài thử nghiệm AI");
        detai.setGiangviendexuatid(gv1.getId());
        detai.setTrangthai(TrangthaiDetai.CONGBO);
        detai = detaiRepository.save(detai);

        // Gán GV1 làm GVHD
        Giangvienhuongdan gvhd = new Giangvienhuongdan();
        gvhd.setDetaiid(detai.getId());
        gvhd.setGiangvienid(gv1.getId());
        giangvienhuongdanRepository.save(gvhd);

        // Nhóm SV
        nhom = new Nhomsinhvien();
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv.getId());
        nhom.setTennhom("Nhóm 01");
        nhom = nhomsinhvienRepository.save(nhom);

        // Đăng ký đề tài (trạng thái chờ duyệt)
        dangky = new Dangkydetai();
        dangky.setNhomid(nhom.getId());
        dangky.setDetaiid(detai.getId());
        dangky.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dangky.setNgaydangky(LocalDateTime.now());
        dangky = dangkydetaiRepository.save(dangky);

        // Set Security Context mặc định là Trưởng khoa
        setAuth("TK_ADMIN", "ROLE_TRUONGKHOA");
    }

    private void setAuth(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, "N/A", List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // 1. KIỂM THỬ YÊU CẦU 01 (STT 01 - P0): Chấm điểm – Kết quả trung gian
    // =========================================================================

    @Test
    @DisplayName("STT 01: Duyệt đăng ký MỚI tự động sinh Ketqua rỗng (diemcuoi = null, không điểm giả, không trùng)")
    void testDuyetDangkySinhKetquaRong() {
        assertThat(ketquaRepository.existsByDangkydetaiid(dangky.getId())).isFalse();

        // Trưởng khoa duyệt đăng ký đề tài
        Dangkydetai approved = dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");
        assertThat(approved.getTrangthai()).isEqualTo(TrangthaiDangkydetai.DADUYET);

        // Xác minh Ketqua rỗng đã được sinh tự động
        var kqOpt = ketquaRepository.findByDangkydetaiid(dangky.getId());
        assertThat(kqOpt).isPresent();
        Ketqua kq = kqOpt.get();
        assertThat(kq.getDiemcuoi()).isNull(); // Không sinh điểm giả!
        assertThat(kq.getDacongbo()).isFalse(); // Chưa công bố

        // Không thể duyệt lại đề tài đã được duyệt
        assertThrows(NghiepvuException.class, () ->
                dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN")
        );
        assertThat(ketquaRepository.findAll().stream().filter(k -> k.getDangkydetaiid().equals(dangky.getId())).count()).isEqualTo(1);
    }

    // =========================================================================
    // 2. KIỂM THỬ YÊU CẦU 02 (STT 02 - P0): Giảng viên phản biện
    // =========================================================================

    @Test
    @DisplayName("STT 02: Phân công GVPB hợp lệ cho đề tài đã duyệt")
    void testPhanCongGvpbHopLe() {
        dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");

        Giangvienphanbien pb = giangvienphanbienService.phanCong(dangky.getId(), gv2.getId(), "TK_ADMIN");
        assertThat(pb).isNotNull();
        assertThat(pb.getDangkydetaiid()).isEqualTo(dangky.getId());
        assertThat(pb.getGiangvienid()).isEqualTo(gv2.getId());
    }

    @Test
    @DisplayName("STT 02: Chặn phân công GVPB khi giảng viên chính là GVHD")
    void testChanGvhdLamGvpb() {
        dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");

        // GV1 là GVHD, cố tình phân công làm GVPB
        NghiepvuException ex = assertThrows(NghiepvuException.class, () ->
                giangvienphanbienService.phanCong(dangky.getId(), gv1.getId(), "TK_ADMIN")
        );
        assertThat(ex.getMessage()).contains("Giảng viên hướng dẫn không được làm giảng viên phản biện");
    }

    @Test
    @DisplayName("STT 02: Chặn phân công trùng 1 GVPB 2 lần cho cùng 1 đề tài")
    void testChanGvpbTrung() {
        dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");

        giangvienphanbienService.phanCong(dangky.getId(), gv2.getId(), "TK_ADMIN");

        // Phân công lần 2 cho cùng GV2
        NghiepvuException ex = assertThrows(NghiepvuException.class, () ->
                giangvienphanbienService.phanCong(dangky.getId(), gv2.getId(), "TK_ADMIN")
        );
        assertThat(ex.getMessage()).contains("đã được phân công phản biện");
    }

    @Test
    @DisplayName("STT 02: Chặn người không có thẩm quyền phân công GVPB")
    void testChanKhongDuThamQuyenPhanCong() {
        dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");

        // Đổi sang quyền SINHVIEN
        setAuth("SV001", "ROLE_SINHVIEN");

        assertThrows(AccessDeniedException.class, () ->
                giangvienphanbienService.phanCong(dangky.getId(), gv2.getId(), "SV001")
        );
    }

    @Test
    @DisplayName("STT 02: Chặn đổi hoặc xóa GVPB sau khi giảng viên này đã chấm điểm")
    void testChanDoiGvpbSauKhiDaChamDiem() {
        dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");
        giangvienphanbienService.phanCong(dangky.getId(), gv2.getId(), "TK_ADMIN");

        // Giả lập GV2 đã nhập điểm
        Diem diem = new Diem();
        diem.setDangkydetaiid(dangky.getId());
        diem.setGiangvienid(gv2.getId());
        diem.setDiemso(new BigDecimal("8.50"));
        diemRepository.save(diem);

        // Cố tình xóa hoặc thay đổi GVPB đã chấm
        NghiepvuException ex = assertThrows(NghiepvuException.class, () ->
                giangvienphanbienService.xoaPhanCong(dangky.getId(), gv2.getId(), "TK_ADMIN")
        );
        assertThat(ex.getMessage()).contains("Không thể xóa hoặc thay đổi giảng viên phản biện đã hoàn thành chấm điểm");
    }

    // =========================================================================
    // 3. KIỂM THỬ YÊU CẦU 03 (STT 03 - P0): Tổng hợp điểm
    // =========================================================================

    @Test
    @DisplayName("STT 03: Tổng hợp điểm trung bình cộng đúng HALF_UP và cập nhật Ketqua")
    void testTongHopDiemVaCapNhatKetqua() {
        dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");

        // Thêm 3 điểm thành phần: 8.0, 8.5, 9.0 -> Tổng = 25.5 / 3 = 8.50
        Diem d1 = new Diem();
        d1.setDangkydetaiid(dangky.getId());
        d1.setGiangvienid(gv2.getId());
        d1.setDiemso(new BigDecimal("8.00"));
        diemRepository.save(d1);

        Diem d2 = new Diem();
        d2.setDangkydetaiid(dangky.getId());
        d2.setGiangvienid(999L);
        d2.setDiemso(new BigDecimal("8.50"));
        diemRepository.save(d2);

        Diem d3 = new Diem();
        d3.setDangkydetaiid(dangky.getId());
        d3.setGiangvienid(998L);
        d3.setDiemso(new BigDecimal("9.00"));
        diemRepository.save(d3);

        // Gọi tổng hợp điểm
        Ketqua kq = nghiepvuDetaiService.tinhVaLuuKetqua(dangky.getId());
        assertThat(kq.getDiemcuoi()).isEqualByComparingTo("8.50");
        assertThat(kq.getDacongbo()).isFalse();

        // Kiểm tra đối chiếu trong repository
        Ketqua savedKq = ketquaRepository.findByDangkydetaiid(dangky.getId()).orElseThrow();
        assertThat(savedKq.getDiemcuoi()).isEqualByComparingTo("8.50");
    }

    @Test
    @DisplayName("STT 03: Chặn tổng hợp điểm khi chưa có bất kỳ điểm thành phần nào")
    void testChanTongHopDiemKhiChuaCoDiem() {
        dangkydetaiService.duyetDangky(dangky.getId(), true, "TK_ADMIN");

        NghiepvuException ex = assertThrows(NghiepvuException.class, () ->
                nghiepvuDetaiService.tinhVaLuuKetqua(dangky.getId())
        );
        assertThat(ex.getMessage()).contains("Chua co diem de tinh ket qua");
    }
}
