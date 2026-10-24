package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:module15-read;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never",
    "app.thongbao.email.outbox-enabled=true","app.thongbao.email.delivery-enabled=true","app.thongbao.email.schedule-enabled=true",
    "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"})
@Import(ThongbaoWorkflowTests.NoDelivery.class)
class ThongbaoReadTests {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ThongbaoService notices;
    @Autowired ThongbaoReadService reads;
    @Autowired ThongbaoReadRepository receipts;
    @Autowired ThongbaoReadActivationRepository activation;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    @Autowired DataSource data;
    MockMvc mvc;TransactionTemplate tx;Long historical;long boundary;
    @BeforeEach void fixture() throws Exception {
        try(var c=data.getConnection()){assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:module15-read");}
        tx=new TransactionTemplate(transactions);
        tx.executeWithoutResult(s->{
            for(String type:List.of("ThongbaoRead","ThongbaoReadActivation","Emailthongbao","Thongbao","Taikhoan")) em.createQuery("delete from "+type).executeUpdate();
            account("read-chief",Vaitro.TRUONGKHOA);account("read-student",Vaitro.SINHVIEN);account("read-student2",Vaitro.SINHVIEN);
            account("read-lecturer",Vaitro.GIANGVIEN);account("read-head",Vaitro.TRUONGBOMON);
            var old=new Thongbao();old.setTieude("Lịch sử");old.setNoidung("Không tính chưa đọc");old.setNguoidangid(accountId("read-chief"));old.setNgaydang(LocalDateTime.of(2025,1,1,8,0));em.persist(old);em.flush();historical=old.getId();
            boundary=historical;em.persist(new ThongbaoReadActivation(boundary,LocalDateTime.now()));
        });
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();auth("read-chief","TRUONGKHOA");
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void account(String name,Vaitro role){var a=new Taikhoan();a.setTendangnhap(name);a.setMatkhau("H2-only");a.setHoten("Họ tên "+name);a.setVaitro(role);a.setKichhoat(true);em.persist(a);}
    Long accountId(String name){return em.createQuery("select a.id from Taikhoan a where a.tendangnhap=:name",Long.class).setParameter("name",name).getSingleResult();}
    void auth(String name,String role){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(name,null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));}
    ThongbaoViewDTO create(int mask){auth("read-chief","TRUONGKHOA");var form=new ThongbaoFormDTO();form.setTieude("Thông báo "+UUID.randomUUID());form.setNoidung("Nội dung tiếng Việt");var roles=EnumSet.noneOf(DoituongThongbao.class);for(var role:DoituongThongbao.values())if((mask&(1<<role.ordinal()))!=0)roles.add(role);form.setRecipientRoles(roles);return notices.create(form);}
    long count(String name,String role){auth(name,role);return reads.bell().unreadCount();}
    @ParameterizedTest @ValueSource(ints={1,2,3,4,5,6,7})
    void everyAudienceCountsOnlyItsRolesAndNeverTheChief(int mask){
        create(mask);assertThat(receipts.count()).isZero();
        assertThat(count("read-student","SINHVIEN")).isEqualTo((mask&1)!=0?1:0);
        assertThat(count("read-lecturer","GIANGVIEN")).isEqualTo((mask&2)!=0?1:0);
        assertThat(count("read-head","TRUONGBOMON")).isEqualTo((mask&4)!=0?1:0);
        auth("read-chief","TRUONGKHOA");assertThat(reads.bell()).isEqualTo(new ThongbaoBellDTO(0,List.of()));
        assertThat(jdbc.queryForObject("select count(*) from emailthongbao",Long.class)).isZero();
    }
    @Test void independentAccountsDropdownAndDetailGetNeverWriteButPostDecrementsExactlyOnce() throws Exception {
        var notice=create(5);var role=user("read-student").roles("SINHVIEN");
        for(int i=0;i<2;i++)mvc.perform(get("/thongbao/chuong").with(role)).andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1)).andExpect(header().string("Cache-Control","no-store"));
        var detail=mvc.perform(get("/thongbao/"+notice.id()).with(role)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(detail).contains("id=\"noticeMarkReadForm\"","name=\"_csrf\"","/js/thongbao-bell.js","id=\"headerNoticeBadge\"");
        assertThat(receipts.count()).isZero();
        mvc.perform(post("/thongbao/"+notice.id()+"/dadoc").with(role)).andExpect(status().isForbidden());assertThat(receipts.count()).isZero();
        for(int i=0;i<2;i++)mvc.perform(post("/thongbao/"+notice.id()+"/dadoc").with(role).with(csrf()).param("Taikhoanid",String.valueOf(accountId("read-head"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
        assertThat(receipts.count()).isEqualTo(1);
        mvc.perform(get("/thongbao/chuong").with(role)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unread").value(false)).andExpect(jsonPath("$.unreadCount").value(0));
        var afterRead=mvc.perform(get("/thongbao").with(role)).andReturn().getResponse().getContentAsString();
        assertThat(java.util.regex.Pattern.compile("<span\\b[^>]*id=\"headerNoticeBadge\"[^>]*>").matcher(afterRead).results()
                .map(java.util.regex.MatchResult::group).findFirst().orElseThrow()).contains("hidden");
        assertThat(count("read-student2","SINHVIEN")).isEqualTo(1);assertThat(count("read-head","TRUONGBOMON")).isEqualTo(1);
        auth("read-head","TRUONGBOMON");reads.markRead(notice.id());assertThat(reads.bell().unreadCount()).isZero();
        assertThat(jdbc.queryForObject("select Taikhoanid from thongbaodadoc where Taikhoanid=?",Long.class,accountId("read-student"))).isEqualTo(accountId("read-student"));
    }
    @Test void countsAreNotLimitedToFiveAndBadgeCapsAt99() throws Exception {
        for(int i=0;i<101;i++)create(1);
        auth("read-student","SINHVIEN");var bell=reads.bell();assertThat(bell.unreadCount()).isEqualTo(101);assertThat(bell.items()).hasSize(5).allMatch(ThongbaoBellDTO.Item::unread);
        var html=mvc.perform(get("/thongbao").with(user("read-student").roles("SINHVIEN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains(">99+</span>","notice-item-unread");assertThat(receipts.count()).isZero();
    }
    @Test void unauthorizedMissingAndInvalidIdsCannotCreateReceiptsAndChiefGetsNoBadge() throws Exception {
        var notice=create(1);
        mvc.perform(post("/thongbao/"+notice.id()+"/dadoc").with(user("read-lecturer").roles("GIANGVIEN")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/thongbao/9999999/dadoc").with(user("read-student").roles("SINHVIEN")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(post("/thongbao/0/dadoc").with(user("read-student").roles("SINHVIEN")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/thongbao/abc/dadoc").with(user("read-student").roles("SINHVIEN")).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post("/thongbao/"+notice.id()+"/dadoc").with(user("read-chief").roles("TRUONGKHOA")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/thongbao/chuong").with(user("read-chief").roles("TRUONGKHOA"))).andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0)).andExpect(jsonPath("$.items").isEmpty());
        var chief=mvc.perform(get("/thongbao/"+notice.id()).with(user("read-chief").roles("TRUONGKHOA"))).andReturn().getResponse().getContentAsString();
        assertThat(chief).doesNotContain("id=\"noticeMarkReadForm\"","data-notice-feed-url=");assertThat(receipts.count()).isZero();
    }
    @Test void changedAudienceAndDeletesRecomputeCountsWithoutPrecreatedRecipients(){
        var notice=create(1);auth("read-student","SINHVIEN");reads.markRead(notice.id());
        var updated=new ThongbaoFormDTO();updated.setTieude(notice.tieude());updated.setNoidung(notice.noidung());updated.setRecipientRoles(Set.of(DoituongThongbao.GIANGVIEN));updated.setRevision(ThongbaoRevision.of(notice));
        auth("read-chief","TRUONGKHOA");notices.update(notice.id(),updated);
        assertThat(count("read-student","SINHVIEN")).isZero();assertThat(count("read-lecturer","GIANGVIEN")).isEqualTo(1);
        auth("read-chief","TRUONGKHOA");notices.deleteById(notice.id());assertThat(receipts.count()).isZero();
        assertThat(count("read-lecturer","GIANGVIEN")).isZero();
    }
    @Test void originalReadSurvivesAudienceRemovalAndReadditionAndUnreadDeletesDecreaseCounts(){
        var notice=create(1);auth("read-student","SINHVIEN");reads.markRead(notice.id());
        auth("read-chief","TRUONGKHOA");var f=new ThongbaoFormDTO();f.setTieude(notice.tieude());f.setNoidung(notice.noidung());f.setRecipientRoles(Set.of(DoituongThongbao.GIANGVIEN));notices.update(notice.id(),f);
        f.setRecipientRoles(Set.of(DoituongThongbao.SINHVIEN));notices.update(notice.id(),f);assertThat(count("read-student","SINHVIEN")).isZero();
        var unread=create(1);assertThat(count("read-student","SINHVIEN")).isEqualTo(1);
        auth("read-chief","TRUONGKHOA");notices.deleteById(unread.id());assertThat(count("read-student","SINHVIEN")).isZero();
    }
    @Test void persistentBoundaryIgnoresHistoryAndNewServiceInstanceDoesNotResetIt(){
        assertThat(count("read-student","SINHVIEN")).isZero();reads.markRead(historical);assertThat(receipts.count()).isZero();
        create(7);long stored=jdbc.queryForObject("select Thongbaoidmoc from thongbaodocmoc where Id=1",Long.class);
        var restarted=new ThongbaoReadService(receipts,activation,context.getBean(ThongbaoRepository.class),context.getBean(TaikhoanRepository.class),notices);
        auth("read-student","SINHVIEN");assertThat(restarted.bell().unreadCount()).isEqualTo(1);assertThat(stored).isEqualTo(boundary);
        assertThat(jdbc.queryForObject("select Thongbaoidmoc from thongbaodocmoc where Id=1",Long.class)).isEqualTo(boundary);
    }
    @Test void concurrentReadPostsAreIdempotentAndKeepFirstReadTime() throws Exception {
        var notice=create(1);
        try(var pool=Executors.newFixedThreadPool(4)){
            var gate=new CountDownLatch(1);var tasks=new ArrayList<Future<Integer>>();
            for(int i=0;i<4;i++)tasks.add(pool.submit(()->{gate.await();return mvc.perform(post("/thongbao/"+notice.id()+"/dadoc").with(user("read-student").roles("SINHVIEN")).with(csrf())).andReturn().getResponse().getStatus();}));
            gate.countDown();for(var task:tasks)assertThat(task.get(15,TimeUnit.SECONDS)).isEqualTo(200);
        }
        var before=jdbc.queryForList("select * from thongbaodadoc");assertThat(before).hasSize(1);
        auth("read-student","SINHVIEN");reads.markRead(notice.id());assertThat(jdbc.queryForList("select * from thongbaodadoc")).isEqualTo(before);
    }
    @Test void missingActivationFailsClosedAndDoesNotSilentlyStartANewEpoch() throws Exception {
        var notice=create(1);tx.executeWithoutResult(s->activation.deleteAll());
        mvc.perform(get("/thongbao/chuong").with(user("read-student").roles("SINHVIEN"))).andExpect(status().isServiceUnavailable());
        mvc.perform(post("/thongbao/"+notice.id()+"/dadoc").with(user("read-student").roles("SINHVIEN")).with(csrf())).andExpect(status().isServiceUnavailable());
        assertThat(activation.count()).isZero();assertThat(receipts.count()).isZero();
    }
    @Test void staleAccountRoleOrDisabledAccountCannotReadAnotherRoleOrMark() throws Exception {
        var notice=create(1);tx.executeWithoutResult(s->em.createQuery("update Taikhoan a set a.kichhoat=false where a.tendangnhap='read-student'").executeUpdate());
        mvc.perform(post("/thongbao/"+notice.id()+"/dadoc").with(user("read-student").roles("SINHVIEN")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/thongbao/chuong").with(user("read-head").roles("SINHVIEN"))).andExpect(status().isForbidden());
        assertThat(receipts.count()).isZero();
    }
}
