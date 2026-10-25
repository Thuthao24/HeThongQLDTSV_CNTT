package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.NopBaoCaoDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.NopbaocaoService;
import jakarta.persistence.EntityManager;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nplusone-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
@TestPropertySource(properties = "spring.jpa.properties.hibernate.session_factory.statement_inspector="
        + "com.example.quanlydetaisinhvien.ReportListNPlusOneTests$SqlCapture")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReportListNPlusOneTests {

    public static class SqlCapture implements StatementInspector {
        static final ThreadLocal<List<String>> SQL = ThreadLocal.withInitial(ArrayList::new);

        @Override
        public String inspect(String sql) {
            SQL.get().add(sql.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT));
            return sql;
        }
    }

    @Autowired
    private NopbaocaoService nopbaocaoService;

    @Autowired
    private NopbaocaoRepository nopbaocaoRepo;

    @Autowired
    private DangkydetaiRepository dangkyRepo;

    @Autowired
    private DetaiRepository detaiRepo;

    @Autowired
    private NhomsinhvienRepository nhomRepo;

    @Autowired
    private SinhvienRepository sinhvienRepo;

    @Autowired
    private TaikhoanRepository taikhoanRepo;

    @Autowired
    private BomonRepository bomonRepo;

    @Autowired
    private DotdangkyRepository dotRepo;

    @Autowired
    private EntityManager em;

    @AfterEach
    void clearSql() {
        SqlCapture.SQL.remove();
    }

    @BeforeEach
    void setUp() {
        SqlCapture.SQL.get().clear();
    }

    private void createFixtures(int count) {
        Bomon bm = new Bomon();
        bm.setMabomon("BM_N1");
        bm.setTenbomon("Bo Mon N1");
        bm = bomonRepo.save(bm);

        Dotdangky dot = new Dotdangky();
        dot.setTendot("Dot N+1 Test");
        dot.setLoaidot(Loaidot.KLTN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(20));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(20));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        dot = dotRepo.save(dot);

        for (int i = 1; i <= count; i++) {
            Taikhoan tk = new Taikhoan();
            tk.setTendangnhap("sv_n1_" + i);
            tk.setMatkhau("pass123");
            tk.setHoten("Sinh Vien " + i);
            tk.setEmail("svn1_" + i + "@test.com");
            tk.setVaitro(Vaitro.SINHVIEN);
            tk.setKichhoat(true);
            tk = taikhoanRepo.save(tk);

            Sinhvien sv = new Sinhvien();
            sv.setMasinhvien("sv_n1_" + i);
            sv.setTaikhoanid(tk.getId());
            sv.setBomonid(bm.getId());
            sv = sinhvienRepo.save(sv);

            Nhomsinhvien nhom = new Nhomsinhvien();
            nhom.setDotdangkyid(dot.getId());
            nhom.setTennhom("Nhom N1 " + i);
            nhom.setNhomtruongid(sv.getId());
            nhom = nhomRepo.save(nhom);

            Detai dt = new Detai();
            dt.setDotdangkyid(dot.getId());
            dt.setTendetai("De Tai " + i);
            dt.setBomonid(bm.getId());
            dt.setGiangviendexuatid(1L);
            dt.setTrangthai(TrangthaiDetai.DADUYET);
            dt = detaiRepo.save(dt);

            Dangkydetai dk = new Dangkydetai();
            dk.setNhomid(nhom.getId());
            dk.setDetaiid(dt.getId());
            dk.setNgaydangky(LocalDateTime.now());
            dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
            dk = dangkyRepo.save(dk);

            Nopbaocao rep = new Nopbaocao();
            rep.setDangkydetaiid(dk.getId());
            rep.setSinhvienid(sv.getId());
            rep.setDuongdanfile("/uploads/baocao/report_" + i + ".pdf");
            rep.setNgaynop(LocalDateTime.now());
            nopbaocaoRepo.save(rep);
        }
    }

    @Test
    @Transactional
    @DisplayName("N+1 Check: layDanhSachBaoCaoDto query count does not scale with N (constant batch queries)")
    void testLayDanhSachBaoCaoDtoHasConstantQueries() {
        int numReports = 10;
        createFixtures(numReports);

        em.flush();
        em.clear();
        SqlCapture.SQL.get().clear();

        List<NopBaoCaoDto> results = nopbaocaoService.layDanhSachBaoCaoDto(0);

        assertThat(results).hasSize(numReports);

        List<String> selectQueries = SqlCapture.SQL.get().stream()
                .filter(sql -> sql.startsWith("select "))
                .toList();

        // With N=10 reports, an unbatched N+1 implementation would issue at least 2N+1 = 21 queries.
        // With our batch queries in chuyenDanhSachBaoCaoSangDto, total select queries must be <= 10.
        assertThat(selectQueries.size())
                .as("Total SELECT queries should be bounded and batch-oriented, not 2*N (%d)", selectQueries.size())
                .isLessThanOrEqualTo(10);

        // Verify batch queries for diem and ketqua are present
        boolean hasDiemBatchQuery = selectQueries.stream().anyMatch(sql -> sql.contains("from diem") && sql.contains("in ("));
        boolean hasKetquaBatchQuery = selectQueries.stream().anyMatch(sql -> sql.contains("from ketqua") && sql.contains("in ("));

        assertThat(hasDiemBatchQuery).as("Should use batch query for diem: findDangkydetaiidsWithDiem").isTrue();
        assertThat(hasKetquaBatchQuery).as("Should use batch query for ketqua: findDangkydetaiidsWithKetquaDaCongBo").isTrue();
    }
}
