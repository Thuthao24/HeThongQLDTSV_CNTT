package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.KetquaDoiChieuDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.KetquaService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class KetquaPerformanceAndCorrectnessTests extends MorongSearchTestSupport {

    @Autowired private EntityManager em;
    @Autowired private KetquaService ketquaService;
    @Autowired private KetquaRepository ketquaRepository;
    @Autowired private DangkydetaiRepository dangkydetaiRepository;
    @Autowired private DetaiRepository detaiRepository;
    @Autowired private NhomsinhvienRepository nhomsinhvienRepository;
    @Autowired private HoidongRepository hoidongRepository;
    @Autowired private DiemRepository diemRepository;

    private Statistics hibernateStats;

    @BeforeEach
    void setupBenchmark() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        SessionFactory sessionFactory = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        hibernateStats = sessionFactory.getStatistics();
        hibernateStats.clear();
    }

    @Test
    @DisplayName("1. Ketqua page renders full and correct lookups: topic, group, council names")
    void testPageRendersCorrectLookups() throws Exception {
        Dotdangky dot = round("Dot Lookups", Loaidot.TLCN, TrangthaiDotdangky.CONGBO, 1);
        Hoidong hd = new Hoidong();
        hd.setTenhoidong("Hội đồng Đánh giá 01");
        hd.setDotdangkyid(dot.getId());
        em.persist(hd);

        Detai dt = topic("Đề tài Xây dựng Hệ thống", "Mô tả", dot, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk = registration(dt, studentA, "Nhóm Alpha", TrangthaiDangkydetai.DADUYET);
        Ketqua kq = result(dk, true, "9.00", LocalDateTime.now());

        Phanconghoidong pchd = new Phanconghoidong();
        pchd.setDangkydetaiid(dk.getId());
        pchd.setHinhthuc(HinhthucDanhgia.COHOIDONG);
        pchd.setHoidongid(hd.getId());
        em.persist(pchd);

        em.flush();
        em.clear();

        MvcResult result = mvc.perform(get("/ketqua").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> model = result.getModelAndView().getModel();
        assertThat(model.get("canManage")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        Map<Long, String> topicNames = (Map<Long, String>) model.get("topicNames");
        @SuppressWarnings("unchecked")
        Map<Long, String> groupNames = (Map<Long, String>) model.get("groupNames");
        @SuppressWarnings("unchecked")
        Map<Long, String> councilNames = (Map<Long, String>) model.get("councilNames");

        assertThat(topicNames).isNotNull();
        assertThat(topicNames.get(dk.getId())).isEqualTo("Đề tài Xây dựng Hệ thống");

        assertThat(groupNames).isNotNull();
        assertThat(groupNames.get(dk.getId())).isEqualTo("Nhóm Alpha");

        assertThat(councilNames).isNotNull();
        assertThat(councilNames.get(hd.getId())).isEqualTo("Hội đồng Đánh giá 01");
    }

    @Test
    @DisplayName("2. Handles empty pages, missing IDs, or deleted foreign entities without crashing")
    void testEmptyPageAndMissingRelationships() throws Exception {
        // Create an orphan Ketqua with non-existent dangkydetaiid
        Ketqua orphan = new Ketqua();
        orphan.setDangkydetaiid(999999L);
        orphan.setDacongbo(true);
        orphan.setDiemcuoi(new BigDecimal("7.00"));
        orphan.setNgaycongbo(LocalDateTime.now());
        em.persist(orphan);

        // Create a Dangkydetai with non-existent detai and nhom
        Dangkydetai brokenDk = new Dangkydetai();
        brokenDk.setDetaiid(888888L); // non-existent
        brokenDk.setNhomid(777777L);  // non-existent
        brokenDk.setTrangthai(TrangthaiDangkydetai.DADUYET);
        brokenDk.setNgaydangky(LocalDateTime.now());
        em.persist(brokenDk);

        Ketqua brokenKq = new Ketqua();
        brokenKq.setDangkydetaiid(brokenDk.getId());
        brokenKq.setDacongbo(true);
        brokenKq.setDiemcuoi(new BigDecimal("6.50"));
        brokenKq.setNgaycongbo(LocalDateTime.now());
        em.persist(brokenKq);
        em.flush();
        em.clear();

        // 1. Page with orphan & broken records must return 200 OK
        MvcResult pageResult = mvc.perform(get("/ketqua").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(pageResult.getResponse().getStatus()).isEqualTo(200);

        // 2. Empty page (page=999) must return 200 OK with empty items
        MvcResult emptyPageResult = mvc.perform(get("/ketqua?page=999").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andReturn();
        @SuppressWarnings("unchecked")
        Page<Ketqua> pageData = (Page<Ketqua>) emptyPageResult.getModelAndView().getModel().get("pageData");
        assertThat(pageData.getContent()).isEmpty();

        // 3. Service findAllDoiChieuDto handles orphans gracefully
        auth("search-chief", "TRUONGKHOA");
        List<KetquaDoiChieuDto> dtos = ketquaService.findAllDoiChieuDto();
        KetquaDoiChieuDto orphanDto = dtos.stream().filter(d -> d.getId().equals(orphan.getId())).findFirst().orElse(null);
        assertThat(orphanDto).isNotNull();
        assertThat(orphanDto.getTendetai()).isEqualTo("Đăng ký #999999");
        assertThat(orphanDto.getTennhom()).isEqualTo("---");

        KetquaDoiChieuDto brokenDto = dtos.stream().filter(d -> d.getId().equals(brokenKq.getId())).findFirst().orElse(null);
        assertThat(brokenDto).isNotNull();
        assertThat(brokenDto.getTendetai()).isEqualTo("Đề tài #888888");
        assertThat(brokenDto.getTennhom()).isEqualTo("Nhóm #777777");
    }

    @Test
    @DisplayName("3. Pagination and descending ID ordering remain strictly unchanged")
    void testPaginationAndOrdering() throws Exception {
        Dotdangky dot = round("Dot Order", Loaidot.TLCN, TrangthaiDotdangky.CONGBO, 2);
        for (int i = 1; i <= 25; i++) {
            Detai dt = topic("De tai Order " + i, "Mo ta " + i, dot, departmentA, teacherA, TrangthaiDetai.CONGBO);
            Dangkydetai dk = registration(dt, studentA, "Nhom Order " + i, TrangthaiDangkydetai.DADUYET);
            result(dk, true, "8.00", LocalDateTime.now());
        }
        em.flush();
        em.clear();

        // Page 0 (size 20)
        MvcResult page0 = mvc.perform(get("/ketqua?page=0").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andReturn();
        @SuppressWarnings("unchecked")
        Page<Ketqua> pageData0 = (Page<Ketqua>) page0.getModelAndView().getModel().get("pageData");
        assertThat(pageData0.getNumberOfElements()).isEqualTo(20);
        List<Ketqua> items0 = pageData0.getContent();
        for (int i = 0; i < items0.size() - 1; i++) {
            assertThat(items0.get(i).getId()).isGreaterThan(items0.get(i + 1).getId());
        }

        // Page 1 (size remaining)
        MvcResult page1 = mvc.perform(get("/ketqua?page=1").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andReturn();
        @SuppressWarnings("unchecked")
        Page<Ketqua> pageData1 = (Page<Ketqua>) page1.getModelAndView().getModel().get("pageData");
        assertThat(pageData1.getNumberOfElements()).isGreaterThanOrEqualTo(5);
        List<Ketqua> items1 = pageData1.getContent();
        for (int i = 0; i < items1.size() - 1; i++) {
            assertThat(items1.get(i).getId()).isGreaterThan(items1.get(i + 1).getId());
        }
    }

    @Test
    @DisplayName("4. Role-based visibility and security boundaries are fully preserved")
    void testRoleBasedVisibility() throws Exception {
        // Student request
        MvcResult studentResult = mvc.perform(get("/ketqua").with(user("search-student-a").roles("SINHVIEN")))
                .andExpect(status().isOk())
                .andReturn();
        Map<String, Object> studentModel = studentResult.getModelAndView().getModel();
        assertThat(studentModel.get("canManage")).isEqualTo(false);
        assertThat(studentModel.get("doiChieuItems")).isNull();
        assertThat(studentModel.get("publicationInfo")).isNull();

        @SuppressWarnings("unchecked")
        List<Ketqua> studentItems = (List<Ketqua>) studentModel.get("items");
        for (Ketqua k : studentItems) {
            assertThat(k.getDacongbo()).isTrue();
        }
    }

    @Test
    @DisplayName("5. Score reconciliation (findAllDoiChieuDto) math, rounding HALF_UP, and khop flag")
    void testDoiChieuCalculationExactness() {
        Dotdangky dot = round("Dot Math", Loaidot.TLCN, TrangthaiDotdangky.CONGBO, 3);

        // Case 1: 3 scores: 8.00, 8.50, 9.00 -> avg = 8.50, diemcuoi = 8.50 -> khop = true
        Detai dt1 = topic("DT Math 1", "M1", dot, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk1 = registration(dt1, studentA, "Nhom Math 1", TrangthaiDangkydetai.DADUYET);
        Ketqua kq1 = result(dk1, true, "8.50", LocalDateTime.now());
        addDiem(dk1.getId(), teacherA.getId(), "8.00");
        addDiem(dk1.getId(), teacherB.getId(), "8.50");
        addDiem(dk1.getId(), teacherA.getId(), "9.00");

        // Case 2: Repeating fraction: 8.00, 8.00, 9.00 -> sum = 25.00 -> 25/3 = 8.333... -> HALF_UP 8.33
        // If diemcuoi = 8.33 -> khop = true
        Detai dt2 = topic("DT Math 2", "M2", dot, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk2 = registration(dt2, studentA, "Nhom Math 2", TrangthaiDangkydetai.DADUYET);
        Ketqua kq2 = result(dk2, true, "8.33", LocalDateTime.now());
        addDiem(dk2.getId(), teacherA.getId(), "8.00");
        addDiem(dk2.getId(), teacherA.getId(), "8.00");
        addDiem(dk2.getId(), teacherB.getId(), "9.00");

        // Case 3: Same repeating fraction but diemcuoi = 8.35 -> khop = false
        Detai dt3 = topic("DT Math 3", "M3", dot, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk3 = registration(dt3, studentA, "Nhom Math 3", TrangthaiDangkydetai.DADUYET);
        Ketqua kq3 = result(dk3, true, "8.35", LocalDateTime.now());
        addDiem(dk3.getId(), teacherA.getId(), "8.00");
        addDiem(dk3.getId(), teacherA.getId(), "8.00");
        addDiem(dk3.getId(), teacherB.getId(), "9.00");

        // Case 4: No scores entered -> sodiemthanhphan = 0, diemtrungbinhthanhphan = null, khop = false
        Detai dt4 = topic("DT Math 4", "M4", dot, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk4 = registration(dt4, studentA, "Nhom Math 4", TrangthaiDangkydetai.DADUYET);
        Ketqua kq4 = result(dk4, true, "8.00", LocalDateTime.now());

        em.flush();
        em.clear();

        auth("search-chief", "TRUONGKHOA");
        List<KetquaDoiChieuDto> dtos = ketquaService.findAllDoiChieuDto();

        KetquaDoiChieuDto dto1 = dtos.stream().filter(d -> d.getId().equals(kq1.getId())).findFirst().orElseThrow();
        assertThat(dto1.getSodiemthanhphan()).isEqualTo(3);
        assertThat(dto1.getDiemtrungbinhthanhphan()).isEqualByComparingTo("8.50");
        assertThat(dto1.isKhop()).isTrue();

        KetquaDoiChieuDto dto2 = dtos.stream().filter(d -> d.getId().equals(kq2.getId())).findFirst().orElseThrow();
        assertThat(dto2.getSodiemthanhphan()).isEqualTo(3);
        assertThat(dto2.getDiemtrungbinhthanhphan()).isEqualByComparingTo("8.33");
        assertThat(dto2.isKhop()).isTrue();

        KetquaDoiChieuDto dto3 = dtos.stream().filter(d -> d.getId().equals(kq3.getId())).findFirst().orElseThrow();
        assertThat(dto3.getSodiemthanhphan()).isEqualTo(3);
        assertThat(dto3.getDiemtrungbinhthanhphan()).isEqualByComparingTo("8.33");
        assertThat(dto3.isKhop()).isFalse();

        KetquaDoiChieuDto dto4 = dtos.stream().filter(d -> d.getId().equals(kq4.getId())).findFirst().orElseThrow();
        assertThat(dto4.getSodiemthanhphan()).isEqualTo(0);
        assertThat(dto4.getDiemtrungbinhthanhphan()).isNull();
        assertThat(dto4.isKhop()).isFalse();
    }


    @Test
    @DisplayName("6. Measure Query Count and Entity Load Benchmark on N=50")
    void testQueryCountAndEntityLoadAudit() throws Exception {
        Dotdangky dot = round("Dot Benchmark 50", Loaidot.TLCN, TrangthaiDotdangky.CONGBO, 10);
        for (int i = 1; i <= 50; i++) {
            Detai dt = topic("De tai BM " + i, "Mo ta " + i, dot, departmentA, teacherA, TrangthaiDetai.CONGBO);
            Dangkydetai dk = registration(dt, studentA, "Nhom BM " + i, TrangthaiDangkydetai.DADUYET);
            result(dk, true, "8.50", LocalDateTime.now());
            addDiem(dk.getId(), teacherA.getId(), "8.00");
            addDiem(dk.getId(), teacherB.getId(), "9.00");
        }
        em.flush();
        em.clear();

        hibernateStats.clear();
        long start = System.nanoTime();
        mvc.perform(get("/ketqua").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk());
        long ms = (System.nanoTime() - start) / 1_000_000;
        long qCount = hibernateStats.getPrepareStatementCount();
        long entitiesLoaded = hibernateStats.getEntityLoadCount();

        System.out.println("====================================================");
        System.out.println("[BENCHMARK METRICS N=50]");
        System.out.println("  Total HTTP GET /ketqua SQLs: " + qCount);
        System.out.println("  Total Entities Loaded: " + entitiesLoaded);
        System.out.println("  Total Duration: " + ms + " ms");
        System.out.println("====================================================");
    }
    private void addDiem(Long dkId, Long gvId, String score) {
        Diem d = new Diem();
        d.setDangkydetaiid(dkId);
        d.setGiangvienid(gvId);
        d.setDiemso(new BigDecimal(score));
        d.setNhanxet("Cham diem test");
        em.persist(d);
    }
}
