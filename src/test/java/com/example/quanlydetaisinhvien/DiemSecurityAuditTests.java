package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.DiemFormDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.DiemService;
import com.example.quanlydetaisinhvien.service.NghiepvuDetaiService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:diem-audit-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
class DiemSecurityAuditTests {

    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private PasswordEncoder encoder;
    @Autowired private DiemService diemService;
    @Autowired private NghiepvuDetaiService nghiepvuDetaiService;
    @Autowired private DiemRepository diemRepository;
    @Autowired private KetquaRepository ketquaRepository;

    private Long regId;
    private Long gvReviewerId;
    private Long gvChairId;
    private Long gvGuideId;
    private Long gvUnrelatedId;

    @BeforeEach
    void setupFixture() {
        SecurityContextHolder.clearContext();
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            for (String entity : List.of("Diem", "Ketqua", "Phanconghoidong", "Thanhvienhoidong",
                    "Hoidong", "Giangvienphanbien", "Giangvienhuongdan", "Dangkydetai",
                    "Thanhviennhom", "Nhomsinhvien", "Detai", "Dotdangky", "Sinhvien",
                    "Giangvien", "Taikhoan", "Bomon")) {
                try {
                    em.createQuery("delete from " + entity).executeUpdate();
                } catch (Exception ignored) {}
            }

            Bomon bm = new Bomon();
            bm.setMabomon("CNTT");
            bm.setTenbomon("Công nghệ thông tin");
            em.persist(bm);

            Dotdangky dot = new Dotdangky();
            dot.setTendot("Đợt ĐATN 2026");
            dot.setLoaidot(Loaidot.KLTN);
            dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
            dot.setBatdaugiangvien(LocalDateTime.now().minusMonths(2));
            dot.setKetthucgiangvien(LocalDateTime.now().minusMonths(1));
            dot.setBatdausinhvien(LocalDateTime.now().minusWeeks(2));
            dot.setKetthucsinhvien(LocalDateTime.now().plusWeeks(2));
            em.persist(dot);

            String pwd = encoder.encode("pass123");

            // 1. GV Hướng dẫn
            Taikhoan tkGuide = new Taikhoan();
            tkGuide.setTendangnhap("audit-guide");
            tkGuide.setMatkhau(pwd);
            tkGuide.setHoten("GV Hướng Dẫn");
            tkGuide.setVaitro(Vaitro.GIANGVIEN);
            tkGuide.setEmail("guide@hcmute.test");
            tkGuide.setKichhoat(true);
            em.persist(tkGuide);

            Giangvien gvGuide = new Giangvien();
            gvGuide.setTaikhoanid(tkGuide.getId());
            gvGuide.setMagiangvien("GV-GUIDE");
            gvGuide.setBomonid(bm.getId());
            em.persist(gvGuide);
            gvGuideId = gvGuide.getId();

            // 2. GV Phản biện
            Taikhoan tkRev = new Taikhoan();
            tkRev.setTendangnhap("audit-rev");
            tkRev.setMatkhau(pwd);
            tkRev.setHoten("GV Phản Biện");
            tkRev.setVaitro(Vaitro.GIANGVIEN);
            tkRev.setEmail("rev@hcmute.test");
            tkRev.setKichhoat(true);
            em.persist(tkRev);

            Giangvien gvRev = new Giangvien();
            gvRev.setTaikhoanid(tkRev.getId());
            gvRev.setMagiangvien("GV-REV");
            gvRev.setBomonid(bm.getId());
            em.persist(gvRev);
            gvReviewerId = gvRev.getId();

            // 3. GV Chủ tịch Hội đồng
            Taikhoan tkChair = new Taikhoan();
            tkChair.setTendangnhap("audit-chair");
            tkChair.setMatkhau(pwd);
            tkChair.setHoten("GV Chủ Tịch");
            tkChair.setVaitro(Vaitro.GIANGVIEN);
            tkChair.setEmail("chair@hcmute.test");
            tkChair.setKichhoat(true);
            em.persist(tkChair);

            Giangvien gvChair = new Giangvien();
            gvChair.setTaikhoanid(tkChair.getId());
            gvChair.setMagiangvien("GV-CHAIR");
            gvChair.setBomonid(bm.getId());
            em.persist(gvChair);
            gvChairId = gvChair.getId();

            // 3b. GV Thư ký Hội đồng
            Taikhoan tkSec = new Taikhoan();
            tkSec.setTendangnhap("audit-sec");
            tkSec.setMatkhau(pwd);
            tkSec.setHoten("GV Thư Ký");
            tkSec.setVaitro(Vaitro.GIANGVIEN);
            tkSec.setEmail("sec@hcmute.test");
            tkSec.setKichhoat(true);
            em.persist(tkSec);

            Giangvien gvSec = new Giangvien();
            gvSec.setTaikhoanid(tkSec.getId());
            gvSec.setMagiangvien("GV-SEC");
            gvSec.setBomonid(bm.getId());
            em.persist(gvSec);

            // 3c. GV Ủy viên Hội đồng
            Taikhoan tkMem = new Taikhoan();
            tkMem.setTendangnhap("audit-mem");
            tkMem.setMatkhau(pwd);
            tkMem.setHoten("GV Ủy Viên");
            tkMem.setVaitro(Vaitro.GIANGVIEN);
            tkMem.setEmail("mem@hcmute.test");
            tkMem.setKichhoat(true);
            em.persist(tkMem);

            Giangvien gvMem = new Giangvien();
            gvMem.setTaikhoanid(tkMem.getId());
            gvMem.setMagiangvien("GV-MEM");
            gvMem.setBomonid(bm.getId());
            em.persist(gvMem);

            // 4. GV Không liên quan
            Taikhoan tkUnrelated = new Taikhoan();
            tkUnrelated.setTendangnhap("audit-unrelated");
            tkUnrelated.setMatkhau(pwd);
            tkUnrelated.setHoten("GV Ngoài Lề");
            tkUnrelated.setVaitro(Vaitro.GIANGVIEN);
            tkUnrelated.setEmail("unrelated@hcmute.test");
            tkUnrelated.setKichhoat(true);
            em.persist(tkUnrelated);

            Giangvien gvUnrelated = new Giangvien();
            gvUnrelated.setTaikhoanid(tkUnrelated.getId());
            gvUnrelated.setMagiangvien("GV-UNRELATED");
            gvUnrelated.setBomonid(bm.getId());
            em.persist(gvUnrelated);
            gvUnrelatedId = gvUnrelated.getId();

            // Sinh viên & Nhóm
            Taikhoan tkStu = new Taikhoan();
            tkStu.setTendangnhap("audit-stu");
            tkStu.setMatkhau(pwd);
            tkStu.setHoten("Sinh viên Test");
            tkStu.setVaitro(Vaitro.SINHVIEN);
            tkStu.setEmail("stu@hcmute.test");
            tkStu.setKichhoat(true);
            em.persist(tkStu);

            Sinhvien sv = new Sinhvien();
            sv.setTaikhoanid(tkStu.getId());
            sv.setMasinhvien("SV-AUDIT-01");
            sv.setBomonid(bm.getId());
            em.persist(sv);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(dot.getId());
            grp.setTennhom("Nhóm Audit");
            grp.setNhomtruongid(sv.getId());
            em.persist(grp);

            Thanhviennhom tv = new Thanhviennhom();
            tv.setNhomid(grp.getId());
            tv.setSinhvienid(sv.getId());
            em.persist(tv);

            // Đề tài & GVHD
            Detai dt = new Detai();
            dt.setDotdangkyid(dot.getId());
            dt.setBomonid(bm.getId());
            dt.setTendetai("Đề tài Audit Bảo Mật");
            dt.setGiangviendexuatid(gvGuide.getId());
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan gvhd = new Giangvienhuongdan();
            gvhd.setDetaiid(dt.getId());
            gvhd.setGiangvienid(gvGuide.getId());
            em.persist(gvhd);

            // Đăng ký đề tài
            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now().minusDays(5));
            em.persist(dk);
            regId = dk.getId();

            // Phân công phản biện
            Giangvienphanbien pb = new Giangvienphanbien();
            pb.setDangkydetaiid(dk.getId());
            pb.setGiangvienid(gvRev.getId());
            em.persist(pb);

            // Hội đồng & Phân công hội đồng
            Hoidong hd = new Hoidong();
            hd.setDotdangkyid(dot.getId());
            hd.setTenhoidong("Hội đồng Audit");
            hd.setNgaybaocao(LocalDateTime.now().plusDays(2));
            hd.setPhong("B1-101");
            em.persist(hd);

            Thanhvienhoidong tvhd = new Thanhvienhoidong();
            tvhd.setHoidongid(hd.getId());
            tvhd.setGiangvienid(gvChair.getId());
            tvhd.setVaitro(VaitroHoidong.CHUTICH);
            em.persist(tvhd);

            Thanhvienhoidong tvhd2 = new Thanhvienhoidong();
            tvhd2.setHoidongid(hd.getId());
            tvhd2.setGiangvienid(gvSec.getId());
            tvhd2.setVaitro(VaitroHoidong.THUKY);
            em.persist(tvhd2);

            Thanhvienhoidong tvhd3 = new Thanhvienhoidong();
            tvhd3.setHoidongid(hd.getId());
            tvhd3.setGiangvienid(gvMem.getId());
            tvhd3.setVaitro(VaitroHoidong.THANHVIEN);
            em.persist(tvhd3);

            Phanconghoidong pchd = new Phanconghoidong();
            pchd.setDangkydetaiid(dk.getId());
            pchd.setHinhthuc(HinhthucDanhgia.COHOIDONG);
            pchd.setHoidongid(hd.getId());
            em.persist(pchd);

            em.flush();
        });
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void auth(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(username, "unused",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Audit 1: Giảng viên phản biện nhập điểm chính mình -> Thành công ở cả nhapDiem và save")
    void testGrading_AssignedLecturer_Success() {
        auth("audit-rev", "GIANGVIEN");

        DiemFormDto form = new DiemFormDto();
        form.setDangkydetaiid(regId);
        form.setGiangvienid(gvReviewerId);
        form.setDiemso(new BigDecimal("8.50"));
        form.setNhanxet("Phản biện tốt");

        Diem saved = diemService.nhapDiem(form);
        assertThat(saved).isNotNull();
        assertThat(saved.getDiemso()).isEqualByComparingTo(new BigDecimal("8.50"));
        assertThat(saved.getGiangvienid()).isEqualTo(gvReviewerId);
    }

    @Test
    @DisplayName("Audit 2: Chống chấm thay (Anti-impersonation) qua nhapDiem: audit-rev gửi giangvienid của audit-chair -> BỊ TỪ CHỐI")
    void testAntiImpersonation_nhapDiem_Rejected() {
        auth("audit-rev", "GIANGVIEN");

        DiemFormDto spoofedForm = new DiemFormDto();
        spoofedForm.setDangkydetaiid(regId);
        spoofedForm.setGiangvienid(gvChairId); // Giả mạo gửi ID của Chủ tịch
        spoofedForm.setDiemso(new BigDecimal("9.50"));
        spoofedForm.setNhanxet("Cố tình chấm thay chủ tịch");

        assertThatThrownBy(() -> diemService.nhapDiem(spoofedForm))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("chỉ được nhập điểm cho chính mình");

        assertThat(diemRepository.findByDangkydetaiidAndGiangvienid(regId, gvChairId)).isEmpty();
    }

    @Test
    @DisplayName("Audit 3: Chống chấm thay qua save(Diem): audit-rev gửi entity có giangvienid của audit-chair -> BỊ TỪ CHỐI")
    void testAntiImpersonation_save_Rejected() {
        auth("audit-rev", "GIANGVIEN");

        Diem spoofedScore = new Diem();
        spoofedScore.setDangkydetaiid(regId);
        spoofedScore.setGiangvienid(gvChairId);
        spoofedScore.setDiemso(new BigDecimal("9.50"));
        spoofedScore.setNhanxet("Cố tình chấm thay qua POST /diem/tao");

        assertThatThrownBy(() -> diemService.save(spoofedScore))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("chỉ được nhập điểm cho chính mình");

        assertThat(diemRepository.findByDangkydetaiidAndGiangvienid(regId, gvChairId)).isEmpty();
    }

    @Test
    @DisplayName("Audit 4: Giảng viên không được phân công chấm đề tài -> BỊ TỪ CHỐI ở cả hai đường")
    void testUnassignedLecturer_Rejected() {
        auth("audit-unrelated", "GIANGVIEN");

        DiemFormDto form = new DiemFormDto();
        form.setDangkydetaiid(regId);
        form.setGiangvienid(gvUnrelatedId);
        form.setDiemso(new BigDecimal("7.00"));
        form.setNhanxet("Chấm chui");

        assertThatThrownBy(() -> diemService.nhapDiem(form))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("chưa được phân công");

        Diem score = new Diem();
        score.setDangkydetaiid(regId);
        score.setGiangvienid(gvUnrelatedId);
        score.setDiemso(new BigDecimal("7.00"));

        assertThatThrownBy(() -> diemService.save(score))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("chưa được phân công");
    }

    @Test
    @DisplayName("Audit 5: Giảng viên hướng dẫn không được tự chấm đề tài của mình -> BỊ TỪ CHỐI")
    void testSupervisorGrading_Rejected() {
        auth("audit-guide", "GIANGVIEN");

        DiemFormDto form = new DiemFormDto();
        form.setDangkydetaiid(regId);
        form.setGiangvienid(gvGuideId);
        form.setDiemso(new BigDecimal("10.00"));
        form.setNhanxet("GVHD tự chấm");

        assertThatThrownBy(() -> diemService.nhapDiem(form))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Giảng viên hướng dẫn không được tự chấm đề tài.");

        assertThatThrownBy(() -> nghiepvuDetaiService.chamDiem(regId, gvGuideId, new BigDecimal("10.00"), "GVHD tự chấm"))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Giang vien khong the cham diem de tai do minh huong dan");
    }

    @Test
    @DisplayName("Audit 6: Không được sửa hoặc thêm điểm sau khi kết quả đã công bố")
    void testGradingAfterPublication_Rejected() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Ketqua kq = new Ketqua();
            kq.setDangkydetaiid(regId);
            kq.setDacongbo(true);
            kq.setNgaycongbo(LocalDateTime.now());
            kq.setDiemcuoi(new BigDecimal("8.50"));
            ketquaRepository.save(kq);
        });

        auth("audit-rev", "GIANGVIEN");

        DiemFormDto form = new DiemFormDto();
        form.setDangkydetaiid(regId);
        form.setGiangvienid(gvReviewerId);
        form.setDiemso(new BigDecimal("9.00"));
        form.setNhanxet("Sửa điểm sau công bố");

        assertThatThrownBy(() -> diemService.nhapDiem(form))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã công bố, không được sửa điểm");

        Diem score = new Diem();
        score.setDangkydetaiid(regId);
        score.setGiangvienid(gvReviewerId);
        score.setDiemso(new BigDecimal("9.00"));

        assertThatThrownBy(() -> diemService.save(score))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Kết quả đã công bố, không được sửa điểm");
    }

    @Test
    @DisplayName("Audit 7: Điểm ngoài thang 0 - 10 bị từ chối")
    void testScoreOutOfRange_Rejected() {
        auth("audit-rev", "GIANGVIEN");

        DiemFormDto formNegative = new DiemFormDto(regId, gvReviewerId, new BigDecimal("-1.0"), "Điểm âm");
        assertThatThrownBy(() -> diemService.nhapDiem(formNegative))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("thang điểm từ 0 đến 10");

        DiemFormDto formOver10 = new DiemFormDto(regId, gvReviewerId, new BigDecimal("10.5"), "Quá 10");
        assertThatThrownBy(() -> diemService.nhapDiem(formOver10))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("thang điểm từ 0 đến 10");
    }

    @Test void anotherLecturerCannotReadTheEditFormByGuessingScoreId() {
        auth("audit-rev", "GIANGVIEN");
        var saved = diemService.nhapDiem(new DiemFormDto(regId, gvReviewerId, new BigDecimal("8.50"), "private"));
        auth("audit-chair", "GIANGVIEN");
        assertThatThrownBy(() -> diemService.getFormDtoById(saved.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test void editFormCannotUseAnotherLecturersScoreIdToCreateAnOwnScore() {
        var other = new Diem(); other.setDangkydetaiid(regId); other.setGiangvienid(gvChairId);
        other.setDiemso(new BigDecimal("7.00")); other = diemRepository.saveAndFlush(other);
        var form = new DiemFormDto(regId, gvReviewerId, new BigDecimal("9.00"), "spoofed id");
        form.setId(other.getId());
        auth("audit-rev", "GIANGVIEN");
        assertThatThrownBy(() -> diemService.nhapDiem(form)).isInstanceOf(AccessDeniedException.class);
        assertThat(diemRepository.findByDangkydetaiidAndGiangvienid(regId, gvReviewerId)).isEmpty();
    }
}
