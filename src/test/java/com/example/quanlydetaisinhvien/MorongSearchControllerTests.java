package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.SearchModels.*;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MorongSearchControllerTests extends MorongSearchTestSupport {
    @ParameterizedTest @ValueSource(strings = {"detai", "dotdangky", "ketqua"})
    void endpointsExposeAgreedModelAndWorkingExportLinks(String dataset) throws Exception {
        var response = mvc.perform(get(url(dataset)).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name("morong/timkiem-" + dataset))
                .andExpect(model().attributeExists("title", "filter", "items", "pageInfo", "filterOptions", "exportOptions")).andReturn();
        var model = response.getModelAndView().getModel();
        var export = (ExportOptions) model.get("exportOptions");
        assertThat(export.isEnabled()).isTrue();
        assertThat(export.getFormats()).containsExactly("xlsx", "pdf");
        assertThat(export.getExcelUrl()).startsWith("/morong/xuat/" + dataset + ".xlsx?");
        assertThat(export.getPdfUrl()).startsWith("/morong/xuat/" + dataset + ".pdf?");
        assertThat(((PageInfo) model.get("pageInfo")).size()).isEqualTo(20);
        assertThat(((PageInfo) model.get("pageInfo")).number()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN", "UNKNOWN"})
    void httpRoleMatrixMatchesExistingModuleReadPermissions(String role) throws Exception {
        for (String dataset : List.of("detai", "dotdangky", "ketqua")) {
            boolean allowed = switch (dataset) {
                case "detai" -> Set.of("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN").contains(role);
                case "dotdangky" -> role.equals("TRUONGKHOA");
                default -> Set.of("TRUONGKHOA", "SINHVIEN").contains(role);
            };
            mvc.perform(get(url(dataset)).with(user(role.equals("SINHVIEN") ? "search-student-a" : "search-chief").roles(role)))
                    .andExpect(status().is(allowed ? 200 : 403));
        }
    }

    @Test void anonymousIsRedirectedAndPostOrUnimplementedFeaturesAreDenied() throws Exception {
        for (String dataset : List.of("detai", "dotdangky", "ketqua")) {
            mvc.perform(get(url(dataset)).with(anonymous())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
            mvc.perform(post(url(dataset)).with(user("search-chief").roles("TRUONGKHOA")).with(csrf())).andExpect(status().isForbidden());
        }
        // Statistics is now implemented and its role matrix is covered by ThongkeTests.
        for (String url : List.of("/morong/email"))
            mvc.perform(get(url).with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isForbidden());
    }

    @Test void clientIdentityParametersCannotChangeStudentScopeOrOptions() throws Exception {
        var result = mvc.perform(get(url("ketqua")).with(user("search-student-a").roles("SINHVIEN"))
                .param("username", "search-chief").param("sinhvienid", studentB.getId().toString())
                .param("role", "TRUONGKHOA").param("student", "false").param("nhomid", otherResult.getDangkydetaiid().toString())
                .param("size", "1").param("sort", "id,asc"))
                .andExpect(status().isOk()).andReturn();
        var model = result.getModelAndView().getModel();
        assertThat((List<?>) model.get("items")).extracting("id").containsExactly(resultA.getId());
        assertThat(((PageInfo) model.get("pageInfo")).totalElements()).isEqualTo(2);
        var options = (FilterOptions) model.get("filterOptions");
        assertThat(options.dotdangky()).extracting(Option::value).doesNotContain(roundC.getId().toString(), roundD.getId().toString());
    }

    static Stream<Arguments> invalidFilters() {
        return Stream.of(
            Arguments.of("detai", "dotdangkyid", "0"), Arguments.of("detai", "bomonid", "-1"),
            Arguments.of("detai", "giangviendexuatid", "-1"), Arguments.of("detai", "giangvienhuongdanid", "0"),
            Arguments.of("detai", "dotdangkyid", "abc"), Arguments.of("detai", "loaidot", "LOAIDETAI"),
            Arguments.of("detai", "trangthai", "DONG"), Arguments.of("detai", "sort", "matkhau,asc"),
            Arguments.of("detai", "sort", "id;drop table detai,desc"), Arguments.of("detai", "page", "-1"),
            Arguments.of("detai", "size", "0"), Arguments.of("detai", "size", "101"),
            Arguments.of("detai", "page", "99999999999999999999"), Arguments.of("detai", "keyword", "X".repeat(256)),
            Arguments.of("dotdangky", "mocThoiGian", "NGAYTAO"), Arguments.of("dotdangky", "trangthai", "DADUYET"),
            Arguments.of("dotdangky", "thoiGianTu", "2026-02-30"), Arguments.of("dotdangky", "thoiGianDen", "+999999999-12-31"),
            Arguments.of("ketqua", "dacongbo", "wrong"), Arguments.of("ketqua", "trangthai", "CONGBO"),
            Arguments.of("ketqua", "diemTu", "-0.01"), Arguments.of("ketqua", "diemDen", "10.01"),
            Arguments.of("ketqua", "diemTu", "8.123"), Arguments.of("ketqua", "diemDen", "nan"),
            Arguments.of("ketqua", "ngayCongboTu", "2026-13-01"), Arguments.of("ketqua", "ngayCongboDen", "10000-01-01")
        );
    }

    @ParameterizedTest @MethodSource("invalidFilters")
    void malformedFilterReturns400WithModelErrorsAndNoResults(String dataset, String field, String value) throws Exception {
        var response = mvc.perform(get(url(dataset)).with(user("search-chief").roles("TRUONGKHOA")).param(field, value))
                .andExpect(status().isBadRequest()).andExpect(view().name("morong/timkiem-" + dataset))
                .andExpect(model().attributeHasErrors("filter")).andExpect(model().attributeExists("pageInfo", "filterOptions", "exportOptions"))
                .andReturn();
        assertThat((List<?>) response.getModelAndView().getModel().get("items")).isEmpty();
    }

    @Test void invertedDateAndScoreRangesAndExcessiveOffsetAreRejectedByHttpAndService() throws Exception {
        mvc.perform(get(url("ketqua")).with(user("search-chief").roles("TRUONGKHOA"))
                .param("diemTu", "9").param("diemDen", "8"))
                .andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("filter", "scoreRangeValid"));
        mvc.perform(get(url("ketqua")).with(user("search-chief").roles("TRUONGKHOA"))
                .param("ngayCongboTu", "2026-10-20").param("ngayCongboDen", "2026-10-01"))
                .andExpect(status().isBadRequest()).andExpect(model().attributeHasFieldErrors("filter", "dateRangeValid"));
        mvc.perform(get(url("dotdangky")).with(user("search-chief").roles("TRUONGKHOA"))
                .param("thoiGianTu", "2026-10-20").param("thoiGianDen", "2026-10-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(url("detai")).with(user("search-chief").roles("TRUONGKHOA"))
                .param("page", "2147483647").param("size", "100"))
                .andExpect(status().isBadRequest());
        auth("search-chief", "TRUONGKHOA");
        var scores = new KetquaFilterDTO(); scores.setDiemTu(new BigDecimal("9")); scores.setDiemDen(new BigDecimal("8"));
        assertThatThrownBy(() -> service.ketqua(scores)).isInstanceOf(ConstraintViolationException.class);
        var dates = new KetquaFilterDTO(); dates.setNgayCongboTu(LocalDate.of(2026, 10, 20)); dates.setNgayCongboDen(LocalDate.of(2026, 10, 1));
        assertThatThrownBy(() -> service.ketqua(dates)).isInstanceOf(ConstraintViolationException.class);
        var topicFilter = new DetaiFilterDTO(); topicFilter.setSort("matkhau,asc");
        assertThatThrownBy(() -> service.detai(topicFilter)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void invalidStudentQueryStillOnlyReceivesScopedDropdowns() throws Exception {
        var response = mvc.perform(get(url("ketqua")).with(user("search-student-a").roles("SINHVIEN"))
                .param("diemTu", "invalid").param("dotdangkyid", roundC.getId().toString()))
                .andExpect(status().isBadRequest()).andReturn();
        var options = (FilterOptions) response.getModelAndView().getModel().get("filterOptions");
        assertThat(options.dotdangky()).extracting(Option::value).containsExactlyInAnyOrder(roundA.getId().toString(), roundB.getId().toString());
        assertThat(options.loaidot()).extracting(Option::value).doesNotContain("NCKH", "TLCN");
    }

    @Test void combinedHttpFiltersUseFinalContractNames() throws Exception {
        var expectedTopics = service.detai(topicFilter()).items();
        mvc.perform(get(url("detai")).with(user("search-chief").roles("TRUONGKHOA"))
                .param("keyword", "  sinh viên  ").param("dotdangkyid", roundA.getId().toString())
                .param("bomonid", departmentA.getId().toString()).param("loaidot", "MONHOC").param("trangthai", "CONGBO"))
                .andExpect(status().isOk()).andExpect(model().attribute("items", expectedTopics));
        var response = mvc.perform(get(url("ketqua")).with(user("search-student-a").roles("SINHVIEN"))
                .param("keyword", "Một").param("dotdangkyid", roundA.getId().toString()).param("loaidot", "MONHOC")
                .param("trangthai", "DADUYET").param("dacongbo", "true").param("diemTu", "8.25").param("diemDen", "8.25")
                .param("ngayCongboTu", "2026-10-15").param("ngayCongboDen", "2026-10-15"))
                .andExpect(status().isOk()).andReturn();
        assertThat((List<?>) response.getModelAndView().getModel().get("items")).extracting("id").containsExactly(resultA.getId());
    }
    private DetaiFilterDTO topicFilter() {
        var f = new DetaiFilterDTO(); f.setKeyword("sinh viên"); f.setDotdangkyid(roundA.getId());
        f.setBomonid(departmentA.getId()); f.setLoaidot(com.example.quanlydetaisinhvien.entity.Loaidot.MONHOC);
        f.setTrangthai(com.example.quanlydetaisinhvien.entity.TrangthaiDetai.CONGBO); return f;
    }
    private String url(String dataset) { return "/morong/timkiem/" + dataset; }
}
