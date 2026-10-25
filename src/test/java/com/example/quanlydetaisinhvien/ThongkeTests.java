package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.ThongkeFilterDTO;
import com.example.quanlydetaisinhvien.dto.morong.StatisticsModels.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.ThongkeRoundNotFoundException;
import com.example.quanlydetaisinhvien.repository.morong.ThongkeRepository;
import com.example.quanlydetaisinhvien.service.morong.ThongkeService;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Reuses the verified phase-1 H2 fixture and backend-only views; never renders Antigravity's files. */
class ThongkeTests extends MorongSearchTestSupport {
    @Autowired ThongkeService statsService;
    @Autowired ThongkeRepository statsRepository;

    @BeforeEach void statisticsFixture() {
        freeGroup();
        var pending = registration(topicA, studentA, "Nhóm chờ", TrangthaiDangkydetai.CHODUYET);
        result(pending, false, null, null);
        var cross = registration(topicA, studentB, "Nhóm lệch đợt", TrangthaiDangkydetai.DADUYET);
        em.find(Nhomsinhvien.class, cross.getNhomid()).setDotdangkyid(roundB.getId());
        result(cross, true, null, oldDate());
        var extraTopic = topic("Đề tài chưa có kết quả", null, roundA, departmentA, teacherA, TrangthaiDetai.DADUYET);
        registration(extraTopic, studentA, "Nhóm chưa có kết quả", TrangthaiDangkydetai.DADUYET);
        em.flush();
    }

    private java.time.LocalDateTime oldDate() { return java.time.LocalDateTime.of(2026, 10, 1, 8, 0); }
    private void freeGroup() {
        var group = new Nhomsinhvien(); group.setDotdangkyid(roundA.getId());
        group.setNhomtruongid(studentA.getId()); group.setTennhom("Nhóm chưa đăng ký"); em.persist(group);
    }
    private ThongkeFilterDTO filter(Long roundId) {
        var f = new ThongkeFilterDTO(); f.setDotdangkyid(roundId); return f;
    }

    @Test void totalsUseActualIndependentCountsIncludingNullScoresAndUnregisteredGroups() {
        var report = statsService.statistics(filter(null));
        assertThat(report.summary()).isEqualTo(new Summary(5, 8, 7, 6, 4, 2, 1, 3, 1));
        assertThat(report.filterOptions().dotdangky()).hasSize(5);
        assertThat(report.byRound()).extracting(RoundRow::id).isSortedAccordingTo(Comparator.reverseOrder());
        assertThat(report.detaiByStatus()).extracting(StatusCount::count).containsExactly(1L, 2L, 1L, 1L);
        assertThat(report.dangkyByStatus()).extracting(StatusCount::count).containsExactly(2L, 4L, 1L);
        assertThat(report.ketquaByStatus()).extracting(StatusCount::count).containsExactly(4L, 2L);
    }

    @ParameterizedTest @ValueSource(strings = {"all", "A", "B", "C", "D", "empty"})
    void everyDistributionAndRoundTableReconcileWithTheSameFilteredSummary(String scope) {
        Long roundId = switch (scope) {
            case "A" -> roundA.getId(); case "B" -> roundB.getId(); case "C" -> roundC.getId();
            case "D" -> roundD.getId(); case "empty" -> emptyRound.getId(); default -> null;
        };
        var report = statsService.statistics(filter(roundId));
        var s = report.summary();
        assertThat(report.detaiByStatus().stream().mapToLong(StatusCount::count).sum()).isEqualTo(s.tongDetai());
        assertThat(report.dangkyByStatus().stream().mapToLong(StatusCount::count).sum()).isEqualTo(s.tongDangky());
        assertThat(report.ketquaByStatus().stream().mapToLong(StatusCount::count).sum()).isEqualTo(s.tongKetqua());
        assertThat(s.daCongbo() + s.chuaCongbo()).isEqualTo(s.tongKetqua());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soDetai).sum()).isEqualTo(s.tongDetai());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soNhom).sum()).isEqualTo(s.tongNhom());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soDangky).sum()).isEqualTo(s.tongDangky());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soKetqua).sum()).isEqualTo(s.tongKetqua());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soDaCongbo).sum()).isEqualTo(s.daCongbo());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soNhomChuaDangky).sum()).isEqualTo(s.nhomChuaDangky());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soKetquaThieuDiemCuoi).sum()).isEqualTo(s.ketquaThieuDiemCuoi());
        assertThat(report.byRound().stream().mapToLong(RoundRow::soDangkyLechDot).sum()).isEqualTo(s.dangkyLechDot());
        assertThat(report.byRound()).hasSize(roundId == null ? 5 : 1);
        assertThat(report.filterOptions().dotdangky()).hasSize(5);
    }

    @Test void mismatchedGroupIsCountedInItsOwnRoundAndRegistrationAndResultBelongToTopicRound() {
        var a = statsService.statistics(filter(roundA.getId()));
        var b = statsService.statistics(filter(roundB.getId()));
        assertThat(a.summary()).isEqualTo(new Summary(2, 4, 4, 3, 2, 1, 1, 2, 1));
        assertThat(b.summary()).isEqualTo(new Summary(1, 2, 1, 1, 1, 0, 0, 0, 0));
        assertThat(a.byRound().getFirst().soDangkyLechDot()).isEqualTo(1);
        assertThat(a.dangkyByStatus()).extracting(StatusCount::count).containsExactly(1L, 3L, 0L);
    }

    @Test void emptyRoundHasExplicitZeroKpisAndZeroStatusBuckets() {
        var report = statsService.statistics(filter(emptyRound.getId()));
        assertThat(report.summary()).isEqualTo(new Summary(0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(report.byRound()).hasSize(1);
        for (var buckets : List.of(report.detaiByStatus(), report.dangkyByStatus(), report.ketquaByStatus())) {
            assertThat(buckets).allSatisfy(b -> {
                assertThat(b.count()).isZero(); assertThat(b.percentage()).isEqualByComparingTo("0");
            });
        }
    }

    @Test void noRoundsAndNoBusinessDataReturnAnEmptyTableAndZeroSummary() {
        for (String entity : List.of("Ketqua", "Thanhviennhom", "Dangkydetai", "Nhomsinhvien",
                "Giangvienhuongdan", "Detai", "Dotdangky")) em.createQuery("delete from " + entity).executeUpdate();
        em.flush(); em.clear();
        var report = statsService.statistics(filter(null));
        assertThat(report.byRound()).isEmpty();
        assertThat(report.filterOptions().dotdangky()).isEmpty();
        assertThat(report.summary()).isEqualTo(new Summary(0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertThat(report.detaiByStatus()).hasSize(4);
        assertThat(report.dangkyByStatus()).hasSize(3);
        assertThat(report.ketquaByStatus()).hasSize(2);
    }

    @Test void multipleGuidesAndMembersDoNotMultiplyStatisticsAndQueryCountDoesNotGrowPerRound() {
        for (int i = 0; i < 20; i++) round("Đợt bổ sung " + i, Loaidot.NCKH, TrangthaiDotdangky.NHAP, i);
        em.flush(); em.clear();
        var metrics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics(); metrics.clear();
        var report = statsService.statistics(filter(null));
        assertThat(report.summary()).isEqualTo(new Summary(5, 8, 7, 6, 4, 2, 1, 3, 1));
        assertThat(report.byRound()).hasSize(25);
        assertThat(metrics.getQueryExecutionCount()).isEqualTo(5);
        assertThat(metrics.getEntityLoadCount()).isZero();
    }

    @Test void controllerExposesExactModelsAndWorkingExportWithoutReadingTemplates() throws Exception {
        var expected = statsService.statistics(filter(roundA.getId()));
        mvc.perform(get("/morong/thongke").param("dotdangkyid", roundA.getId().toString())
                        .with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name("morong/thongke"))
                .andExpect(model().attributeExists("title", "filter", "summary", "byRound", "detaiByStatus",
                        "dangkyByStatus", "ketquaByStatus", "filterOptions", "exportOptions", "headerThongbaos"))
                .andExpect(model().attribute("summary", expected.summary()))
                .andExpect(model().attribute("byRound", expected.byRound()))
                .andExpect(model().attribute("detaiByStatus", expected.detaiByStatus()))
                .andExpect(model().attribute("dangkyByStatus", expected.dangkyByStatus()))
                .andExpect(model().attribute("ketquaByStatus", expected.ketquaByStatus()))
                .andExpect(model().attribute("filterOptions", expected.filterOptions()))
                .andExpect(model().attribute("exportOptions", new ExportOptions(true, List.of("xlsx", "pdf"),
                        "/morong/xuat/thongke.xlsx?dotdangkyid=" + roundA.getId(),
                        "/morong/xuat/thongke.pdf?dotdangkyid=" + roundA.getId())));
    }

    @ParameterizedTest @ValueSource(strings = {"0", "-1", "abc", "1.5", "9999999999999999999999999", "1 OR 1=1", "1,2"})
    void malformedFiltersAre400(String value) throws Exception {
        mvc.perform(get("/morong/thongke").param("dotdangkyid", value).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isBadRequest()).andExpect(view().name("error"))
                .andExpect(model().attributeDoesNotExist("summary", "byRound", "headerThongbaos"));
    }

    @Test void duplicateFilterValuesAreRejectedAndBlankMeansAllRounds() throws Exception {
        mvc.perform(get("/morong/thongke").param("dotdangkyid", roundA.getId().toString(), roundB.getId().toString())
                        .with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isBadRequest());
        mvc.perform(get("/morong/thongke").param("dotdangkyid", "").with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(model().attribute("summary", new Summary(5, 8, 7, 6, 4, 2, 1, 3, 1)));
    }

    @Test void missingRoundIs404AndDirectServiceValidatesIds() throws Exception {
        assertThatThrownBy(() -> statsService.statistics(filter(Long.MAX_VALUE))).isInstanceOf(ThongkeRoundNotFoundException.class);
        assertThatThrownBy(() -> statsService.statistics(filter(0L))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> statsRepository.aggregate(-1L))
                .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> statsService.statistics(null)).isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get("/morong/thongke").param("dotdangkyid", Long.toString(Long.MAX_VALUE))
                        .with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isNotFound()).andExpect(view().name("error"));
    }

    @ParameterizedTest @ValueSource(strings = {"TRUONGBOMON", "GIANGVIEN", "SINHVIEN", "UNKNOWN"})
    void otherRolesCannotReadViaHttpServiceOrRepositoryEvenWithForgedIdentityParameters(String role) throws Exception {
        auth("search-chief", role);
        assertThatThrownBy(() -> statsService.statistics(filter(null))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> statsRepository.aggregate(null)).isInstanceOf(AccessDeniedException.class);
        mvc.perform(get("/morong/thongke").param("username", "search-chief").param("role", "TRUONGKHOA")
                        .with(user("search-chief").roles(role))).andExpect(status().isForbidden());
    }

    @Test void anonymousAndUnauthenticatedUsersHaveNoDirectAccessAndHttpUsesExistingLoginFlow() throws Exception {
        var anonymous = new AnonymousAuthenticationToken("test", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        for (var authentication : Arrays.asList(null, anonymous,
                UsernamePasswordAuthenticationToken.unauthenticated("search-chief", "unused"))) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            assertThatThrownBy(() -> statsService.statistics(filter(null))).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> statsRepository.aggregate(null)).isInstanceOf(AccessDeniedException.class);
        }
        mvc.perform(get("/morong/thongke").with(anonymous())).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test void clientCannotLimitOrElevateTheChiefScopeWithExtraParametersAndNoExportOrWriteRouteExists() throws Exception {
        mvc.perform(get("/morong/thongke").param("username", "search-student-a").param("role", "SINHVIEN")
                        .param("filter.dotdangkyid", roundA.getId().toString()).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(model().attribute("summary", new Summary(5, 8, 7, 6, 4, 2, 1, 3, 1)));
        mvc.perform(post("/morong/thongke").with(user("search-chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().isForbidden());
        for (String suffix : List.of("/excel", "/pdf", "/export")) {
            mvc.perform(get("/morong/thongke" + suffix).with(user("search-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void statisticsServiceAndHttpNeverMutateAnyDatabaseTable() throws Exception {
        var before = snapshot();
        statsService.statistics(filter(null)); statsService.statistics(filter(roundA.getId()));
        mvc.perform(get("/morong/thongke").with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isOk());
        assertThat(snapshot()).isEqualTo(before);
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        em.flush();
        var tables = jdbc.queryForList("select TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' order by TABLE_NAME", String.class);
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : tables) result.put(table, jdbc.queryForList("select * from " + table));
        return result;
    }
}
