package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.notification.NotificationEmailWorker;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
        "spring.datasource.url=jdbc:h2:mem:notification-missing-schema;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.mail.test-connection=false",
        "app.thongbao.email.outbox-enabled=true","app.thongbao.email.delivery-enabled=true",
        "app.thongbao.email.schedule-enabled=false","app.thongbao.email.mode=SYSTEM",
        "app.ketqua.email.enabled=false","app.ketqua.email.scheduling-enabled=false"
})
@Import({NotificationEmailTests.MailConfig.class,ThongbaoTestSupport.TestViews.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationEmailMissingSchemaTests {
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired WebApplicationContext context;
    @Autowired NotificationEmailWorker worker;
    @Autowired NotificationEmailTests.FakeSender fake;
    MockMvc mvc;

    @BeforeAll void removeOnlyNewTableInAnIsolatedH2Database() throws Exception {
        try(var c=source.getConnection()) {
            assertThat(c.getMetaData().getURL()).startsWith("jdbc:h2:mem:notification-missing-schema");
        }
        jdbc.execute("drop table emailthongbao");
        new TransactionTemplate(transactions).executeWithoutResult(s->{
            var chief=new Taikhoan(); chief.setTendangnhap("missing-chief"); chief.setHoten("Trưởng khoa H2");
            chief.setMatkhau("unused-H2"); chief.setVaitro(Vaitro.TRUONGKHOA); chief.setKichhoat(true); em.persist(chief);
        });
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void enabledOutboxWithMissingSchemaReturnsFormErrorAndRollsBackCreate() throws Exception {
        mvc.perform(post("/thongbao").with(user("missing-chief").roles("TRUONGKHOA")).with(csrf())
                        .param("tieude","Chưa triển khai bảng mới").param("noidung","Chỉ kiểm thử H2"))
                .andExpect(status().isOk()).andExpect(view().name("thongbao-form"))
                .andExpect(model().attributeHasErrors("thongbaoForm"))
                .andExpect(model().attributeExists("errorMessage"));
        assertThat(jdbc.queryForObject("select count(*) from thongbao",Long.class)).isZero();
        assertThat(fake.sent).isEmpty();
    }

    @Test void missingSchemaCannotReachTransportEvenWhenDeliveryFlagsAreTrue() throws Exception {
        worker.drain();
        mvc.perform(get("/morong/email-thongbao").with(user("missing-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.schemaAvailable").value(false))
                .andExpect(jsonPath("$.deliveryEnabled").value(false)).andExpect(jsonPath("$.items.length()").value(0));
        assertThat(fake.sent).isEmpty();
    }
}
