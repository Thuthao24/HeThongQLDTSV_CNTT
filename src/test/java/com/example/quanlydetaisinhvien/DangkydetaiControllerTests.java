package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = QuanlydetaisinhvienApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:dkdt-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@Import(DangkydetaiControllerTests.TestViews.class)
@Transactional
class DangkydetaiControllerTests {

    @Autowired private EntityManager em;
    @Autowired private WebApplicationContext context;

    private MockMvc mvc;
    private Taikhoan tkGv1;
    private Taikhoan tkGv2;
    private Giangvien gv1;
    private Giangvien gv2;
    private Detai dt1;
    private Dangkydetai dk1;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        Bomon bm = new Bomon();
        bm.setMabomon("CNPM");
        bm.setTenbomon("Cong nghe phan mem");
        em.persist(bm);

        tkGv1 = new Taikhoan();
        tkGv1.setTendangnhap("gv01");
        tkGv1.setMatkhau("123456");
        tkGv1.setHoten("Giang vien 1");
        tkGv1.setVaitro(Vaitro.GIANGVIEN);
        tkGv1.setKichhoat(true);
        em.persist(tkGv1);

        tkGv2 = new Taikhoan();
        tkGv2.setTendangnhap("gv02");
        tkGv2.setMatkhau("123456");
        tkGv2.setHoten("Giang vien 2");
        tkGv2.setVaitro(Vaitro.GIANGVIEN);
        tkGv2.setKichhoat(true);
        em.persist(tkGv2);

        gv1 = new Giangvien();
        gv1.setTaikhoanid(tkGv1.getId());
        gv1.setMagiangvien("GV01");
        gv1.setBomonid(bm.getId());
        em.persist(gv1);

        gv2 = new Giangvien();
        gv2.setTaikhoanid(tkGv2.getId());
        gv2.setMagiangvien("GV02");
        gv2.setBomonid(bm.getId());
        em.persist(gv2);

        Dotdangky dot = new Dotdangky();
        dot.setTendot("Dot 1");
        dot.setLoaidot(Loaidot.KLTN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(10));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        em.persist(dot);

        dt1 = new Detai();
        dt1.setTendetai("De tai AI kiem thu");
        dt1.setDotdangkyid(dot.getId());
        dt1.setBomonid(bm.getId());
        dt1.setGiangviendexuatid(gv1.getId());
        dt1.setTrangthai(TrangthaiDetai.CONGBO);
        em.persist(dt1);

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhom 1");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(999L);
        em.persist(nhom);

        for (int i = 1; i <= 3; i++) {
            Taikhoan tkSv = new Taikhoan();
            tkSv.setTendangnhap("sv_setup_" + i);
            tkSv.setMatkhau("123");
            tkSv.setHoten("Sinh Vien Setup " + i);
            tkSv.setEmail("sv_setup_" + i + "@student.edu.vn");
            tkSv.setVaitro(Vaitro.SINHVIEN);
            tkSv.setKichhoat(true);
            em.persist(tkSv);

            Sinhvien sv = new Sinhvien();
            sv.setMasinhvien("SV_SETUP_" + i);
            sv.setBomonid(bm.getId());
            sv.setTaikhoanid(tkSv.getId());
            em.persist(sv);
            if (i == 1) nhom.setNhomtruongid(sv.getId());
            em.persist(new Thanhviennhom(nhom.getId(), sv.getId()));
        }

        dk1 = new Dangkydetai();
        dk1.setNhomid(nhom.getId());
        dk1.setDetaiid(dt1.getId());
        dk1.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dk1.setNgaydangky(LocalDateTime.now());
        em.persist(dk1);

        em.flush();
    }

    @Test
    void testLecturerCanAccessDangkydetaiList() throws Exception {
        mvc.perform(get("/dangkydetai").with(user("gv01").roles("GIANGVIEN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dangkydetai-list"))
                .andExpect(model().attributeExists("items", "vaitro"))
                .andExpect(model().attribute("vaitro", "GIANGVIEN"));
    }

    @Test
    void testHeadOfDepartmentCanAccessDangkydetaiList() throws Exception {
        mvc.perform(get("/dangkydetai").with(user("tbm01").roles("TRUONGBOMON")))
                .andExpect(status().isOk())
                .andExpect(view().name("dangkydetai-list"))
                .andExpect(model().attribute("vaitro", "TRUONGBOMON"));
    }

    @Test
    void testDeanCanAccessDangkydetaiList() throws Exception {
        mvc.perform(get("/dangkydetai").with(user("dean").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andExpect(view().name("dangkydetai-list"))
                .andExpect(model().attribute("vaitro", "TRUONGKHOA"));
    }

    @Test
    void testStudentCanAccessDangkydetaiList() throws Exception {
        mvc.perform(get("/dangkydetai").with(user("sv01").roles("SINHVIEN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dangkydetai-list"))
                .andExpect(model().attribute("vaitro", "SINHVIEN"));
    }

    @Test
    void testLecturerCanApproveTheirOwnTopicRegistration() throws Exception {
        mvc.perform(post("/dangkydetai/" + dk1.getId() + "/duyet")
                        .with(user("gv01").roles("GIANGVIEN"))
                        .with(csrf())
                        .param("chapnhan", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dangkydetai"))
                .andExpect(flash().attribute("successMessage", "Đã duyệt đề tài cho nhóm thành công!"));
    }

    @Test
    void testLecturerCannotApproveAnotherLecturerTopic() throws Exception {
        mvc.perform(post("/dangkydetai/" + dk1.getId() + "/duyet")
                        .with(user("gv02").roles("GIANGVIEN"))
                        .with(csrf())
                        .param("chapnhan", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testStudentLeaderCanRegisterAndCancelTopic() throws Exception {
        // Tạo sinh viên nhóm trưởng và 2 thành viên khác (tổng 3 thành viên)
        Bomon bm = em.createQuery("select b from Bomon b", Bomon.class).setMaxResults(1).getSingleResult();
        Dotdangky dot = em.createQuery("select d from Dotdangky d", Dotdangky.class).setMaxResults(1).getSingleResult();

        Taikhoan tkLeader = new Taikhoan();
        tkLeader.setTendangnhap("sv_leader");
        tkLeader.setMatkhau("123456");
        tkLeader.setHoten("Sinh vien Leader");
        tkLeader.setVaitro(Vaitro.SINHVIEN);
        tkLeader.setKichhoat(true);
        em.persist(tkLeader);

        Sinhvien svLeader = new Sinhvien();
        svLeader.setTaikhoanid(tkLeader.getId());
        svLeader.setMasinhvien("sv_leader");
        svLeader.setBomonid(bm.getId());
        em.persist(svLeader);

        Taikhoan tkMember2 = new Taikhoan();
        tkMember2.setTendangnhap("sv_m2");
        tkMember2.setMatkhau("123456");
        tkMember2.setHoten("Sinh vien M2");
        tkMember2.setVaitro(Vaitro.SINHVIEN);
        tkMember2.setKichhoat(true);
        em.persist(tkMember2);

        Sinhvien svM2 = new Sinhvien();
        svM2.setTaikhoanid(tkMember2.getId());
        svM2.setMasinhvien("sv_m2");
        svM2.setBomonid(bm.getId());
        em.persist(svM2);

        Taikhoan tkMember3 = new Taikhoan();
        tkMember3.setTendangnhap("sv_m3");
        tkMember3.setMatkhau("123456");
        tkMember3.setHoten("Sinh vien M3");
        tkMember3.setVaitro(Vaitro.SINHVIEN);
        tkMember3.setKichhoat(true);
        em.persist(tkMember3);

        Sinhvien svM3 = new Sinhvien();
        svM3.setTaikhoanid(tkMember3.getId());
        svM3.setMasinhvien("sv_m3");
        svM3.setBomonid(bm.getId());
        em.persist(svM3);

        Nhomsinhvien newNhom = new Nhomsinhvien();
        newNhom.setTennhom("Nhom Test Dang Ky");
        newNhom.setDotdangkyid(dot.getId());
        newNhom.setNhomtruongid(svLeader.getId());
        em.persist(newNhom);

        Thanhviennhom tv1 = new Thanhviennhom();
        tv1.setNhomid(newNhom.getId());
        tv1.setSinhvienid(svLeader.getId());
        em.persist(tv1);

        Thanhviennhom tv2 = new Thanhviennhom();
        tv2.setNhomid(newNhom.getId());
        tv2.setSinhvienid(svM2.getId());
        em.persist(tv2);

        Thanhviennhom tv3 = new Thanhviennhom();
        tv3.setNhomid(newNhom.getId());
        tv3.setSinhvienid(svM3.getId());
        em.persist(tv3);

        Detai newDetai = new Detai();
        newDetai.setTendetai("De tai moi cong bo");
        newDetai.setDotdangkyid(dot.getId());
        newDetai.setBomonid(bm.getId());
        newDetai.setGiangviendexuatid(gv1.getId());
        newDetai.setTrangthai(TrangthaiDetai.CONGBO);
        em.persist(newDetai);

        em.flush();

        // 1. Nhóm trưởng đăng ký đề tài
        mvc.perform(post("/dangkydetai/dangky")
                        .with(user("sv_leader").roles("SINHVIEN"))
                        .with(csrf())
                        .param("nhomid", newNhom.getId().toString())
                        .param("detaiid", newDetai.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dangkydetai"))
                .andExpect(flash().attribute("successMessage", "Đăng ký đề tài thành công! Chờ xét duyệt."));

        // Kiểm tra bản ghi đã được lưu
        Dangkydetai savedDk = em.createQuery("select d from Dangkydetai d where d.nhomid = :nhomid", Dangkydetai.class)
                .setParameter("nhomid", newNhom.getId())
                .getSingleResult();
        org.junit.jupiter.api.Assertions.assertNotNull(savedDk);
        org.junit.jupiter.api.Assertions.assertEquals(TrangthaiDangkydetai.CHODUYET, savedDk.getTrangthai());

        // 2. Nhóm trưởng hủy đăng ký đề tài
        mvc.perform(post("/dangkydetai/" + savedDk.getId() + "/huy")
                        .with(user("sv_leader").roles("SINHVIEN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dangkydetai"))
                .andExpect(flash().attribute("successMessage", "Đã hủy đăng ký đề tài thành công!"));

        // Kiểm tra bản ghi đã bị xóa
        boolean exists = !em.createQuery("select d from Dangkydetai d where d.id = :id", Dangkydetai.class)
                .setParameter("id", savedDk.getId())
                .getResultList().isEmpty();
        org.junit.jupiter.api.Assertions.assertFalse(exists);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestViews {
        @Bean
        ViewResolver testViewResolver() {
            return new ViewResolver() {
                @Override
                public View resolveViewName(String name, Locale locale) {
                    if (name.startsWith("redirect:")) {
                        return new RedirectView(name.substring(9), true);
                    }
                    return new AbstractView() {
                        @Override
                        protected void renderMergedOutputModel(Map<String, Object> model,
                                                              HttpServletRequest request,
                                                              HttpServletResponse response) {
                        }
                    };
                }
            };
        }
    }
}