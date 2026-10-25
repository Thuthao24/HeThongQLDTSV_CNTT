package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.*;
import com.example.quanlydetaisinhvien.service.mail.ResultMailSender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.io.*;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Structural removal, fresh-schema operation and immutable legacy history. H2 and fake HTTP only. */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:notice-mail-removal;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never",
    "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false",
    // Untrusted leftover deployment flags must not recreate the deleted feature.
    "app.thongbao.email.outbox-enabled=true","app.thongbao.email.delivery-enabled=true","app.thongbao.email.schedule-enabled=true"
})
@Import(NoticeEmailRemovalTests.NoNetwork.class)
class NoticeEmailRemovalTests {
    @TestConfiguration(proxyBeanMethods=false) static class NoNetwork {
        @Bean @Primary ResultMailSender removedNoticeFakeTransport() { return mock(ResultMailSender.class); }
    }
    @Autowired WebApplicationContext context;
    @Autowired EntityManager em;
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ThongbaoService notices;
    @Autowired ThongbaoReadService reads;
    @Autowired EmailketquaRepository resultEmails;
    @Autowired ResultMailSender client;
    MockMvc mvc;
    @BeforeEach void fixture() throws Exception {
        try(var connection=source.getConnection()) {assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:notice-mail-removal");}
        reset(client);
        new TransactionTemplate(transactions).executeWithoutResult(status->{
            for(String entity:List.of("ThongbaoRead","ThongbaoReadActivation","Thongbao","Sinhvien","Taikhoan"))em.createQuery("delete from "+entity).executeUpdate();
            for(var role:List.of(Vaitro.TRUONGKHOA,Vaitro.SINHVIEN,Vaitro.GIANGVIEN,Vaitro.TRUONGBOMON)) {
                var account=new Taikhoan();account.setTendangnhap("removal-"+role);account.setHoten(role.name());
                account.setMatkhau("H2-only");account.setKichhoat(true);account.setVaitro(role);account.setEmail(role.name().toLowerCase()+"@example.invalid");em.persist(account);
                if(role==Vaitro.SINHVIEN){var student=new Sinhvien();student.setTaikhoanid(account.getId());student.setBomonid(1L);student.setMasinhvien("H2-17C");em.persist(student);}
            }
            em.persist(new ThongbaoReadActivation(0,LocalDateTime.of(2026,10,24,8,0)));
        });
        auth("TRUONGKHOA");mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void cleanup() {verifyNoInteractions(client);SecurityContextHolder.clearContext();}
    void auth(String role){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("removal-"+role,null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
    ThongbaoFormDTO form(String title,Set<DoituongThongbao> roles){var f=new ThongbaoFormDTO();f.setTieude(title);f.setNoidung("Nội dung tiếng Việt chỉ trên website");f.setRecipientRoles(roles);return f;}
    boolean legacyTable(){return jdbc.queryForObject("select count(*) from information_schema.tables where upper(table_name)='EMAILTHONGBAO'",Long.class)>0;}

    @Test void noRetiredClassBeanEntityOrScheduledComponentRemains() throws Exception {
        assertThat(legacyTable()).isFalse();
        assertThat(em.getMetamodel().getEntities()).extracting(t->t.getName()).doesNotContain("Emailthongbao");
        for(String name:List.of("config.NotificationEmailConfig","config.NotificationEmailSettings","config.NotificationMailTestDatabaseConfig",
                "controller.morong.NotificationEmailController","controller.morong.NotificationEmailPageController",
                "service.notification.NotificationEmailWorker","service.notification.NotificationEmailScheduler","service.notification.NotificationEmailOutbox",
                "repository.EmailthongbaoRepository","entity.Emailthongbao", "service.notification.SmtpNotificationMailSender"))
            assertThat(org.springframework.util.ClassUtils.isPresent("com.example.quanlydetaisinhvien."+name,getClass().getClassLoader())).as(name).isFalse();
        assertThat(context.getBeanDefinitionNames()).noneMatch(n->n.toLowerCase(Locale.ROOT).startsWith("notificationemail") || n.equals("mailjetNotificationMailSender"));
        assertThat(context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().keySet()).allSatisfy(mapping->
                assertThat(mapping.getPatternValues()).noneMatch(path->path.startsWith("/morong/email-thongbao")));
        for(String resource:List.of("templates/morong/email-thongbao.html","static/css/morong-email-thongbao.css","static/js/morong-email-thongbao.js",
                "application-demo-auto.properties","application-notification-mail-test.properties"))assertThat(new ClassPathResource(resource).exists()).as(resource).isFalse();
        for(String profile:List.of("application.properties","application-demo.properties")) {
            var properties=PropertiesLoaderUtils.loadProperties(new ClassPathResource(profile));
            assertThat(properties.stringPropertyNames()).noneMatch(k->k.startsWith("app.thongbao.email"));
        }
    }
    @Test void crudAudienceAndUnreadReceiptsWorkWithoutAnyNoticeEmailSchema() throws Exception {
        assertThat(legacyTable()).isFalse();var saved=notices.create(form("Thông báo 17C",Set.of(DoituongThongbao.SINHVIEN)));
        auth("SINHVIEN");assertThat(reads.bell().unreadCount()).isEqualTo(1);reads.markRead(saved.id());assertThat(reads.bell().unreadCount()).isZero();
        auth("GIANGVIEN");assertThat(reads.bell().items()).isEmpty();
        auth("TRUONGKHOA");var update=form("Đã chỉnh sửa",DoituongThongbao.all());update.setRevision(ThongbaoRevision.of(saved));
        var edited=notices.update(saved.id(),update);assertThat(edited.ngaydang()).isEqualTo(saved.ngaydang());
        auth("GIANGVIEN");assertThat(reads.bell().unreadCount()).isEqualTo(1);
        auth("TRUONGKHOA");notices.deleteById(saved.id());assertThat(notices.findAll()).isEmpty();
        assertThat(resultEmails.count()).isZero();assertThat(legacyTable()).isFalse();
    }
    @Test void deployedLegacyTableAndItsHistoryRemainUntouchedByCreateEditAndRead() throws Exception {
        var old=notices.create(form("Thông báo lịch sử",DoituongThongbao.all()));
        String sql=Files.readString(Path.of("database/migrations/pending/V003__emailthongbao.sql"))
                .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci","");
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(source);
        try {
            for(String state:List.of("PENDING","SENDING","SENT","FAILED","UNCERTAIN","MISSING_EMAIL"))
                jdbc.update("insert into emailthongbao(Thongbaoid,Thongbaosnapshotid,Sinhvienid,Nguoinhan,Email,Tieude,Noidung,Trangthai,Ngaytao) values(?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                        old.id(),old.id(),1000L+state.length()+state.hashCode(),"Lịch sử", "history@example.invalid", "Snapshot", "Không xử lý",state);
            var before=jdbc.queryForList("select * from emailthongbao order by Id");
            var created=notices.create(form("Thông báo mới",DoituongThongbao.all()));
            var updated=form("Thông báo đã sửa",DoituongThongbao.all());notices.update(created.id(),updated);
            auth("SINHVIEN");reads.bell();reads.markRead(created.id());
            auth("TRUONGKHOA");notices.deleteById(created.id());
            assertThat(jdbc.queryForList("select * from emailthongbao order by Id")).isEqualTo(before);
            assertThat(resultEmails.count()).isZero();
        } finally {jdbc.execute("drop table emailthongbao");} // H2 test only; no production migration executes.
    }
    @ParameterizedTest @ValueSource(strings={"TRUONGKHOA","TRUONGBOMON","GIANGVIEN","SINHVIEN"})
    void retiredMonitorApiAndRetryAreUnmappedAndDeniedEvenToTheChief(String role) throws Exception {
        for(String path:List.of("/morong/email-thongbao","/morong/email-thongbao/quanly"))
            mvc.perform(get(path).with(user("removal-"+role).roles(role))).andExpect(status().isForbidden());
        mvc.perform(post("/morong/email-thongbao/1/thulai").with(user("removal-"+role).roles(role)).with(csrf())).andExpect(status().isForbidden());
        for(String path:List.of("/css/morong-email-thongbao.css","/js/morong-email-thongbao.js"))
            mvc.perform(get(path).with(user("removal-"+role).roles(role))).andExpect(result ->
                    assertThat(result.getResolvedException()).isInstanceOf(org.springframework.web.servlet.resource.NoResourceFoundException.class));
    }
    @Test void sidebarDropsOnlyTheRetiredMenuWhileNoticeBellAndModule17LinksRemain() throws Exception {
        String chief=mvc.perform(get("/thongbao").with(user("removal-TRUONGKHOA").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(chief).contains("/morong/thongke","/detai","TIỆN ÍCH").doesNotContain("/morong/timkiem/detai","/morong/timkiem/dotdangky","/morong/timkiem/ketqua","Giám sát Email","/morong/email-thongbao");
        String student=mvc.perform(get("/thongbao").with(user("removal-SINHVIEN").roles("SINHVIEN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(student).contains("headerNoticeBadge","/js/thongbao-bell.js").doesNotContain("Giám sát Email","/morong/email-thongbao");
    }
}
