package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import org.junit.jupiter.api.*;
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
 * STT 13 (P1) – Hội đồng 3–5 GV
 *
 * Bao gồm toàn bộ các ca kiểm thử theo yêu cầu bảng phân công:
 *  - Thêm 2 GV (< 3): hội đồng hợp lệ nhưng KetquaPublicationValidator phải báo lỗi
 *  - Thêm 3 GV: hợp lệ tối thiểu
 *  - Thêm 5 GV: hợp lệ tối đa
 *  - Thêm 6 GV (> 5): bị chặn bởi HoidongMemberService
 *  - 0 chủ tịch: bị validator chặn khi công bố
 *  - 2 chủ tịch: bị service chặn khi thêm thành viên
 *  - 0 thư ký: bị validator chặn khi công bố
 *  - 2 thư ký: bị service chặn khi thêm thành viên
 *  - GVHD trùng: KetquaPublicationValidator chặn khi công bố
 *  - Hội đồng khác đợt: KetquaPublicationValidator chặn
 *  - Sửa hội đồng đã có điểm công bố: HoidongMemberService chặn
 *  - GV trùng lặp trong cùng hội đồng: service chặn
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hoidong-member-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
@Transactional
class HoidongMemberServiceTest {

    @Autowired private HoidongMemberService memberService;
    @Autowired private KetquaPublicationValidator validator;
    @Autowired private KetquaService ketquaService;

    @Autowired private BomonRepository bomonRepo;
    @Autowired private TaikhoanRepository taikhoanRepo;
    @Autowired private GiangvienRepository giangvienRepo;
    @Autowired private SinhvienRepository sinhvienRepo;
    @Autowired private DotdangkyRepository dotdangkyRepo;
    @Autowired private DetaiRepository detaiRepo;
    @Autowired private GiangvienhuongdanRepository gvhdRepo;
    @Autowired private NhomsinhvienRepository nhomRepo;
    @Autowired private DangkydetaiRepository dangkyRepo;
    @Autowired private HoidongRepository hoidongRepo;
    @Autowired private ThanhvienhoidongRepository tvhdRepo;
    @Autowired private KetquaRepository ketquaRepo;
    @Autowired private PhanconghoidongRepository pchdRepo;
    @Autowired private DiemRepository diemRepo;
    @Autowired private GiangvienphanbienRepository gvpbRepo;

    private static final AtomicInteger SEQ = new AtomicInteger();

    /* ───── Shared fixtures ───── */
    private Bomon bm;
    private Dotdangky dot;
    private Detai detai;
    private Giangvien gvhd;     // GVHD – không được làm thành viên hội đồng chấm
    private Hoidong hoidong;
    private Dangkydetai dangky;
    private Ketqua ketqua;

    /** Tạo GV mới với mã tự tăng tránh trùng UNIQUE */
    private Giangvien newGv() {
        int n = SEQ.incrementAndGet();
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap("gv_h" + n);
        tk.setMatkhau("x");
        tk.setHoten("GV H" + n);
        tk.setVaitro(Vaitro.GIANGVIEN);
        tk.setKichhoat(true);
        tk = taikhoanRepo.save(tk);
        Giangvien gv = new Giangvien();
        gv.setTaikhoanid(tk.getId());
        gv.setMagiangvien("H_GV" + n);
        gv.setBomonid(bm.getId());
        return giangvienRepo.save(gv);
    }

    /** Thêm thành viên vào hội đồng (dùng auth trưởng khoa) */
    private void addMember(Long councilId, Long lecturerId, VaitroHoidong role) {
        setAuth("chief_h", "ROLE_TRUONGKHOA");
        memberService.add(councilId, lecturerId, role);
    }

    private void setAuth(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "x",
                        List.of(new SimpleGrantedAuthority(role))));
    }

    @BeforeEach
    void setUp() {
        setAuth("chief_h", "ROLE_TRUONGKHOA");

        bm = new Bomon();
        bm.setMabomon("BM_H" + SEQ.incrementAndGet());
        bm.setTenbomon("BM Hoidong Test");
        bm = bomonRepo.save(bm);

        // Trưởng khoa
        Taikhoan tkChief = new Taikhoan();
        tkChief.setTendangnhap("chief_h");
        tkChief.setMatkhau("x");
        tkChief.setHoten("TK Hoidong");
        tkChief.setVaitro(Vaitro.TRUONGKHOA);
        tkChief.setKichhoat(true);
        taikhoanRepo.save(tkChief);

        // GVHD
        gvhd = newGv();

        // Đợt KLTN (bắt buộc hội đồng)
        dot = new Dotdangky();
        dot.setTendot("Dot KLTN Hoidong");
        dot.setLoaidot(Loaidot.KLTN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(20));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(10));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(15));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        dot = dotdangkyRepo.save(dot);

        // Đề tài
        detai = new Detai();
        detai.setDotdangkyid(dot.getId());
        detai.setBomonid(bm.getId());
        detai.setTendetai("De tai Hoidong Test");
        detai.setGiangviendexuatid(gvhd.getId());
        detai.setTrangthai(TrangthaiDetai.CONGBO);
        detai = detaiRepo.save(detai);

        // Gán GVHD
        Giangvienhuongdan hd = new Giangvienhuongdan();
        hd.setDetaiid(detai.getId());
        hd.setGiangvienid(gvhd.getId());
        gvhdRepo.save(hd);

        // Nhóm & sinh viên
        Taikhoan tkSv = new Taikhoan();
        tkSv.setTendangnhap("sv_h" + SEQ.incrementAndGet());
        tkSv.setMatkhau("x");
        tkSv.setHoten("SV H");
        tkSv.setVaitro(Vaitro.SINHVIEN);
        tkSv.setKichhoat(true);
        tkSv = taikhoanRepo.save(tkSv);

        Sinhvien sv = new Sinhvien();
        sv.setTaikhoanid(tkSv.getId());
        sv.setMasinhvien("SV_H" + SEQ.incrementAndGet());
        sv.setBomonid(bm.getId());
        sv = sinhvienRepo.save(sv);

        com.example.quanlydetaisinhvien.entity.Nhomsinhvien nhom = new com.example.quanlydetaisinhvien.entity.Nhomsinhvien();
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv.getId());
        nhom.setTennhom("Nhom H");
        nhom = nhomRepo.save(nhom);

        dangky = new Dangkydetai();
        dangky.setNhomid(nhom.getId());
        dangky.setDetaiid(detai.getId());
        dangky.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dangky.setNgaydangky(LocalDateTime.now());
        dangky = dangkyRepo.save(dangky);

        // Hội đồng (cùng đợt với đề tài)
        hoidong = new Hoidong();
        hoidong.setDotdangkyid(dot.getId());
        hoidong.setTenhoidong("Hoi dong Test");
        hoidong = hoidongRepo.save(hoidong);

        // Kết quả trung gian
        ketqua = new Ketqua();
        ketqua.setDangkydetaiid(dangky.getId());
        ketqua.setDacongbo(false);
        ketqua = ketquaRepo.save(ketqua);
    }

    /* ────────────────────────────────────────────────────
     * NHÓM 1: Ràng buộc số lượng thành viên (3–5 GV)
     * ──────────────────────────────────────────────────── */

    @Test
    @DisplayName("STT13-01: Thêm GV thứ 6 bị từ chối (>5)")
    void addSixthMemberRejected() {
        for (int i = 0; i < 5; i++) {
            addMember(hoidong.getId(), newGv().getId(),
                    i == 0 ? VaitroHoidong.CHUTICH : i == 1 ? VaitroHoidong.THUKY : VaitroHoidong.THANHVIEN);
        }
        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.THANHVIEN));
        assertThat(ex.getMessage()).contains("tối đa 5");
    }

    @Test
    @DisplayName("STT13-02: Hội đồng 3 GV (đủ 1 CT, 1 TK, 1 TV) – thêm thành công")
    void threeGvIsMinimumValid() {
        Giangvien ct = newGv();
        Giangvien tk = newGv();
        Giangvien tv = newGv();
        addMember(hoidong.getId(), ct.getId(), VaitroHoidong.CHUTICH);
        addMember(hoidong.getId(), tk.getId(), VaitroHoidong.THUKY);
        addMember(hoidong.getId(), tv.getId(), VaitroHoidong.THANHVIEN);
        assertThat(tvhdRepo.countByHoidongid(hoidong.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("STT13-03: Hội đồng 5 GV (tối đa) – thêm thành công")
    void fiveGvIsMaximumValid() {
        addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.CHUTICH);
        addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.THUKY);
        for (int i = 0; i < 3; i++) addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.THANHVIEN);
        assertThat(tvhdRepo.countByHoidongid(hoidong.getId())).isEqualTo(5);
    }

    @Test
    @DisplayName("STT13-04: 2 GV trong hội đồng — validator chặn công bố (thiếu đủ thành viên: cần >=3)")
    void twoGvTriggerValidatorOnPublish() {
        // Thêm 2 GV + gán hội đồng + điểm
        Giangvien ct = newGv();
        Giangvien tk = newGv();
        addMember(hoidong.getId(), ct.getId(), VaitroHoidong.CHUTICH);
        addMember(hoidong.getId(), tk.getId(), VaitroHoidong.THUKY);

        // Phân công GVPB (KLTN bắt buộc có GVPB để validator vượt qua bước kiểm tra GVPB)
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dangky.getId());
        pb.setGiangvienid(ct.getId());
        gvpbRepo.save(pb);

        // Phân công hội đồng
        Phanconghoidong pchd = new Phanconghoidong();
        pchd.setDangkydetaiid(dangky.getId());
        pchd.setHinhthuc(HinhthucDanhgia.COHOIDONG);
        pchd.setHoidongid(hoidong.getId());
        pchdRepo.save(pchd);

        // Nhập điểm cho 2 GV + cập nhật diemcuoi
        saveScore(dangky.getId(), ct.getId(), "8.00");
        saveScore(dangky.getId(), tk.getId(), "9.00");
        ketqua.setDiemcuoi(new BigDecimal("8.50"));
        ketquaRepo.save(ketqua);

        // Validator phải từ chối vì hội đồng chỉ có 2 GV (< 3)
        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> validator.validate(ketqua, pchd));
        assertThat(ex.getMessage()).contains("3–5 giảng viên");
    }

    /* ────────────────────────────────────────────────────
     * NHÓM 2: Ràng buộc vai trò chủ tịch / thư ký
     * ──────────────────────────────────────────────────── */

    @Test
    @DisplayName("STT13-05: 2 chủ tịch – bị chặn khi thêm")
    void twoChairmanRejected() {
        addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.CHUTICH);
        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.CHUTICH));
        assertThat(ex.getMessage()).contains("chủ tịch");
    }

    @Test
    @DisplayName("STT13-06: 2 thư ký – bị chặn khi thêm")
    void twoSecretaryRejected() {
        addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.THUKY);
        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.THUKY));
        assertThat(ex.getMessage()).contains("thư ký");
    }

    @Test
    @DisplayName("STT13-07: 0 chủ tịch – validator chặn khi công bố kết quả")
    void noChairmanRejectedByValidator() {
        // Chỉ có 3 TV, không có CT
        Giangvien tk = newGv(); addMember(hoidong.getId(), tk.getId(), VaitroHoidong.THUKY);
        Giangvien tv1 = newGv(); addMember(hoidong.getId(), tv1.getId(), VaitroHoidong.THANHVIEN);
        Giangvien tv2 = newGv(); addMember(hoidong.getId(), tv2.getId(), VaitroHoidong.THANHVIEN);

        // KLTN bắt buộc có GVPB – dùng tv1 làm GVPB (không trùng GVHD)
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dangky.getId());
        pb.setGiangvienid(tv1.getId());
        gvpbRepo.save(pb);

        Phanconghoidong pchd = buildPchd(HinhthucDanhgia.COHOIDONG, hoidong.getId());
        saveScore(dangky.getId(), tv1.getId(), "8.00");
        saveScore(dangky.getId(), tv2.getId(), "8.00");
        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketquaRepo.save(ketqua);

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> validator.validate(ketqua, pchd));
        assertThat(ex.getMessage()).contains("chủ tịch");
    }

    @Test
    @DisplayName("STT13-08: 0 thư ký – validator chặn khi công bố kết quả")
    void noSecretaryRejectedByValidator() {
        Giangvien ct = newGv(); addMember(hoidong.getId(), ct.getId(), VaitroHoidong.CHUTICH);
        Giangvien tv1 = newGv(); addMember(hoidong.getId(), tv1.getId(), VaitroHoidong.THANHVIEN);
        Giangvien tv2 = newGv(); addMember(hoidong.getId(), tv2.getId(), VaitroHoidong.THANHVIEN);

        // KLTN bắt buộc có GVPB – dùng ct làm GVPB (không trùng GVHD)
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dangky.getId());
        pb.setGiangvienid(ct.getId());
        gvpbRepo.save(pb);

        Phanconghoidong pchd = buildPchd(HinhthucDanhgia.COHOIDONG, hoidong.getId());
        saveScore(dangky.getId(), ct.getId(), "8.00");
        saveScore(dangky.getId(), tv1.getId(), "8.00");
        saveScore(dangky.getId(), tv2.getId(), "8.00");
        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketquaRepo.save(ketqua);

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> validator.validate(ketqua, pchd));
        assertThat(ex.getMessage()).contains("thư ký");
    }

    /* ────────────────────────────────────────────────────
     * NHÓM 3: GV trùng & GVHD trùng
     * ──────────────────────────────────────────────────── */

    @Test
    @DisplayName("STT13-09: GV trùng trong cùng hội đồng – bị chặn")
    void duplicateGvInCouncilRejected() {
        Giangvien gv = newGv();
        addMember(hoidong.getId(), gv.getId(), VaitroHoidong.CHUTICH);
        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> addMember(hoidong.getId(), gv.getId(), VaitroHoidong.THANHVIEN));
        assertThat(ex.getMessage()).contains("Giảng viên đã thuộc hội đồng");
    }

    @Test
    @DisplayName("STT13-10: GVHD trùng – validator chặn khi công bố")
    void gvhdInCouncilRejectedByValidator() {
        // Thêm GVHD vào hội đồng (service cho phép – chỉ validator chặn khi congbo)
        Giangvien ct = newGv(); addMember(hoidong.getId(), ct.getId(), VaitroHoidong.CHUTICH);
        Giangvien tk = newGv(); addMember(hoidong.getId(), tk.getId(), VaitroHoidong.THUKY);
        // GVHD làm THANHVIEN trực tiếp qua repo (bỏ qua service để test validator thuần)
        Thanhvienhoidong tvGvhd = new Thanhvienhoidong();
        tvGvhd.setHoidongid(hoidong.getId());
        tvGvhd.setGiangvienid(gvhd.getId());
        tvGvhd.setVaitro(VaitroHoidong.THANHVIEN);
        tvhdRepo.save(tvGvhd);

        // KLTN bắt buộc có GVPB – dùng ct làm GVPB (không trùng GVHD)
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dangky.getId());
        pb.setGiangvienid(ct.getId());
        gvpbRepo.save(pb);

        Phanconghoidong pchd = buildPchd(HinhthucDanhgia.COHOIDONG, hoidong.getId());
        saveScore(dangky.getId(), ct.getId(), "8.00");
        saveScore(dangky.getId(), tk.getId(), "9.00");
        saveScore(dangky.getId(), gvhd.getId(), "7.00");
        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketquaRepo.save(ketqua);

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> validator.validate(ketqua, pchd));
        assertThat(ex.getMessage()).contains("hướng dẫn đề tài");
    }

    /* ────────────────────────────────────────────────────
     * NHÓM 4: Hội đồng khác đợt
     * ──────────────────────────────────────────────────── */

    @Test
    @DisplayName("STT13-11: Phân công hội đồng khác đợt – validator chặn")
    void councilFromDifferentRoundRejected() {
        // Tạo hội đồng thuộc đợt khác
        Dotdangky dot2 = new Dotdangky();
        dot2.setTendot("Dot KLTN 2");
        dot2.setLoaidot(Loaidot.KLTN);
        dot2.setBatdaugiangvien(LocalDateTime.now().minusDays(30));
        dot2.setKetthucgiangvien(LocalDateTime.now().minusDays(5));
        dot2.setBatdausinhvien(LocalDateTime.now().minusDays(25));
        dot2.setKetthucsinhvien(LocalDateTime.now().minusDays(10));
        dot2.setTrangthai(TrangthaiDotdangky.DONG);
        dot2 = dotdangkyRepo.save(dot2);

        Hoidong hd2 = new Hoidong();
        hd2.setDotdangkyid(dot2.getId());
        hd2.setTenhoidong("HD khac dot");
        hd2 = hoidongRepo.save(hd2);

        // Thêm 3 GV đủ điều kiện vào hd2
        addMember(hd2.getId(), newGv().getId(), VaitroHoidong.CHUTICH);
        addMember(hd2.getId(), newGv().getId(), VaitroHoidong.THUKY);
        Giangvien tv = newGv(); addMember(hd2.getId(), tv.getId(), VaitroHoidong.THANHVIEN);

        // KLTN bắt buộc có GVPB cho đề tài ở đợt gốc (dot) – dùng tv làm GVPB
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dangky.getId());
        pb.setGiangvienid(tv.getId());
        gvpbRepo.save(pb);

        // Phân công hội đồng khác đợt cho đề tài của dot gốc
        Phanconghoidong pchd = buildPchd(HinhthucDanhgia.COHOIDONG, hd2.getId());
        saveScore(dangky.getId(), tv.getId(), "8.00");
        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketquaRepo.save(ketqua);

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> validator.validate(ketqua, pchd));
        assertThat(ex.getMessage()).contains("Hội đồng không thuộc đợt");
    }

    /* ────────────────────────────────────────────────────
     * NHÓM 5: Sửa hội đồng đã công bố kết quả
     * ──────────────────────────────────────────────────── */

    @Test
    @DisplayName("STT13-12: Xóa thành viên hội đồng đã có kết quả công bố – bị chặn")
    void removeFromPublishedCouncilRejected() {
        Giangvien ct = newGv(); addMember(hoidong.getId(), ct.getId(), VaitroHoidong.CHUTICH);
        Giangvien tk = newGv(); addMember(hoidong.getId(), tk.getId(), VaitroHoidong.THUKY);
        Giangvien tv = newGv(); addMember(hoidong.getId(), tv.getId(), VaitroHoidong.THANHVIEN);

        // Phân công hội đồng
        Phanconghoidong pchd = buildPchd(HinhthucDanhgia.COHOIDONG, hoidong.getId());

        // Đánh dấu kết quả đã công bố
        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketqua.setDacongbo(true);
        ketqua.setNgaycongbo(LocalDateTime.now());
        ketquaRepo.save(ketqua);

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> memberService.remove(hoidong.getId(), tv.getId()));
        assertThat(ex.getMessage()).contains("công bố");
    }

    @Test
    @DisplayName("STT13-13: Thêm thành viên vào hội đồng đã có kết quả công bố – bị chặn")
    void addToPublishedCouncilRejected() {
        addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.CHUTICH);
        addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.THUKY);
        buildPchd(HinhthucDanhgia.COHOIDONG, hoidong.getId());

        ketqua.setDiemcuoi(new BigDecimal("8.00"));
        ketqua.setDacongbo(true);
        ketqua.setNgaycongbo(LocalDateTime.now());
        ketquaRepo.save(ketqua);

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> addMember(hoidong.getId(), newGv().getId(), VaitroHoidong.THANHVIEN));
        assertThat(ex.getMessage()).contains("công bố");
    }

    /* ────────────────────────────────────────────────────
     * NHÓM 6: Phân quyền
     * ──────────────────────────────────────────────────── */

    @Test
    @DisplayName("STT13-14: Người không phải Trưởng khoa không được thêm thành viên hội đồng")
    void nonChiefCannotAddMember() {
        setAuth("gv_normal", "ROLE_GIANGVIEN");
        assertThrows(AccessDeniedException.class,
                () -> memberService.add(hoidong.getId(), newGv().getId(), VaitroHoidong.THANHVIEN));
    }

    /* ────────────────────────────────────────────────────
     * Helpers
     * ──────────────────────────────────────────────────── */

    private Phanconghoidong buildPchd(HinhthucDanhgia hinhthuc, Long hoidongId) {
        Phanconghoidong p = new Phanconghoidong();
        p.setDangkydetaiid(dangky.getId());
        p.setHinhthuc(hinhthuc);
        p.setHoidongid(hoidongId);
        return pchdRepo.save(p);
    }

    private void saveScore(Long dangkyId, Long giangvienId, String score) {
        Diem d = new Diem();
        d.setDangkydetaiid(dangkyId);
        d.setGiangvienid(giangvienId);
        d.setDiemso(new BigDecimal(score));
        diemRepo.save(d);
    }
}
