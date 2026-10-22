package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.ThongbaoFormDTO;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
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
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest(classes = QuanlydetaisinhvienApplication.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:thongbao-backend;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
    "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@Import(ThongbaoTestSupport.TestViews.class)
@Transactional
abstract class ThongbaoTestSupport {
    @Autowired EntityManager em;
    @Autowired ThongbaoRepository repository;
    @Autowired TaikhoanRepository accounts;
    @Autowired ThongbaoService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    Taikhoan chief;
    final LocalDateTime oldDate = LocalDateTime.of(2025, 9, 1, 8, 0);

    @BeforeEach void h2Fixture() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:thongbao-backend");
        }
        chief = account("notice-chief", Vaitro.TRUONGKHOA, "Trưởng khoa kiểm thử");
        account("notice-student", Vaitro.SINHVIEN, "Sinh viên kiểm thử");
        account("notice-lecturer", Vaitro.GIANGVIEN, "Giảng viên kiểm thử");
        account("notice-head", Vaitro.TRUONGBOMON, "Trưởng bộ môn kiểm thử");
        authenticate("notice-chief", "TRUONGKHOA");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    Taikhoan account(String username, Vaitro role, String fullName) {
        var account = new Taikhoan();
        account.setTendangnhap(username); account.setMatkhau("unused-h2-password");
        account.setHoten(fullName); account.setVaitro(role); account.setKichhoat(true);
        em.persist(account); em.flush(); return account;
    }

    Thongbao notice(String title, String body, Long author, LocalDateTime date) {
        var row = new Thongbao(); row.setTieude(title); row.setNoidung(body);
        row.setNguoidangid(author); row.setNgaydang(date); em.persist(row); em.flush(); return row;
    }

    ThongbaoFormDTO form(String title, String body) {
        var form = new ThongbaoFormDTO(); form.setTieude(title); form.setNoidung(body); return form;
    }

    void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                username, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    List<Map<String, Object>> noticeSnapshot() {
        em.flush(); return jdbc.queryForList("select * from thongbao order by Id");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestViews {
        @Bean ViewResolver backendOnlyViews() { return new BackendViews(); }
    }

    /** Test model/view contracts without reading or writing Antigravity's templates. */
    static class BackendViews implements ViewResolver, Ordered {
        @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
        @Override public View resolveViewName(String name, Locale locale) {
            if (name.startsWith("redirect:")) return new RedirectView(name.substring(9), true);
            if (!Set.of("thongbao-list", "thongbao-form", "thongbao-detail", "error", "403").contains(name)) return null;
            return new AbstractView() {
                @Override protected void renderMergedOutputModel(Map<String, Object> model,
                        HttpServletRequest request, HttpServletResponse response) {}
            };
        }
    }
}
