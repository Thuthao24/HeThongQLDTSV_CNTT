package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.Ketqua;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.KetquaService;
import com.example.quanlydetaisinhvien.service.mail.KetquaEmailWorker;
import com.example.quanlydetaisinhvien.service.mail.ResultMailSender;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/** Executes the delivered fixture/DDL only in H2, never starts the MySQL demo profile. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:demo-fixture;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=none",
    "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
    "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@Import(DemoFixtureTests.NoTransportConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoFixtureTests {
    @Autowired DataSource source;
    @Autowired JdbcTemplate jdbc;
    @Autowired KetquaService service;
    @Autowired KetquaEmailWorker worker;
    @Autowired com.example.quanlydetaisinhvien.service.morong.ThongkeService statistics;
    @Autowired PasswordEncoder encoder;
    @Autowired AtomicInteger deliveries;
    String fixture;

    @TestConfiguration(proxyBeanMethods = false)
    static class NoTransportConfig {
        @Bean AtomicInteger deliveries() { return new AtomicInteger(); }
        @Bean @Primary ResultMailSender noTransport(AtomicInteger deliveries) {
            return message -> { deliveries.incrementAndGet(); throw new AssertionError("Demo must never send mail"); };
        }
    }

    @BeforeAll void createH2SchemaFromDeliveredSql() throws Exception {
        try (var connection = source.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:demo-fixture");
        }
        String schema = read("01_schema.sql").replace("quanlydetaisinhvien_demo.", "")
                .replaceAll(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci", "");
        execute(schema);
        fixture = read("02_fixture_routine.sql").split("-- BEGIN DEMO DATA\\R", 2)[1]
                .split("-- END DEMO DATA", 2)[0].replace("quanlydetaisinhvien_demo.", "");
    }

    @BeforeEach void restoreFixture() {
        execute(fixture);
        deliveries.set(0);
        auth("TK001", "TRUONGKHOA");
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void sourceFixtureHasAllDependenciesAndExpectedCounts() {
        assertThat(count("taikhoan")).isEqualTo(46);
        assertThat(count("sinhvien")).isEqualTo(24);
        assertThat(count("dangkydetai")).isEqualTo(8);
        assertThat(count("diem")).isEqualTo(12);
        assertThat(count("emailketqua")).isZero();
        assertThat(service.findAll()).hasSize(8);
        assertThat(service.publicationInfo(service.findAll()).entrySet().stream()
                .filter(e -> e.getValue().reason().isEmpty()).map(e -> e.getKey()).toList())
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 7L);
        assertThat(jdbc.queryForList("select Dotdangkyid from nhomsinhvien order by Id", Long.class))
                .containsExactly(1L, 2L, 3L, 4L, 1L, 2L, 3L, 4L);
    }

    @Test void statisticsMatchesDeliveredDemoFixtureWithoutChangingResultsOrEmailOutbox() {
        var before = jdbc.queryForList("select * from ketqua order by Id");
        var report = statistics.statistics(new com.example.quanlydetaisinhvien.dto.morong.ThongkeFilterDTO());
        assertThat(report.summary()).isEqualTo(new com.example.quanlydetaisinhvien.dto.morong.StatisticsModels.Summary(
                8, 8, 8, 8, 1, 7, 0, 2, 0));
        assertThat(report.byRound()).hasSize(4).allSatisfy(row -> {
            assertThat(row.soDetai()).isEqualTo(2); assertThat(row.soNhom()).isEqualTo(2);
            assertThat(row.soDangky()).isEqualTo(2); assertThat(row.soKetqua()).isEqualTo(2);
            assertThat(row.soDaCongbo()).isEqualTo(row.id() == 1L ? 1 : 0);
            assertThat(row.soKetquaThieuDiemCuoi()).isEqualTo(row.id() == 2L || row.id() == 4L ? 1 : 0);
        });
        assertThat(jdbc.queryForList("select * from ketqua order by Id")).isEqualTo(before);
        assertThat(count("emailketqua")).isZero();
        assertThat(deliveries.get()).isZero();
    }

    @Test void statisticsPreservesNullStatusRowsAllowedByDeliveredSqlSchema() {
        // This fixture runs only in jdbc:h2:mem:demo-fixture, checked in BeforeAll above.
        jdbc.update("update detai set Trangthai=NULL where Id=1");
        jdbc.update("update dangkydetai set Trangthai=NULL where Id=1");
        var report = statistics.statistics(new com.example.quanlydetaisinhvien.dto.morong.ThongkeFilterDTO());
        assertThat(report.detaiByStatus().getLast().code()).isEqualTo("KHONG_XAC_DINH");
        assertThat(report.detaiByStatus().getLast().count()).isEqualTo(1);
        assertThat(report.dangkyByStatus().getLast().code()).isEqualTo("KHONG_XAC_DINH");
        assertThat(report.dangkyByStatus().getLast().count()).isEqualTo(1);
        assertThat(report.detaiByStatus().stream().mapToLong(s -> s.count()).sum()).isEqualTo(8);
        assertThat(report.dangkyByStatus().stream().mapToLong(s -> s.count()).sum()).isEqualTo(8);
        assertThat(report.summary().tongKetqua()).isEqualTo(8);
    }

    @ParameterizedTest @ValueSource(longs = {1, 2, 3, 4, 7})
    void readyCasesPublishWithThreePendingEmailsWithoutDelivery(long id) {
        var result = service.congboKetqua(id);
        assertThat(result.getDacongbo()).isTrue();
        assertThat(result.getNgaycongbo()).isNotNull();
        assertThat(count("emailketqua")).isEqualTo(3);
        assertThat(jdbc.queryForList("select Trangthai from emailketqua", String.class)).containsOnly("PENDING");
        worker.drain();
        assertThat(deliveries.get()).isZero();
        assertThat(jdbc.queryForObject("select sum(Solanthu) from emailketqua", Integer.class)).isZero();
        assertThatThrownBy(() -> service.congboKetqua(id)).hasMessageContaining("đã được công bố");
        assertThat(count("emailketqua")).isEqualTo(3);
    }

    @ParameterizedTest @ValueSource(longs = {6, 8})
    void incompleteCasesAreBlockedWithoutInventingFinalScore(long id) {
        assertThatThrownBy(() -> service.congboKetqua(id)).isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("Điểm cuối");
        assertThat(count("emailketqua")).isZero();
        long missing = id == 6 ? 10 : 18;
        assertThat(jdbc.queryForObject("select count(*) from diem where Dangkydetaiid=? and Giangvienid=?",
                Integer.class, id, missing)).isZero();
    }

    @Test void studentSeesOnlyPublishedOwnGroupAfterPublication() {
        auth("23110001", "SINHVIEN");
        assertThat(service.findAll()).isEmpty();
        assertThat(service.findById(5L)).isEmpty();
        auth("TK001", "TRUONGKHOA");
        service.congboKetqua(1L);
        auth("23110001", "SINHVIEN");
        assertThat(service.findAll()).extracting(Ketqua::getId).containsExactly(1L);
        assertThat(service.findById(5L)).isEmpty();
        auth("23110004", "SINHVIEN");
        assertThat(service.findAll()).isEmpty();
        assertThat(service.findById(1L)).isEmpty();
        auth("23110013", "SINHVIEN");
        assertThat(service.findAll()).extracting(Ketqua::getId).containsExactly(5L);
    }

    @ParameterizedTest @ValueSource(strings = {"SINHVIEN", "GIANGVIEN", "TRUONGBOMON"})
    void nonChiefCannotPublish(String role) {
        auth("demo-negative", role);
        assertThatThrownBy(() -> service.congboKetqua(1L)).isInstanceOf(AccessDeniedException.class);
        assertThat(count("emailketqua")).isZero();
    }

    @Test void newDemoPasswordsMatchBCryptAndAllAddressesAreReserved() {
        assertThat(jdbc.queryForList("select Matkhau from taikhoan", String.class))
                .allMatch(hash -> hash.startsWith("$2a$10$") && encoder.matches("HcmuteDemo@2026", hash));
        assertThat(jdbc.queryForList("select Email from taikhoan", String.class))
                .allMatch(email -> email.endsWith("@example.invalid"));
    }

    @Test void resettingRestoresWorkbookPublishedCaseAndClearsDemoOutbox() {
        var original = jdbc.queryForMap("select * from ketqua where Id=5");
        service.congboKetqua(1L);
        execute(fixture);
        assertThat(jdbc.queryForMap("select * from ketqua where Id=5")).isEqualTo(original);
        assertThat(count("emailketqua")).isZero();
        assertThat(jdbc.queryForObject("select count(*) from ketqua where Dacongbo=true", Integer.class)).isEqualTo(1);
    }

    private int count(String table) { return jdbc.queryForObject("select count(*) from " + table, Integer.class); }
    private String read(String filename) throws Exception { return Files.readString(Path.of("database/demo", filename)); }
    private void execute(String sql) {
        new ResourceDatabasePopulator(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8))).execute(source);
    }
    private void auth(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                username, "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
