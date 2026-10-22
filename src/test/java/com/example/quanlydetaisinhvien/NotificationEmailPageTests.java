package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.ThongbaoViewDTO;
import com.example.quanlydetaisinhvien.entity.*;
import jakarta.persistence.EntityManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses real Thymeleaf templates/fragments; no backend-only view resolver or MySQL connection. */
@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:notification-email-page;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.mail.test-connection=false",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.example.quanlydetaisinhvien.NotificationEmailPageTests$ReadOnlySqlInspector",
        "app.thongbao.email.outbox-enabled=false","app.thongbao.email.delivery-enabled=false",
        "app.thongbao.email.schedule-enabled=false","app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"
})
class NotificationEmailPageTests {
    private static final String PAGE="/morong/email-thongbao/quanly";
    private static final String API="/morong/email-thongbao";
    private static final List<String> TABLES=List.of("emailthongbao","thongbao","emailketqua","ketqua","taikhoan");
    @Autowired DataSource source;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    Long chiefId, noticeId;

    public static class ReadOnlySqlInspector implements StatementInspector {
        static final List<String> statements=new CopyOnWriteArrayList<>();
        @Override public String inspect(String sql) { statements.add(sql); return sql; }
    }

    @BeforeEach void h2Fixture() throws Exception {
        try(var connection=source.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:notification-email-page");
        }
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            for(String entity:List.of("Emailthongbao","Thongbao","Emailketqua","Ketqua","Taikhoan"))
                em.createQuery("delete from "+entity).executeUpdate();
            chiefId=account("page-chief",Vaitro.TRUONGKHOA,true,"Đặng Trưởng khoa").getId();
            account("page-inactive",Vaitro.TRUONGKHOA,false,"Trưởng khoa ngừng hoạt động");
            account("page-head",Vaitro.TRUONGBOMON,true,"Trưởng bộ môn");
            account("page-teacher",Vaitro.GIANGVIEN,true,"Giảng viên");
            account("page-student",Vaitro.SINHVIEN,true,"Sinh viên");
            var posted=LocalDateTime.of(2026,10,16,8,0);
            for(int i=0;i<7;i++) {
                var notice=new Thongbao(); notice.setNguoidangid(chiefId); notice.setNgaydang(posted);
                notice.setTieude("Thông báo header "+i); notice.setNoidung("Nội dung H2"); em.persist(notice);
                noticeId=notice.getId();
            }
            for(var state:List.of(TrangthaiEmailthongbao.PENDING,TrangthaiEmailthongbao.MISSING_EMAIL)) {
                var email=new Emailthongbao(); email.setThongbaoid(noticeId); email.setThongbaoSnapshotId(noticeId);
                email.setSinhvienid(state==TrangthaiEmailthongbao.PENDING?901L:902L);
                email.setNguoinhan("Người nhận snapshot"); email.setEmail(state==TrangthaiEmailthongbao.PENDING?"page@example.invalid":null);
                email.setTieude("Email chỉ giám sát"); email.setNoidung("Không đưa body này ra monitor");
                email.setNgaytao(posted); email.setTrangthai(state); em.persist(email);
            }
            var result=new Ketqua(); result.setDangkydetaiid(800L); result.setDacongbo(true); em.persist(result);
            var resultEmail=new Emailketqua(); resultEmail.setKetquaid(result.getId()); resultEmail.setSinhvienid(901L);
            resultEmail.setEmail("result@example.invalid"); resultEmail.setTieude("Emailketqua PENDING cũ");
            resultEmail.setNoidung("Không thay đổi"); resultEmail.setNgaytao(posted);
            resultEmail.setTrangthai(TrangthaiEmailketqua.PENDING); em.persist(resultEmail);
            em.flush();
        });
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    Taikhoan account(String username,Vaitro role,boolean active,String name) {
        var account=new Taikhoan(); account.setTendangnhap(username); account.setHoten(name);
        account.setMatkhau("unused-H2-password"); account.setVaitro(role); account.setKichhoat(active);
        em.persist(account); return account;
    }

    MvcResult page() throws Exception {
        return mvc.perform(get(PAGE).with(user("page-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("morong/email-thongbao"))
                .andExpect(model().attribute("title","Giám sát Email thông báo"))
                .andExpect(model().attribute("hoten","Đặng Trưởng khoa"))
                .andExpect(model().attribute("tendangnhap","page-chief"))
                .andExpect(model().attribute("vaitro","TRUONGKHOA"))
                .andExpect(model().attribute("headerThongbaos",hasSize(5))).andReturn();
    }

    String html(MvcResult result) throws Exception { return result.getResponse().getContentAsString(StandardCharsets.UTF_8); }

    @Test void activeChiefGetsRealThymeleafWithPortalHeaderBellSidebarAndAccount() throws Exception {
        var result=page();
        assertThat(html(result)).contains("<!DOCTYPE html>","GIÁM SÁT EMAIL THÔNG BÁO",
                "class=\"portal-header\"","id=\"headerNoticeBtn\"","id=\"headerNoticeDropdown\"",
                "id=\"sidebar\"","Đặng Trưởng khoa","page-chief","TRUONGKHOA")
                .containsPattern("class=\"header-avatar\"[^>]*>Đ</div>")
                .doesNotContain("th:replace=","th:href=","TemplateInputException");
        @SuppressWarnings("unchecked")
        var notices=(List<ThongbaoViewDTO>)result.getModelAndView().getModel().get("headerThongbaos");
        assertThat(notices).extracting(ThongbaoViewDTO::id).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(notices).extracting(ThongbaoViewDTO::tieude).containsExactly(
                "Thông báo header 6","Thông báo header 5","Thông báo header 4","Thông báo header 3","Thông báo header 2");
        assertThat(html(result)).contains("href=\"/thongbao/"+noticeId+"\"","href=\"/thongbao\"",
                "16/10/2026 08:00").doesNotContain("Thông báo header 0","Thông báo header 1");
    }

    @Test void pageLoadsExistingSharedAndDedicatedStylesAndScripts() throws Exception {
        var rendered=html(page());
        for(String path:List.of("/css/app.css","/css/morong-email-thongbao.css","/js/app.js","/js/morong-email-thongbao.js")) {
            assertThat(rendered).contains((path.endsWith(".css")?"href=\"":"src=\"")+path+"\"");
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(result->assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty());
        }
    }

    @ParameterizedTest @ValueSource(strings={"TRUONGBOMON","GIANGVIEN","SINHVIEN"})
    void otherRolesAreForbiddenFromHtmlAndJson(String role) throws Exception {
        String name=switch(role) { case "TRUONGBOMON"->"page-head"; case "GIANGVIEN"->"page-teacher"; default->"page-student"; };
        for(String path:List.of(PAGE,API))
            mvc.perform(get(path).with(user(name).roles(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest @ValueSource(strings={"page-inactive","page-teacher","missing-account"})
    void chiefAuthorityRequiresAnActiveChiefAccountInDatabase(String username) throws Exception {
        for(String path:List.of(PAGE,API))
            mvc.perform(get(path).with(user(username).roles("TRUONGKHOA"))).andExpect(status().isForbidden());
    }

    @Test void anonymousHtmlAndJsonRequestsRedirectToLogin() throws Exception {
        for(String path:List.of(PAGE,API))
            mvc.perform(get(path).with(anonymous())).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
    }

    @Test void htmlAndApiMappingsStaySeparateAndJsonContractIsUnchanged() throws Exception {
        mvc.perform(get(PAGE).accept(MediaType.APPLICATION_JSON).with(user("page-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name("morong/email-thongbao"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        mvc.perform(get(API).with(user("page-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.schemaAvailable").value(true)).andExpect(jsonPath("$.outboxEnabled").value(false))
                .andExpect(jsonPath("$.deliveryEnabled").value(false)).andExpect(jsonPath("$.counts.total").value(2))
                .andExpect(jsonPath("$.counts.pending").value(1)).andExpect(jsonPath("$.counts.missingEmail").value(1))
                .andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.pageInfo.number").value(0))
                .andExpect(jsonPath("$.pageInfo.size").value(20)).andExpect(jsonPath("$.pageInfo.totalElements").value(2))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].thongbaoSnapshotId").value(noticeId))
                .andExpect(jsonPath("$.items[0].noidung").doesNotExist())
                .andExpect(result->assertThat(result.getModelAndView()).isNull());
        mvc.perform(get(API).with(user("page-chief").roles("TRUONGKHOA"))
                        .param("thongbaoid",noticeId.toString()).param("trangthai","PENDING").param("page","0").param("size","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.counts.total").value(1))
                .andExpect(jsonPath("$.items[0].trangthai").value("PENDING"))
                .andExpect(jsonPath("$.items[0].sinhvienid").value(901))
                .andExpect(jsonPath("$.items[0].email").value("page@example.invalid"));
        mvc.perform(get(API).with(user("page-chief").roles("TRUONGKHOA")).param("size","101"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
    }

    @Test void missingOutboxTableStillRendersRealPortalAndJsonReportsUnavailable() throws Exception {
        // Only the isolated, URL-verified H2 fixture is changed; no migration file or MySQL is used.
        var before=snapshots();
        jdbc.execute("alter table emailthongbao rename to emailthongbao_unavailable");
        try {
            ReadOnlySqlInspector.statements.clear();
            assertThat(html(page())).contains("id=\"schemaUnavailableAlert\"","id=\"sidebar\"","id=\"headerNoticeBtn\"");
            mvc.perform(get(API).with(user("page-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.schemaAvailable").value(false))
                    .andExpect(jsonPath("$.outboxEnabled").value(false)).andExpect(jsonPath("$.deliveryEnabled").value(false))
                    .andExpect(jsonPath("$.counts.total").value(0)).andExpect(jsonPath("$.items.length()").value(0));
            assertThat(ReadOnlySqlInspector.statements).isNotEmpty().allMatch(sql->
                    sql.stripLeading().toLowerCase(Locale.ROOT).startsWith("select"));
        } finally { jdbc.execute("alter table emailthongbao_unavailable rename to emailthongbao"); }
        assertThat(snapshots()).isEqualTo(before);
    }

    @Test void getHtmlAndJsonExecuteNoMutationSqlAndPreserveAllBusinessAndQueueSnapshots() throws Exception {
        var before=snapshots(); ReadOnlySqlInspector.statements.clear();
        page();
        mvc.perform(get(API).with(user("page-chief").roles("TRUONGKHOA"))).andExpect(status().isOk());
        assertThat(ReadOnlySqlInspector.statements).isNotEmpty().allMatch(sql->
                sql.stripLeading().toLowerCase(Locale.ROOT).startsWith("select"));
        assertThat(snapshots()).isEqualTo(before);
    }

    @Test void emptyHeaderStillRendersThePortalWithoutRequiringAnyNotice() throws Exception {
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            em.createQuery("delete from Emailthongbao").executeUpdate(); em.createQuery("delete from Thongbao").executeUpdate();
        });
        var result=mvc.perform(get(PAGE).with(user("page-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(model().attribute("headerThongbaos",hasSize(0))).andReturn();
        assertThat(html(result)).contains("Chưa có thông báo nào","id=\"sidebar\"","class=\"header-avatar\"");
    }

    Map<String,List<Map<String,Object>>> snapshots() {
        var values=new LinkedHashMap<String,List<Map<String,Object>>>();
        for(String table:TABLES) values.put(table,jdbc.queryForList("select * from "+table+" order by Id"));
        return values;
    }
}
