package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.DiemService;
import com.example.quanlydetaisinhvien.service.GiangvienphanbienService;
import com.example.quanlydetaisinhvien.dto.PhanbienFormDto;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:m16-sec-ctrl;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Module16SecurityControllerTests {

    @Autowired private DataSource dataSource;
    @Autowired private EntityManager em;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private WebApplicationContext context;

    @Autowired private DangkydetaiRepository dangkydetaiRepository;
    @Autowired private GiangvienphanbienService giangvienphanbienService;
    @Autowired private DiemService diemService;
    @Autowired private DiemRepository diemRepository;
    @Autowired private TaikhoanRepository taikhoanRepository;
    @Autowired private GiangvienRepository giangvienRepository;

    private MockMvc mvc;
    private TransactionTemplate tx;

    private Long regId;
    private Long reviewerGvId;
    private Long chairGvId;

    @BeforeAll
    void initDb() {
        new ResourceDatabasePopulator(new ClassPathResource("ketqua-new-tables-h2.sql")).execute(dataSource);
        tx = new TransactionTemplate(transactions);
    }

    @BeforeEach
    void setupData() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        if (regId != null) {
            return;
        }

        tx.executeWithoutResult(status -> {
            Dotdangky round = new Dotdangky();
            round.setTendot("Đợt Controller Test");
            round.setLoaidot(Loaidot.TLCN);
            round.setTrangthai(TrangthaiDotdangky.DONG);
            round.setBatdaugiangvien(LocalDateTime.now().minusMonths(2));
            round.setKetthucgiangvien(LocalDateTime.now().minusMonths(1));
            round.setBatdausinhvien(LocalDateTime.now().minusWeeks(3));
            round.setKetthucsinhvien(LocalDateTime.now().minusWeeks(2));
            em.persist(round);

            Taikhoan tkGuide = mkAccount("sec-guide", Vaitro.GIANGVIEN);
            Giangvien gvGuide = mkLecturer(tkGuide);

            Taikhoan tkRev = mkAccount("m16-reviewer", Vaitro.GIANGVIEN);
            Giangvien gvRev = mkLecturer(tkRev);
            reviewerGvId = gvRev.getId();

            Taikhoan tkChair = mkAccount("m16-chair", Vaitro.GIANGVIEN);
            Giangvien gvChair = mkLecturer(tkChair);
            chairGvId = gvChair.getId();

            Detai dt = new Detai();
            dt.setDotdangkyid(round.getId());
            dt.setBomonid(1L);
            dt.setTendetai("Đề tài Test Chống Chấm Thay");
            dt.setGiangviendexuatid(gvGuide.getId());
            dt.setTrangthai(TrangthaiDetai.CONGBO);
            em.persist(dt);

            Giangvienhuongdan hd = new Giangvienhuongdan();
            hd.setDetaiid(dt.getId());
            hd.setGiangvienid(gvGuide.getId());
            em.persist(hd);

            Nhomsinhvien grp = new Nhomsinhvien();
            grp.setDotdangkyid(round.getId());
            grp.setTennhom("Nhóm Test Security");
            grp.setNhomtruongid(1L);
            em.persist(grp);

            Dangkydetai dk = new Dangkydetai();
            dk.setDetaiid(dt.getId());
            dk.setNhomid(grp.getId());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk.setNgaydangky(LocalDateTime.now());
            em.persist(dk);
            regId = dk.getId();
        });

        // Phân công m16-reviewer làm phản biện
        PhanbienFormDto pbForm = new PhanbienFormDto();
        pbForm.setDangkydetaiid(regId);
        pbForm.setGiangvienid(reviewerGvId);
        giangvienphanbienService.phanCong(pbForm);
    }

    private Taikhoan mkAccount(String name, Vaitro role) {
        return taikhoanRepository.findByTendangnhap(name).orElseGet(() -> {
            Taikhoan tk = new Taikhoan();
            tk.setTendangnhap(name);
            tk.setMatkhau("pwd");
            tk.setHoten(name);
            tk.setVaitro(role);
            tk.setEmail(name + "@test.com");
            tk.setKichhoat(true);
            em.persist(tk);
            return tk;
        });
    }

    private Giangvien mkLecturer(Taikhoan tk) {
        return giangvienRepository.findByTaikhoanid(tk.getId()).orElseGet(() -> {
            Giangvien gv = new Giangvien();
            gv.setTaikhoanid(tk.getId());
            gv.setMagiangvien("GV-" + tk.getId());
            gv.setBomonid(1L);
            em.persist(gv);
            return gv;
        });
    }

    @Test
    @DisplayName("P0 HTTP: m16-reviewer gửi POST /diem/them với giangvienid = m16-chair -> BỊ TỪ CHỐI, KHÔNG LƯU DB")
    void testHttp_m16Reviewer_chamThay_m16Chair_biTuChoi() throws Exception {
        // Đăng nhập m16-reviewer, nhưng gửi giangvienid của m16-chair
        mvc.perform(post("/diem/them")
                .with(user("m16-reviewer").roles("GIANGVIEN"))
                .with(csrf())
                .param("dangkydetaiid", String.valueOf(regId))
                .param("giangvienid", String.valueOf(chairGvId)) // Spoofed chair ID!
                .param("diemso", "8.75")
                .param("nhanxet", "Kiểm tra phân quyền chấm điểm"))
                .andExpect(status().isOk()) // Trả về trang form kèm thông báo lỗi
                .andExpect(view().name("diem-form"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", org.hamcrest.Matchers.containsString("chỉ được nhập điểm cho chính mình")));

        // Kiểm tra database: TUYỆT ĐỐI không có bản ghi điểm nào được tạo dưới ID của chair
        boolean existsUnderChair = diemRepository.existsByDangkydetaiidAndGiangvienid(regId, chairGvId);
        assertThat(existsUnderChair).isFalse();
    }

    @Test
    @DisplayName("P0 HTTP: m16-reviewer gửi POST /diem/them đúng quyền của mình -> THÀNH CÔNG, LƯU VÀO DB")
    void testHttp_m16Reviewer_chamDungQuyen_thanhCong() throws Exception {
        mvc.perform(post("/diem/them")
                .with(user("m16-reviewer").roles("GIANGVIEN"))
                .with(csrf())
                .param("dangkydetaiid", String.valueOf(regId))
                .param("giangvienid", String.valueOf(reviewerGvId))
                .param("diemso", "8.75")
                .param("nhanxet", "Điểm hợp lệ của reviewer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/diem"))
                .andExpect(flash().attributeExists("successMessage"));

        // Kiểm tra database: điểm đã được lưu thành công cho reviewer
        boolean existsUnderReviewer = diemRepository.existsByDangkydetaiidAndGiangvienid(regId, reviewerGvId);
        assertThat(existsUnderReviewer).isTrue();
    }
}
