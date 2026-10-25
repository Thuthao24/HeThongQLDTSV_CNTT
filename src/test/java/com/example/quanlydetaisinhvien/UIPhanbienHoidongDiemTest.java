package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * STT 42 (P2) – UI phản biện / hội đồng / điểm
 *
 * Kiểm thử Controller tầng HTTP (MockMvc) cho:
 *  - Phân quyền endpoint (403 vs 3xx redirect)
 *  - Điểm biên 0/10/-0.01/10.01 qua POST /diem/tao
 *  - Flash message success/error hiển thị đúng
 *  - Form thiếu trường → redirect với errorMessage
 *  - Thêm/xóa thành viên hội đồng với các quyền khác nhau
 *  - Phân công GVPB và xóa GVPB
 *  - Trang GET /hoidong, /diem, /ketqua trả HTTP 200 với quyền phù hợp
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ui-phanbien-test2;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
class UIPhanbienHoidongDiemTest {

    @Autowired private WebApplicationContext context;

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
    @Autowired private HoidongRepository hoidongRepo;
    @Autowired private ThanhvienhoidongRepository tvhdRepo;

    private static final AtomicInteger SEQ = new AtomicInteger(5000);

    private MockMvc mvc;

    /* ── Shared fixtures ── */
    private static Long dangkyId;
    private static Long ketquaId;
    private static Long gvpbId;
    private static Long hoidongId;
    private static Long gvForMemberId;
    private static boolean fixturesCreated = false;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        if (fixturesCreated) return;
        fixturesCreated = true;

        int n = SEQ.incrementAndGet();

        Bomon bm = new Bomon();
        bm.setMabomon("UI_BM" + n);
        bm.setTenbomon("UI Test BM");
        bm = bomonRepo.save(bm);

        // Trưởng khoa – username "ui_chief"
        Taikhoan tkChief = new Taikhoan();
        tkChief.setTendangnhap("ui_chief");
        tkChief.setMatkhau("x");
        tkChief.setHoten("Chief UI");
        tkChief.setVaitro(Vaitro.TRUONGKHOA);
        tkChief.setKichhoat(true);
        taikhoanRepo.save(tkChief);

        // GV – username "ui_gv"
        Taikhoan tkGv = new Taikhoan();
        tkGv.setTendangnhap("ui_gv");
        tkGv.setMatkhau("x");
        tkGv.setHoten("GV UI");
        tkGv.setVaitro(Vaitro.GIANGVIEN);
        tkGv.setKichhoat(true);
        tkGv = taikhoanRepo.save(tkGv);
        Giangvien gvGeneral = new Giangvien();
        gvGeneral.setTaikhoanid(tkGv.getId());
        gvGeneral.setMagiangvien("UI_GV" + n);
        gvGeneral.setBomonid(bm.getId());
        giangvienRepo.save(gvGeneral);

        // SV – username "ui_sv"
        Taikhoan tkSvUser = new Taikhoan();
        tkSvUser.setTendangnhap("ui_sv");
        tkSvUser.setMatkhau("x");
        tkSvUser.setHoten("SV UI");
        tkSvUser.setVaitro(Vaitro.SINHVIEN);
        tkSvUser.setKichhoat(true);
        taikhoanRepo.save(tkSvUser);

        // GVHD
        Taikhoan tkGvhd = new Taikhoan();
        tkGvhd.setTendangnhap("ui_gvhd" + n);
        tkGvhd.setMatkhau("x");
        tkGvhd.setHoten("GVHD UI " + n);
        tkGvhd.setVaitro(Vaitro.GIANGVIEN);
        tkGvhd.setKichhoat(true);
        tkGvhd = taikhoanRepo.save(tkGvhd);
        Giangvien gvhd = new Giangvien();
        gvhd.setTaikhoanid(tkGvhd.getId());
        gvhd.setMagiangvien("UI_GVHD" + n);
        gvhd.setBomonid(bm.getId());
        gvhd = giangvienRepo.save(gvhd);

        // GVPB – username "ui_gvpb"
        Taikhoan tkGvpb = new Taikhoan();
        tkGvpb.setTendangnhap("ui_gvpb");
        tkGvpb.setMatkhau("x");
        tkGvpb.setHoten("GVPB UI");
        tkGvpb.setVaitro(Vaitro.GIANGVIEN);
        tkGvpb.setKichhoat(true);
        tkGvpb = taikhoanRepo.save(tkGvpb);
        Giangvien gvpb = new Giangvien();
        gvpb.setTaikhoanid(tkGvpb.getId());
        gvpb.setMagiangvien("UI_GVPB" + n);
        gvpb.setBomonid(bm.getId());
        gvpb = giangvienRepo.save(gvpb);
        gvpbId = gvpb.getId();

        // GV thành viên hội đồng
        Taikhoan tkMem = new Taikhoan();
        tkMem.setTendangnhap("ui_mem" + n);
        tkMem.setMatkhau("x");
        tkMem.setHoten("Mem UI " + n);
        tkMem.setVaitro(Vaitro.GIANGVIEN);
        tkMem.setKichhoat(true);
        tkMem = taikhoanRepo.save(tkMem);
        Giangvien gvMem = new Giangvien();
        gvMem.setTaikhoanid(tkMem.getId());
        gvMem.setMagiangvien("UI_MEM" + n);
        gvMem.setBomonid(bm.getId());
        gvMem = giangvienRepo.save(gvMem);
        gvForMemberId = gvMem.getId();

        // Đợt TLCN GIAIDOAN2
        Dotdangky dot = new Dotdangky();
        dot.setTendot("UI Dot " + n);
        dot.setLoaidot(Loaidot.TLCN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(10));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        dot = dotdangkyRepo.save(dot);

        // Đề tài
        Detai detai = new Detai();
        detai.setDotdangkyid(dot.getId());
        detai.setBomonid(bm.getId());
        detai.setTendetai("UI De tai " + n);
        detai.setGiangviendexuatid(gvhd.getId());
        detai.setTrangthai(TrangthaiDetai.CONGBO);
        detai = detaiRepo.save(detai);

        Giangvienhuongdan hd = new Giangvienhuongdan();
        hd.setDetaiid(detai.getId());
        hd.setGiangvienid(gvhd.getId());
        gvhdRepo.save(hd);

        // SV, Nhóm
        Taikhoan tkSv = new Taikhoan();
        tkSv.setTendangnhap("ui_sv" + n);
        tkSv.setMatkhau("x");
        tkSv.setHoten("SV UI " + n);
        tkSv.setVaitro(Vaitro.SINHVIEN);
        tkSv.setKichhoat(true);
        tkSv = taikhoanRepo.save(tkSv);
        Sinhvien sv = new Sinhvien();
        sv.setTaikhoanid(tkSv.getId());
        sv.setMasinhvien("UI_SV" + n);
        sv.setBomonid(bm.getId());
        sv = sinhvienRepo.save(sv);
        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv.getId());
        nhom.setTennhom("UI Nhom " + n);
        nhom = nhomRepo.save(nhom);

        // Đăng ký
        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(detai.getId());
        dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dk.setNgaydangky(LocalDateTime.now());
        dk = dangkyRepo.save(dk);
        dangkyId = dk.getId();

        // Kết quả
        Ketqua kq = new Ketqua();
        kq.setDangkydetaiid(dk.getId());
        kq.setDacongbo(false);
        kq = ketquaRepo.save(kq);
        ketquaId = kq.getId();

        // GVPB
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dk.getId());
        pb.setGiangvienid(gvpb.getId());
        gvpbRepo.save(pb);

        // Phân công KHONGHOIDONG (cho TLCN)
        Phanconghoidong pchd = new Phanconghoidong();
        pchd.setDangkydetaiid(dk.getId());
        pchd.setHinhthuc(HinhthucDanhgia.KHONGHOIDONG);
        pchd.setHoidongid(null);
        pchdRepo.save(pchd);

        // Hội đồng cùng đợt
        Hoidong hd2 = new Hoidong();
        hd2.setDotdangkyid(dot.getId());
        hd2.setTenhoidong("UI Hoi dong " + n);
        hd2 = hoidongRepo.save(hd2);
        hoidongId = hd2.getId();
    }

    @AfterEach
    void cleanUpPerTest() {
        diemRepo.deleteAll();
        tvhdRepo.deleteAll();
    }

    /* ════════════════════════════════════════════════
     * NHÓM A: Phân quyền trang GET
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-A01: GET /hoidong – Trưởng khoa thấy trang 200")
    void getHoidongPageByChief() throws Exception {
        mvc.perform(get("/hoidong").with(user("ui_chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andExpect(view().name("hoidong-list"));
    }

    @Test
    @DisplayName("STT42-A02: GET /hoidong – Giảng viên thấy trang 200 (read-only)")
    void getHoidongPageByGv() throws Exception {
        mvc.perform(get("/hoidong").with(user("ui_gv").roles("GIANGVIEN")))
                .andExpect(status().isOk())
                .andExpect(view().name("hoidong-list"));
    }

    @Test
    @DisplayName("STT42-A03: GET /hoidong – Chưa đăng nhập → redirect login")
    void getHoidongPageUnauthenticated() throws Exception {
        mvc.perform(get("/hoidong"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("STT42-A04: GET /diem – Giảng viên thấy trang 200")
    void getDiemPageByGv() throws Exception {
        mvc.perform(get("/diem").with(user("ui_gv").roles("GIANGVIEN")))
                .andExpect(status().isOk())
                .andExpect(view().name("diem-list"));
    }

    @Test
    @DisplayName("STT42-A05: GET /ketqua – Trưởng khoa thấy trang 200")
    void getKetquaPageByChief() throws Exception {
        mvc.perform(get("/ketqua").with(user("ui_chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andExpect(view().name("ketqua-list"));
    }

    /* ════════════════════════════════════════════════
     * NHÓM B: Phân quyền endpoint POST hội đồng
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-B01: POST /hoidong/tao – Sinh viên bị 403")
    void createHoidongBySinhVienForbidden() throws Exception {
        mvc.perform(post("/hoidong/tao")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf())
                        .param("tenhoidong", "HD cấm")
                        .param("dotdangkyid", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-B02: POST /hoidong/thanhvien – Giảng viên bị 403")
    void addMemberByGvForbidden() throws Exception {
        mvc.perform(post("/hoidong/thanhvien")
                        .with(user("ui_gv").roles("GIANGVIEN")).with(csrf())
                        .param("hoidongid", hoidongId.toString())
                        .param("giangvienid", gvForMemberId.toString())
                        .param("vaitro", "THANHVIEN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-B03: POST /hoidong/thanhvien – Trưởng khoa thêm thành viên → redirect với successMessage")
    void addMemberByChiefSuccess() throws Exception {
        mvc.perform(post("/hoidong/thanhvien")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf())
                        .param("hoidongid", hoidongId.toString())
                        .param("giangvienid", gvForMemberId.toString())
                        .param("vaitro", "CHUTICH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hoidong"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("STT42-B04: POST /hoidong/thanhvien – Thêm GV trùng → redirect với errorMessage")
    void addDuplicateMemberShowsError() throws Exception {
        // Thêm lần 1
        mvc.perform(post("/hoidong/thanhvien")
                .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf())
                .param("hoidongid", hoidongId.toString())
                .param("giangvienid", gvForMemberId.toString())
                .param("vaitro", "THUKY"));

        // Thêm lần 2 – đã có → lỗi
        mvc.perform(post("/hoidong/thanhvien")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf())
                        .param("hoidongid", hoidongId.toString())
                        .param("giangvienid", gvForMemberId.toString())
                        .param("vaitro", "THANHVIEN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hoidong"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("STT42-B05: POST /hoidong/thanhvien – Thiếu param vaitro → redirect với errorMessage")
    void addMemberMissingVaitroShowsError() throws Exception {
        mvc.perform(post("/hoidong/thanhvien")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf())
                        .param("hoidongid", hoidongId.toString())
                        .param("giangvienid", gvForMemberId.toString())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("STT42-B06: POST /hoidong/thanhvien/xoa – Sinh viên bị 403")
    void removeMemberBySinhVienForbidden() throws Exception {
        mvc.perform(post("/hoidong/thanhvien/xoa")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf())
                        .param("hoidongid", hoidongId.toString())
                        .param("giangvienid", gvForMemberId.toString()))
                .andExpect(status().isForbidden());
    }

    /* ════════════════════════════════════════════════
     * NHÓM C: Nhập điểm – biên và phân quyền HTTP
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-C01: POST /diem/tao – Sinh viên bị 403")
    void createDiemBySinhVienForbidden() throws Exception {
        mvc.perform(post("/diem/tao")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf())
                        .param("dangkydetaiid", dangkyId.toString())
                        .param("giangvienid", gvpbId.toString())
                        .param("diemso", "8.00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-C02: POST /diem/tao – Điểm biên 0.00 hợp lệ → redirect thành công")
    void createDiemBoundaryZeroSuccess() throws Exception {
        mvc.perform(post("/diem/tao")
                        .with(user("ui_gvpb").roles("GIANGVIEN")).with(csrf())
                        .param("dangkydetaiid", dangkyId.toString())
                        .param("giangvienid", gvpbId.toString())
                        .param("diemso", "0.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diem"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("STT42-C03: POST /diem/tao – Điểm biên 10.00 hợp lệ → redirect thành công")
    void createDiemBoundaryTenSuccess() throws Exception {
        mvc.perform(post("/diem/tao")
                        .with(user("ui_gvpb").roles("GIANGVIEN")).with(csrf())
                        .param("dangkydetaiid", dangkyId.toString())
                        .param("giangvienid", gvpbId.toString())
                        .param("diemso", "10.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diem"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "10.01", "11.00", "-1.00"})
    @DisplayName("STT42-C04: POST /diem/tao – Điểm ngoài [0,10] → redirect với errorMessage")
    void createDiemOutOfRangeShowsError(String score) throws Exception {
        mvc.perform(post("/diem/tao")
                        .with(user("ui_gvpb").roles("GIANGVIEN")).with(csrf())
                        .param("dangkydetaiid", dangkyId.toString())
                        .param("giangvienid", gvpbId.toString())
                        .param("diemso", score))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diem"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("STT42-C05: POST /diem/tao – ID không hợp lệ → redirect với errorMessage")
    void createDiemInvalidIdShowsError() throws Exception {
        mvc.perform(post("/diem/tao")
                        .with(user("ui_gvpb").roles("GIANGVIEN")).with(csrf())
                        .param("dangkydetaiid", "999999999")
                        .param("giangvienid", gvpbId.toString())
                        .param("diemso", "8.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diem"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    /* ════════════════════════════════════════════════
     * NHÓM D: Xóa điểm – phân quyền HTTP
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-D01: POST /diem/xoa/{id} – Sinh viên bị 403")
    void deleteDiemBySinhVienForbidden() throws Exception {
        mvc.perform(post("/diem/xoa/999")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-D02: POST /diem/xoa/{id} – Trưởng khoa xóa điểm không tồn tại → redirect với errorMessage")
    void deleteNonExistentDiemShowsError() throws Exception {
        mvc.perform(post("/diem/xoa/999999")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diem"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("STT42-D03: POST /diem/xoa/{id} – ID không phải số → redirect với errorMessage")
    void deleteDiemInvalidIdShowsError() throws Exception {
        mvc.perform(post("/diem/xoa/abc")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("errorMessage"));
    }

    /* ════════════════════════════════════════════════
     * NHÓM E: Tổng hợp điểm /diem/tong-hop
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-E01: POST /diem/tong-hop/{id} – Sinh viên bị 403")
    void tongHopBySinhVienForbidden() throws Exception {
        mvc.perform(post("/diem/tong-hop/" + dangkyId)
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-E02: POST /diem/tong-hop/{id} – Thiếu điểm → redirect với errorMessage")
    void tongHopWithNoScoresShowsError() throws Exception {
        mvc.perform(post("/diem/tong-hop/" + dangkyId)
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diem"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    /* ════════════════════════════════════════════════
     * NHÓM F: Phân công GVPB qua KetquaController
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-F01: POST /ketqua/{id}/phanbien – Sinh viên bị 403")
    void assignGvpbBySinhVienForbidden() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/phanbien")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf())
                        .param("giangvienid", gvpbId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-F02: POST /ketqua/{id}/phanbien – GV thường bị 403")
    void assignGvpbByGvForbidden() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/phanbien")
                        .with(user("ui_gv").roles("GIANGVIEN")).with(csrf())
                        .param("giangvienid", gvpbId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-F03: POST /ketqua/{id}/phanbien – Phân công GVPB trùng → redirect với errorMessage")
    void assignDuplicateGvpbShowsError() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/phanbien")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf())
                        .param("giangvienid", gvpbId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ketqua"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("STT42-F04: POST /ketqua/{id}/phanbien/xoa – Sinh viên bị 403")
    void removeGvpbBySinhVienForbidden() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/phanbien/xoa")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf())
                        .param("giangvienid", gvpbId.toString()))
                .andExpect(status().isForbidden());
    }

    /* ════════════════════════════════════════════════
     * NHÓM G: Phân công hội đồng (KetquaController)
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-G01: POST /ketqua/{id}/phancong – Sinh viên bị 403")
    void assignCouncilBySinhVienForbidden() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/phancong")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf())
                        .param("hinhthuc", "COHOIDONG")
                        .param("hoidongid", hoidongId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-G02: POST /ketqua/{id}/phancong – hinhthuc không hợp lệ → 400 error page")
    void assignCouncilInvalidHinhthucReturnsError() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/phancong")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf())
                        .param("hinhthuc", "INVALID_HINHTHUC")
                        .param("hoidongid", hoidongId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("STT42-G03: POST /ketqua/{id}/tonghop – Sinh viên bị 403")
    void tongHopKetquaBySinhVienForbidden() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/tonghop")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    /* ════════════════════════════════════════════════
     * NHÓM H: Công bố kết quả
     * ════════════════════════════════════════════════ */

    @Test
    @DisplayName("STT42-H01: POST /ketqua/{id}/congbo – Điểm cuối chưa đủ điều kiện → redirect errorMessage")
    void publishWithoutScoreShowsError() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/congbo")
                        .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ketqua"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("STT42-H02: POST /ketqua/{id}/congbo – Sinh viên bị 403")
    void publishBySinhVienForbidden() throws Exception {
        mvc.perform(post("/ketqua/" + ketquaId + "/congbo")
                        .with(user("ui_sv").roles("SINHVIEN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("STT42-H03: POST /ketqua/{id}/congbo – Kết quả đã công bố → redirect errorMessage")
    void publishAlreadyPublishedShowsError() throws Exception {
        Ketqua kq = ketquaRepo.findById(ketquaId).orElseThrow();
        kq.setDiemcuoi(new BigDecimal("8.00"));
        kq.setDacongbo(true);
        kq.setNgaycongbo(LocalDateTime.now());
        ketquaRepo.save(kq);

        try {
            mvc.perform(post("/ketqua/" + ketquaId + "/congbo")
                            .with(user("ui_chief").roles("TRUONGKHOA")).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/ketqua"))
                    .andExpect(flash().attributeExists("errorMessage"));
        } finally {
            kq.setDacongbo(false);
            kq.setNgaycongbo(null);
            ketquaRepo.save(kq);
        }
    }
}
