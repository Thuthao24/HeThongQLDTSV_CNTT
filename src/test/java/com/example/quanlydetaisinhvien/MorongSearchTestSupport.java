package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.morong.TimkiemNangcaoRepository;
import com.example.quanlydetaisinhvien.service.morong.TimkiemNangcaoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.view.*;
import javax.sql.DataSource;
import jakarta.servlet.http.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(classes = QuanlydetaisinhvienApplication.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:morong-search;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
    "app.ketqua.email.enabled=false", "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false",
    "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Import(MorongSearchTestSupport.TestViews.class)
@Transactional
abstract class MorongSearchTestSupport {
    @Autowired EntityManager em;
    @Autowired TimkiemNangcaoService service;
    @Autowired TimkiemNangcaoRepository repository;
    @Autowired WebApplicationContext context;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    Bomon departmentA, departmentB;
    Dotdangky roundA, roundB, roundC, roundD, emptyRound;
    Detai topicA, topicB, topicC, topicD;
    Ketqua resultA, resultB, otherResult, draft;
    Giangvien teacherA, teacherB;
    Sinhvien studentA, studentB;
    Dangkydetai registrationA, registrationB;

    @BeforeEach void fixtureInH2Only() throws Exception {
        try (var c = dataSource.getConnection()) {
            assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:morong-search");
        }
        account("search-chief", Vaitro.TRUONGKHOA);
        departmentA = department("A"); departmentB = department("B");
        teacherA = teacher("search-teacher-a", departmentA); teacherB = teacher("search-teacher-b", departmentB);
        studentA = student("search-student-a"); studentB = student("search-student-b");
        student("search-no-group");
        roundA = round("Đợt Môn học", Loaidot.MONHOC, TrangthaiDotdangky.DONG, 1);
        roundB = round("Đợt Khóa luận", Loaidot.KLTN, TrangthaiDotdangky.GIAIDOAN2, 2);
        roundC = round("Đợt riêng nhóm khác", Loaidot.NCKH, TrangthaiDotdangky.NHAP, 3);
        roundD = round("Đợt chưa công bố", Loaidot.TLCN, TrangthaiDotdangky.GIAIDOAN1, 4);
        emptyRound = round("Đợt rỗng", Loaidot.MONHOC, TrangthaiDotdangky.CONGBO, 5);
        emptyRound.setHanchamphanbien(null); emptyRound.setNgaybaocaohoidong(null);
        topicA = topic("Hệ thống quản lý sinh viên", "Tiếng Việt 100% _ dấu! ' OR '1'='1 và \\ đường dẫn", roundA, departmentA, teacherA, TrangthaiDetai.CONGBO);
        topicB = topic("Khóa luận trí tuệ nhân tạo", "Kết quả nghiên cứu", roundB, departmentB, teacherB, TrangthaiDetai.DADUYET);
        topicC = topic("Đề tài riêng người khác", null, roundC, departmentB, teacherA, TrangthaiDetai.TUCHOI);
        topicD = topic("Bản nháp bí mật", "Chưa phát hành", roundD, departmentA, teacherA, TrangthaiDetai.CHODUYET);
        guide(topicA, teacherA); guide(topicA, teacherB); guide(topicB, teacherB);
        registrationA = registration(topicA, studentA, "Nhóm Một", TrangthaiDangkydetai.DADUYET);
        registrationB = registration(topicB, studentA, "Nhóm Hai", TrangthaiDangkydetai.CHODUYET);
        var registrationC = registration(topicC, studentB, "Nhóm Khác", TrangthaiDangkydetai.TUCHOI);
        var registrationD = registration(topicD, studentA, "Nhóm Nháp", TrangthaiDangkydetai.DADUYET);
        // Ordinary member as well as leader; the student result query must not multiply rows.
        Sinhvien member = student("search-member");
        var membership = new Thanhviennhom(); membership.setNhomid(registrationA.getNhomid()); membership.setSinhvienid(member.getId()); em.persist(membership);
        resultA = result(registrationA, true, "8.25", LocalDateTime.of(2026, 10, 15, 0, 0));
        resultB = result(registrationB, true, "9.50", LocalDateTime.of(2026, 10, 16, 23, 59, 59));
        otherResult = result(registrationC, true, "7.00", LocalDateTime.of(2026, 10, 14, 8, 0));
        draft = result(registrationD, false, null, null);
        em.flush();
        auth("search-chief", "TRUONGKHOA");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }
    void auth(String name, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                name, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
    Taikhoan account(String name, Vaitro role) {
        var a = new Taikhoan(); a.setTendangnhap(name); a.setHoten("Kiểm thử " + name);
        a.setMatkhau("unused-h2-only"); a.setVaitro(role); a.setKichhoat(true); em.persist(a); return a;
    }
    Bomon department(String code) {
        var d = new Bomon(); d.setMabomon(code); d.setTenbomon("Bộ môn " + code); em.persist(d); return d;
    }
    Giangvien teacher(String name, Bomon department) {
        var a = account(name, Vaitro.GIANGVIEN);
        var t = new Giangvien(); t.setTaikhoanid(a.getId()); t.setBomonid(department.getId()); t.setMagiangvien(name); em.persist(t); return t;
    }
    Sinhvien student(String name) {
        var a = account(name, Vaitro.SINHVIEN);
        var s = new Sinhvien(); s.setTaikhoanid(a.getId()); s.setBomonid(departmentA.getId());
        s.setMasinhvien("SV" + a.getId()); em.persist(s); return s;
    }
    Dotdangky round(String name, Loaidot type, TrangthaiDotdangky state, int offset) {
        var r = new Dotdangky(); r.setTendot(name); r.setLoaidot(type); r.setTrangthai(state);
        var date = LocalDateTime.of(2026, 9, 1, 0, 0).plusDays(offset);
        r.setBatdaugiangvien(date); r.setKetthucgiangvien(date.plusDays(1));
        r.setBatdausinhvien(date.plusDays(2)); r.setKetthucsinhvien(date.plusDays(3));
        r.setHanchamphanbien(date.plusDays(4)); r.setNgaybaocaohoidong(date.plusDays(5)); em.persist(r); return r;
    }
    Detai topic(String name, String description, Dotdangky round, Bomon dept, Giangvien proposer, TrangthaiDetai state) {
        var t = new Detai(); t.setTendetai(name); t.setMota(description); t.setDotdangkyid(round.getId());
        t.setBomonid(dept.getId()); t.setGiangviendexuatid(proposer.getId()); t.setTrangthai(state); em.persist(t); return t;
    }
    void guide(Detai topic, Giangvien teacher) {
        var g = new Giangvienhuongdan(); g.setDetaiid(topic.getId()); g.setGiangvienid(teacher.getId()); em.persist(g);
    }
    Dangkydetai registration(Detai topic, Sinhvien student, String name, TrangthaiDangkydetai state) {
        var group = new Nhomsinhvien(); group.setDotdangkyid(topic.getDotdangkyid()); group.setTennhom(name);
        group.setNhomtruongid(student.getId()); em.persist(group);
        var membership = new Thanhviennhom(); membership.setNhomid(group.getId()); membership.setSinhvienid(student.getId()); em.persist(membership);
        var r = new Dangkydetai(); r.setDetaiid(topic.getId()); r.setNhomid(group.getId()); r.setTrangthai(state);
        r.setNgaydangky(LocalDateTime.of(2026, 9, 5, 8, 0)); em.persist(r); return r;
    }
    Ketqua result(Dangkydetai registration, boolean published, String score, LocalDateTime date) {
        var k = new Ketqua(); k.setDangkydetaiid(registration.getId()); k.setDacongbo(published);
        k.setDiemcuoi(score == null ? null : new BigDecimal(score)); k.setNgaycongbo(date); em.persist(k); return k;
    }
    @TestConfiguration(proxyBeanMethods = false)
    static class TestViews { @Bean ViewResolver morongTestViews() { return new BackendViews(); } }
    static class BackendViews implements ViewResolver, Ordered {
        @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
        @Override public View resolveViewName(String name, Locale locale) {
            if (!name.startsWith("morong/") && !Set.of("error", "403").contains(name)) return null;
            return new AbstractView() {
                @Override protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {}
            };
        }
    }
}
