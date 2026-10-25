package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.KetquaPublicationInfo;
import com.example.quanlydetaisinhvien.service.KetquaService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class KetquaMultiCouncilAndRoundTests extends MorongSearchTestSupport {

    @Autowired private EntityManager em;
    @Autowired private KetquaService ketquaService;
    @Autowired private HoidongRepository hoidongRepository;
    @Autowired private PhanconghoidongRepository phanconghoidongRepository;

    private Statistics hibernateStats;

    @BeforeEach
    void setupBenchmark() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        SessionFactory sessionFactory = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        hibernateStats = sessionFactory.getStatistics();
        hibernateStats.clear();
    }

    @Test
    @DisplayName("Test: 1 Round with Multiple Councils + 1 Page with Multiple Rounds + No Council + Null Relations")
    void testMultiCouncilAndMultiRoundScenarios() throws Exception {
        // --- Round 1: Has 3 councils ---
        Dotdangky round1 = round("Đợt 1 - Nhiều HĐ", Loaidot.TLCN, TrangthaiDotdangky.CONGBO, 1);
        Hoidong hd1A = createCouncil("Hội đồng 1A", round1.getId());
        Hoidong hd1B = createCouncil("Hội đồng 1B", round1.getId());
        Hoidong hd1C = createCouncil("Hội đồng 1C", round1.getId());

        // Topic 1 in Round 1 -> Assigned to Council 1A
        Detai dt1 = topic("Đề tài 1 Round 1", "Mô tả 1", round1, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk1 = registration(dt1, studentA, "Nhóm 1", TrangthaiDangkydetai.DADUYET);
        Ketqua kq1 = result(dk1, false, "8.00", null);
        assignCouncil(dk1.getId(), HinhthucDanhgia.COHOIDONG, hd1A.getId());

        // Topic 2 in Round 1 -> Assigned to Council 1B
        Detai dt2 = topic("Đề tài 2 Round 1", "Mô tả 2", round1, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk2 = registration(dt2, studentA, "Nhóm 2", TrangthaiDangkydetai.DADUYET);
        Ketqua kq2 = result(dk2, false, "8.50", null);
        assignCouncil(dk2.getId(), HinhthucDanhgia.COHOIDONG, hd1B.getId());

        // Topic 3 in Round 1 -> Form KHONGHOIDONG (No council)
        Detai dt3 = topic("Đề tài 3 Round 1 (No HD)", "Mô tả 3", round1, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk3 = registration(dt3, studentA, "Nhóm 3", TrangthaiDangkydetai.DADUYET);
        Ketqua kq3 = result(dk3, false, "7.50", null);
        assignCouncil(dk3.getId(), HinhthucDanhgia.KHONGHOIDONG, null);

        // --- Round 2: Has 2 councils ---
        Dotdangky round2 = round("Đợt 2 - Khóa luận", Loaidot.KLTN, TrangthaiDotdangky.CONGBO, 2);
        Hoidong hd2A = createCouncil("Hội đồng 2A Khóa luận", round2.getId());
        Hoidong hd2B = createCouncil("Hội đồng 2B Khóa luận", round2.getId());

        // Topic 4 in Round 2 -> Assigned to Council 2A
        Detai dt4 = topic("Đề tài 4 Round 2", "Mô tả 4", round2, departmentB, teacherB, TrangthaiDetai.CONGBO);
        Dangkydetai dk4 = registration(dt4, studentA, "Nhóm 4", TrangthaiDangkydetai.DADUYET);
        Ketqua kq4 = result(dk4, false, "9.00", null);
        assignCouncil(dk4.getId(), HinhthucDanhgia.COHOIDONG, hd2A.getId());

        // Topic 5 in Round 2 -> Not yet assigned (assignment is null)
        Detai dt5 = topic("Đề tài 5 Round 2 (Unassigned)", "Mô tả 5", round2, departmentB, teacherB, TrangthaiDetai.CONGBO);
        Dangkydetai dk5 = registration(dt5, studentA, "Nhóm 5", TrangthaiDangkydetai.DADUYET);
        Ketqua kq5 = result(dk5, false, null, null);

        // --- Round 3: Completely separate round ---
        Dotdangky round3 = round("Đợt 3 - Môn học", Loaidot.MONHOC, TrangthaiDotdangky.CONGBO, 3);
        Hoidong hd3 = createCouncil("Hội đồng 3 Môn học", round3.getId());

        // Topic 6 in Round 3 -> Assigned to Council 3
        Detai dt6 = topic("Đề tài 6 Round 3", "Mô tả 6", round3, departmentA, teacherA, TrangthaiDetai.CONGBO);
        Dangkydetai dk6 = registration(dt6, studentA, "Nhóm 6", TrangthaiDangkydetai.DADUYET);
        Ketqua kq6 = result(dk6, false, "8.25", null);
        assignCouncil(dk6.getId(), HinhthucDanhgia.COHOIDONG, hd3.getId());

        // --- Orphan record: null / missing relations ---
        Ketqua orphanKq = new Ketqua();
        orphanKq.setDangkydetaiid(555555L);
        orphanKq.setDacongbo(true);
        orphanKq.setDiemcuoi(new BigDecimal("7.00"));
        orphanKq.setNgaycongbo(LocalDateTime.now());
        em.persist(orphanKq);

        em.flush();
        em.clear();

        // -------------------------------------------------------------
        // Case A: TRUONGKHOA View
        // -------------------------------------------------------------
        hibernateStats.clear();
        MvcResult tkResult = mvc.perform(get("/ketqua").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk())
                .andReturn();
        long tkQueries = hibernateStats.getPrepareStatementCount();

        Map<String, Object> tkModel = tkResult.getModelAndView().getModel();
        assertThat(tkModel.get("canManage")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        Map<Long, String> councilNames = (Map<Long, String>) tkModel.get("councilNames");
        assertThat(councilNames).isNotNull();

        // Check council names from multiple rounds are correctly loaded
        assertThat(councilNames.get(hd1A.getId())).isEqualTo("Hội đồng 1A");
        assertThat(councilNames.get(hd1B.getId())).isEqualTo("Hội đồng 1B");
        assertThat(councilNames.get(hd1C.getId())).isEqualTo("Hội đồng 1C");
        assertThat(councilNames.get(hd2A.getId())).isEqualTo("Hội đồng 2A Khóa luận");
        assertThat(councilNames.get(hd2B.getId())).isEqualTo("Hội đồng 2B Khóa luận");
        assertThat(councilNames.get(hd3.getId())).isEqualTo("Hội đồng 3 Môn học");

        // Check publication info
        @SuppressWarnings("unchecked")
        Map<Long, KetquaPublicationInfo> pubInfo = (Map<Long, KetquaPublicationInfo>) tkModel.get("publicationInfo");
        assertThat(pubInfo).isNotNull();

        // Topic 1: can assign, has council choices of Round 1 (3 choices)
        KetquaPublicationInfo info1 = pubInfo.get(kq1.getId());
        assertThat(info1).isNotNull();
        assertThat(info1.assignment()).isNotNull();
        assertThat(info1.assignment().getHoidongid()).isEqualTo(hd1A.getId());
        assertThat(info1.councils()).extracting(Hoidong::getTenhoidong)
                .containsExactlyInAnyOrder("Hội đồng 1A", "Hội đồng 1B", "Hội đồng 1C");

        // Topic 3: KHONGHOIDONG -> assignment.getHoidongid() is null
        KetquaPublicationInfo info3 = pubInfo.get(kq3.getId());
        assertThat(info3).isNotNull();
        assertThat(info3.assignment()).isNotNull();
        assertThat(info3.assignment().getHoidongid()).isNull();

        // Topic 4: Round 2 (KLTN) -> allowNoCouncil is false, choices of Round 2 (2 choices)
        KetquaPublicationInfo info4 = pubInfo.get(kq4.getId());
        assertThat(info4).isNotNull();
        assertThat(info4.allowNoCouncil()).isFalse();
        assertThat(info4.councils()).extracting(Hoidong::getTenhoidong)
                .containsExactlyInAnyOrder("Hội đồng 2A Khóa luận", "Hội đồng 2B Khóa luận");

        // Topic 5: Unassigned -> assignment is null
        KetquaPublicationInfo info5 = pubInfo.get(kq5.getId());
        assertThat(info5).isNotNull();
        assertThat(info5.assignment()).isNull();

        // Query count check: Multi-round page must not trigger runaway queries
        System.out.println(">>> Multi-Round Multi-Council Queries (TRUONGKHOA): " + tkQueries);
        assertThat(tkQueries).isLessThanOrEqualTo(50);

        // -------------------------------------------------------------
        // Case B: SINHVIEN View
        // -------------------------------------------------------------
        hibernateStats.clear();
        MvcResult svResult = mvc.perform(get("/ketqua").with(user("search-student-a").roles("SINHVIEN")))
                .andExpect(status().isOk())
                .andReturn();
        long svQueries = hibernateStats.getPrepareStatementCount();

        Map<String, Object> svModel = svResult.getModelAndView().getModel();
        assertThat(svModel.get("canManage")).isEqualTo(false);
        assertThat(svModel.get("doiChieuItems")).isNull();
        assertThat(svModel.get("publicationInfo")).isNull();

        @SuppressWarnings("unchecked")
        Map<Long, String> svCouncilNames = (Map<Long, String>) svModel.get("councilNames");
        assertThat(svCouncilNames).isEmpty();

        System.out.println(">>> Multi-Round Multi-Council Queries (SINHVIEN): " + svQueries);
        assertThat(svQueries).isLessThanOrEqualTo(8);
    }

    private Hoidong createCouncil(String name, Long roundId) {
        Hoidong hd = new Hoidong();
        hd.setTenhoidong(name);
        hd.setDotdangkyid(roundId);
        em.persist(hd);
        return hd;
    }

    private void assignCouncil(Long dkId, HinhthucDanhgia form, Long councilId) {
        Phanconghoidong p = new Phanconghoidong();
        p.setDangkydetaiid(dkId);
        p.setHinhthuc(form);
        p.setHoidongid(councilId);
        em.persist(p);
    }
}
