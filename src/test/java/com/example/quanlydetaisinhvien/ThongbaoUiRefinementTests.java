package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.util.regex.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:module15-ui-2e;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never",
    "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"})
@Import(ThongbaoWorkflowTests.NoDelivery.class)
class ThongbaoUiRefinementTests {
    static final String WARNING="Bạn đang thay đổi đối tượng nhận thông báo. Những đối tượng bị loại sẽ không còn xem được thông báo này. Bạn có chắc chắn muốn lưu thay đổi?";
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ThongbaoService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @Autowired DataSource data;
    MockMvc mvc;ThongbaoViewDTO notice;
    @BeforeEach void fixture() throws Exception {
        try(var c=data.getConnection()){assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:module15-ui-2e");}
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            for(var type:List.of("ThongbaoRead","ThongbaoReadActivation","Thongbao","Taikhoan"))em.createQuery("delete from "+type).executeUpdate();
            for(var role:Vaitro.values()){
                var account=new Taikhoan();account.setTendangnhap("ui-"+role.name());account.setMatkhau("H2-only");account.setHoten("Họ tên "+role.name());account.setVaitro(role);account.setKichhoat(true);em.persist(account);
            }
            em.persist(new ThongbaoReadActivation(0,java.time.LocalDateTime.now()));
        });
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("ui-TRUONGKHOA",null,List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
        var form=new ThongbaoFormDTO();form.setTieude("Thông báo để nghiệm thu UI");form.setNoidung("Nội dung giữ nguyên");form.setRecipientRoles(DoituongThongbao.all());notice=service.create(form);
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    String page(String url,String role) throws Exception {
        return mvc.perform(get(url).with(user("ui-"+role).roles(role))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    String hidden(String html,String name){var tag=Pattern.compile("<input\\b[^>]*name=\""+name+"\"[^>]*>").matcher(html);assertThat(tag.find()).isTrue();var value=Pattern.compile("value=\"([^\"]*)\"").matcher(tag.group());assertThat(value.find()).isTrue();return value.group(1);}
    @ParameterizedTest @ValueSource(strings={"TRUONGKHOA","SINHVIEN","GIANGVIEN","TRUONGBOMON"})
    void allRolesGetTrimmedHeaderAndSidebarButOnlyRecipientsKeepTheWorkingBell(String role) throws Exception {
        for(var url:List.of("/thongbao","/thongbao/"+notice.id())){
            var html=page(url,role);var header=Pattern.compile("(?s)<header\\b.*?</header>").matcher(html);assertThat(header.find()).isTrue();
            assertThat(header.group()).contains("TRƯỜNG ĐẠI HỌC CÔNG NGHỆ KỸ THUẬT TP.HCM").doesNotContain("Ngôn ngữ","🇻🇳","header-avatar");
            if(role.equals("TRUONGKHOA")) assertThat(header.group()).doesNotContain("headerNoticeBtn","headerNoticeDropdown","headerNoticeBadge","portal-header-tools");
            else assertThat(header.group()).contains("headerNoticeBtn","headerNoticeDropdown","headerNoticeBadge",">1</span>","notice-item-unread");
            assertThat(html).contains("src=\"/images/hcmute-logo.png\"","class=\"profile-avatar\"","href=\"/thongbao\"")
                    .doesNotContain("<strong>HCM-UTE</strong>","Thông tin và thông báo chính thức từ Khoa và Nhà trường.","Nội dung thông báo chính thức được đăng trên cổng thông tin.");
        }
        if(!role.equals("TRUONGKHOA"))mvc.perform(get("/thongbao/chuong").with(user("ui-"+role).roles(role))).andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1));
        assertThat(jdbc.queryForObject("select count(*) from emailketqua",Long.class)).isZero();
    }
    @Test void createAndEditFormsRemoveOnlyRequestedCopyAndRenderConfirmationOnlyForEdit() throws Exception {
        for(var url:List.of("/thongbao/create","/thongbao/"+notice.id()+"/edit")){
            var html=page(url,"TRUONGKHOA");
            assertThat(html).doesNotContain("Thông báo hiển thị trên website cho các đối tượng đã chọn; không gửi email.","Chọn ít nhất một đối tượng. Trưởng khoa quản lý thông báo, không thuộc danh sách nhận.","recipientRolesHint");
            assertThat(html).contains("data-audience-all","name=\"recipientRoles\"","name=\"_csrf\"","maxlength=\"255\"","maxlength=\"10000\"");
            if(url.endsWith("/edit")){
                assertThat(html).contains("<dialog","id=\"noticeAudienceDialog\"",WARNING,"Hủy bỏ","Xác nhận lưu",ThongbaoRevision.of(notice));
                assertThat(hidden(html,"originalRecipientRoles").split(",")).containsExactlyInAnyOrder("SINHVIEN","GIANGVIEN","TRUONGBOMON");
            }else assertThat(html).doesNotContain("id=\"noticeAudienceDialog\"");
        }
    }
    @Test void validationRedisplayRetainsOriginalAudienceAndUserInput() throws Exception {
        var original=hidden(page("/thongbao/"+notice.id()+"/edit","TRUONGKHOA"),"originalRecipientRoles");
        var result=mvc.perform(post("/thongbao/"+notice.id()+"/edit").with(user("ui-TRUONGKHOA").roles("TRUONGKHOA")).with(csrf())
                .param("tieude","").param("noidung","Nội dung đang sửa").param("recipientRoles","SINHVIEN")
                .param("revision",ThongbaoRevision.of(notice)).param("originalRecipientRoles",original))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("thongbaoForm","tieude")).andReturn();
        var html=result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(hidden(html,"originalRecipientRoles")).isEqualTo(original);assertThat(html).contains("Nội dung đang sửa");
        assertThat(((ThongbaoFormDTO)result.getModelAndView().getModel().get("thongbaoForm")).getRecipientRoles()).containsExactly(DoituongThongbao.SINHVIEN);
    }
    @Test void noAudienceStillFailsAndPresentationMetadataCannotBypassRevisionOrPermissions() throws Exception {
        mvc.perform(post("/thongbao/"+notice.id()+"/edit").with(user("ui-TRUONGKHOA").roles("TRUONGKHOA")).with(csrf())
                .param("tieude","Không ghi").param("noidung","Nội dung").param("revision",ThongbaoRevision.of(notice)).param("originalRecipientRoles","SINHVIEN"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("thongbaoForm","recipientRoles"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("ui-TRUONGKHOA",null,List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
        var change=new ThongbaoFormDTO();change.setTieude("Bản mới nhất");change.setNoidung(notice.noidung());change.setRecipientRoles(Set.of(DoituongThongbao.SINHVIEN));service.update(notice.id(),change);
        mvc.perform(post("/thongbao/"+notice.id()+"/edit").with(user("ui-TRUONGKHOA").roles("TRUONGKHOA")).with(csrf())
                .param("tieude","Không ghi đè").param("noidung","Nội dung").param("recipientRoles","GIANGVIEN")
                .param("revision",ThongbaoRevision.of(notice)).param("originalRecipientRoles","SINHVIEN,GIANGVIEN,TRUONGBOMON"))
                .andExpect(status().isConflict()).andExpect(model().attribute("originalRecipientRoles","GIANGVIEN,SINHVIEN,TRUONGBOMON"));
        mvc.perform(post("/thongbao/"+notice.id()+"/edit").with(user("ui-SINHVIEN").roles("SINHVIEN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/thongbao/"+notice.id()+"/edit").with(user("ui-TRUONGKHOA").roles("TRUONGKHOA"))).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select Tieude from thongbao where Id=?",String.class,notice.id())).isEqualTo("Bản mới nhất");
    }
}
