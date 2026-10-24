package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.ThongbaoFormDTO;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import com.example.quanlydetaisinhvien.service.mail.ResultMailSender;
import com.example.quanlydetaisinhvien.service.notification.NotificationMailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:module15-workflow;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never",
        "app.thongbao.email.outbox-enabled=true","app.thongbao.email.delivery-enabled=false","app.thongbao.email.schedule-enabled=false",
        "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"})
@Import(ThongbaoWorkflowTests.NoDelivery.class)
class ThongbaoWorkflowTests {
    @TestConfiguration(proxyBeanMethods=false) static class NoDelivery {
        @Bean @Primary NotificationMailSender noNotificationDelivery() { return message->{throw new AssertionError("No real email allowed in Module 15 tests");}; }
        @Bean @Primary ResultMailSender noResultDelivery() { return message->{throw new AssertionError("No result email allowed");}; }
    }
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ThongbaoService service;
    @Autowired ThongbaoRepository notices;
    @Autowired EmailthongbaoRepository emails;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource source;
    MockMvc mvc; TransactionTemplate tx;
    @BeforeEach void fixture() throws Exception {
        try(var connection=source.getConnection()) { assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:module15-workflow"); }
        tx=new TransactionTemplate(transactions);
        tx.executeWithoutResult(s->{
            for(String entity:List.of("Emailthongbao","Thongbao","Sinhvien","Taikhoan")) em.createQuery("delete from "+entity).executeUpdate();
            account("m15-chief",Vaitro.TRUONGKHOA);
            var a=account("m15-student",Vaitro.SINHVIEN);
            var student=new Sinhvien(); student.setTaikhoanid(a.getId()); student.setBomonid(1L); student.setMasinhvien("M15-TEST"); em.persist(student);
        });
        authenticate(); mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    Taikhoan account(String name,Vaitro role) {
        var a=new Taikhoan(); a.setTendangnhap(name); a.setMatkhau("unused-h2"); a.setVaitro(role); a.setKichhoat(true); a.setHoten("Họ tên "+name); a.setEmail(name+"@example.invalid"); em.persist(a); return a;
    }
    void authenticate() { SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("m15-chief",null,List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA")))); }
    ThongbaoFormDTO form(String title) { var f=new ThongbaoFormDTO(); f.setTieude(title); f.setNoidung("Nội dung <script>alert('x')</script>\nTiếng Việt"); f.setRecipientRoles(DoituongThongbao.all()); return f; }
    @Test void parallelHttpCreateWithOneTokenCommitsOneNoticeAndOneRecipientSet() throws Exception {
        var session=new MockHttpSession();
        var get=mvc.perform(get("/thongbao/create").session(session).with(user("m15-chief").roles("TRUONGKHOA"))).andReturn();
        String token=(String)get.getModelAndView().getModel().get("submissionToken");
        Callable<String> create=()->mvc.perform(post("/thongbao").session(session).with(user("m15-chief").roles("TRUONGKHOA")).with(csrf())
                .param("submissionToken",token).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","Tạo đồng thời").param("noidung","Nội dung"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        var responses=parallel(create,create);
        assertThat(responses.get(0)).isEqualTo(responses.get(1)); assertThat(notices.count()).isEqualTo(1);
        assertThat(emails.findAll()).isEmpty();
    }
    @Test void parallelEditsWithSameRevisionHaveOneSuccessAndOneConflict() throws Exception {
        var created=service.create(form("Gốc")); var revision=ThongbaoRevision.of(created);
        Callable<Integer> a=()->edit(created.id(),revision,"Người A"); Callable<Integer> b=()->edit(created.id(),revision,"Người B");
        assertThat(parallel(a,b)).containsExactlyInAnyOrder(302,409);
        assertThat(emails.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from thongbaodoituong",Long.class)).isEqualTo(3);
        assertThat(service.findById(created.id()).ngaydang()).isEqualTo(created.ngaydang());
    }
    int edit(Long id,String revision,String title) throws Exception {
        return mvc.perform(post("/thongbao/"+id+"/edit").with(user("m15-chief").roles("TRUONGKHOA")).with(csrf())
                .param("revision",revision).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude",title).param("noidung","Nội dung mới")).andReturn().getResponse().getStatus();
    }
    @Test void parallelAudienceOnlyEditsCannotSilentlyOverwriteEachOther() throws Exception {
        var f=form("Hai người sửa đối tượng");var saved=service.create(f);String revision=ThongbaoRevision.of(saved);
        Callable<Integer> a=()->editAudience(saved.id(),revision,"SINHVIEN");
        Callable<Integer> b=()->editAudience(saved.id(),revision,"GIANGVIEN");
        assertThat(parallel(a,b)).containsExactlyInAnyOrder(302,409);
        var current=service.findById(saved.id());assertThat(current.recipientRoles()).hasSize(1);
        assertThat(current.ngaydang()).isEqualTo(saved.ngaydang());assertThat(emails.count()).isZero();
    }
    int editAudience(Long id,String revision,String role) throws Exception {
        return mvc.perform(post("/thongbao/"+id+"/edit").with(user("m15-chief").roles("TRUONGKHOA")).with(csrf())
                .param("recipientRoles",role).param("revision",revision).param("tieude","Hai người sửa đối tượng")
                .param("noidung","Nội dung không đổi giữa hai request")).andReturn().getResponse().getStatus();
    }
    @Test void rollbackOnAudienceFailureAllowsTheSameFormToRetryExactlyOnce() throws Exception {
        var session=new MockHttpSession();
        var get=mvc.perform(get("/thongbao/create").session(session).with(user("m15-chief").roles("TRUONGKHOA"))).andReturn();
        String token=(String)get.getModelAndView().getModel().get("submissionToken");
        jdbc.execute("alter table thongbaodoituong add constraint RejectM15 check (Vaitro <> 'SINHVIEN')");
        try {
            mvc.perform(post("/thongbao").session(session).with(user("m15-chief").roles("TRUONGKHOA")).with(csrf())
                    .param("submissionToken",token).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","FAIL_M15").param("noidung","Nội dung"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasErrors("thongbaoForm"));
            assertThat(notices.count()).isZero(); assertThat(emails.count()).isZero();
        } finally { jdbc.execute("alter table thongbaodoituong drop constraint RejectM15"); }
        for(int i=0;i<2;i++) mvc.perform(post("/thongbao").session(session).with(user("m15-chief").roles("TRUONGKHOA")).with(csrf())
                .param("submissionToken",token).param("recipientRoles","SINHVIEN","GIANGVIEN","TRUONGBOMON").param("tieude","FAIL_M15").param("noidung","Nội dung"))
                .andExpect(status().is3xxRedirection());
        assertThat(notices.count()).isEqualTo(1); assertThat(emails.count()).isZero();
    }
    @Test void realTemplatesEscapeContentAndRenderCsrfRevisionAndSubmissionTokens() throws Exception {
        var created=service.create(form("Tiêu đề <script>unsafe</script>"));
        String detail=mvc.perform(get("/thongbao/"+created.id()).with(user("m15-chief").roles("TRUONGKHOA"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(detail).contains("&lt;script&gt;unsafe&lt;/script&gt;","name=\"_csrf\"","Người đăng:","UTC+7").doesNotContain("<script>unsafe</script>");
        String edit=mvc.perform(get("/thongbao/"+created.id()+"/edit").with(user("m15-chief").roles("TRUONGKHOA"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(edit).contains("name=\"revision\"",ThongbaoRevision.of(created),"maxlength=\"255\"","maxlength=\"10000\"");
        var get=mvc.perform(get("/thongbao/create").with(user("m15-chief").roles("TRUONGKHOA"))).andExpect(status().isOk()).andReturn();
        assertThat(get.getResponse().getContentAsString()).contains("name=\"submissionToken\"","name=\"_csrf\"");
        String student=mvc.perform(get("/thongbao/"+created.id()).with(user("m15-student").roles("SINHVIEN"))).andReturn().getResponse().getContentAsString();
        assertThat(student).doesNotContain("id=\"deleteDetailForm\"","title=\"Chỉnh sửa thông báo này\"");
    }
    @Test void repeatedDeleteAndEditingADeletedNoticeCannotRecreateIt() throws Exception {
        var created=service.create(form("Xóa")); service.deleteById(created.id());
        mvc.perform(post("/thongbao/"+created.id()+"/delete").with(user("m15-chief").roles("TRUONGKHOA")).with(csrf())).andExpect(status().isNotFound());
        assertThat(edit(created.id(),ThongbaoRevision.of(created),"Không khôi phục")).isEqualTo(404);
        assertThat(notices.count()).isZero(); assertThat(emails.count()).isZero();
    }
    @Test void simultaneousEditAndDeleteCannotResurrectTheNoticeOrDuplicateOutbox() throws Exception {
        var created=service.create(form("Đồng thời sửa và xóa"));
        Callable<Integer> update=()->edit(created.id(),ThongbaoRevision.of(created),"Đã sửa");
        Callable<Integer> delete=()->mvc.perform(post("/thongbao/"+created.id()+"/delete").with(user("m15-chief").roles("TRUONGKHOA")).with(csrf()))
                .andReturn().getResponse().getStatus();
        var statuses=parallel(update,delete);
        assertThat(statuses.get(0)).isIn(302,404); assertThat(statuses.get(1)).isEqualTo(302);
        assertThat(notices.count()).isZero(); assertThat(emails.count()).isZero();
    }
    <T> List<T> parallel(Callable<T> a,Callable<T> b) throws Exception {
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            var one=pool.submit(()->{start.await();return a.call();}); var two=pool.submit(()->{start.await();return b.call();});
            start.countDown(); return List.of(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS));
        }
    }
}
