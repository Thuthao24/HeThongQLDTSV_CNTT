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
    @ParameterizedTest @ValueSource(strings = {"detai"})
    void endpointsExposeAgreedModelAndWorkingExportLinks(String dataset) throws Exception {
        var response = mvc.perform(get(url(dataset)).with(user("search-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name(dataset.equals("detai") ? "detai-list" : "morong/timkiem-" + dataset))
                .andExpect(model().attributeExists("title", "filter", "items", "pageInfo", "filterOptions", "exportOptions")).andReturn();
        var model = response.getModelAndView().getModel();
        var export = (ExportOptions) model.get("exportOptions");
        assertThat(export.isEnabled()).isTrue();
        assertThat(export.getFormats()).containsExactly("xlsx", "pdf");
        assertThat(export.getExcelUrl()).startsWith("/morong/xuat/" + dataset + ".xlsx?");
        assertThat(export.getPdfUrl()).startsWith("/morong/xuat/" + dataset + ".pdf?");
        assertThat(((PageInfo) model.get("pageInfo")).size()).isEqualTo(dataset.equals("detai") ? 25 : 20);
        assertThat(((PageInfo) model.get("pageInfo")).number()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN", "UNKNOWN"})
    void httpRoleMatrixMatchesExistingModuleReadPermissions(String role) throws Exception {
        for (String dataset : List.of("detai")) {
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
        for (String dataset : List.of("detai")) {
            mvc.perform(get(url(dataset)).with(anonymous())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
            mvc.perform(post(url(dataset)).with(user("search-chief").roles("TRUONGKHOA")).with(csrf())).andExpect(status().is(dataset.equals("detai") ? 405 : 403));
        }
        // Statistics is now implemented and its role matrix is covered by ThongkeTests.
        for (String url : List.of("/morong/email"))
            mvc.perform(get(url).with(user("search-chief").roles("TRUONGKHOA"))).andExpect(status().isForbidden());
    }

    static Stream<Arguments> invalidFilters() {
        return Stream.of(
            Arguments.of("detai", "dotdangkyid", "0"), Arguments.of("detai", "bomonid", "-1"),
            Arguments.of("detai", "giangviendexuatid", "-1"), Arguments.of("detai", "giangvienhuongdanid", "0"),
            Arguments.of("detai", "dotdangkyid", "abc"), Arguments.of("detai", "loaidot", "LOAIDETAI"),
            Arguments.of("detai", "trangthai", "DONG"), Arguments.of("detai", "sort", "matkhau,asc"),
            Arguments.of("detai", "sort", "id;drop table detai,desc"), Arguments.of("detai", "page", "-1"),
            Arguments.of("detai", "size", "0"), Arguments.of("detai", "size", "101"),
            Arguments.of("detai", "page", "99999999999999999999"), Arguments.of("detai", "keyword", "X".repeat(256))
        );
    }

    @ParameterizedTest @MethodSource("invalidFilters")
    void malformedFilterReturns400WithModelErrorsAndNoResults(String dataset, String field, String value) throws Exception {
        var response = mvc.perform(get(url(dataset)).with(user("search-chief").roles("TRUONGKHOA")).param(field, value))
                .andExpect(status().isBadRequest()).andExpect(view().name(dataset.equals("detai") ? "detai-list" : "morong/timkiem-" + dataset))
                .andExpect(model().attributeHasErrors("filter")).andExpect(model().attributeExists("pageInfo", "filterOptions", "exportOptions"))
                .andReturn();
        assertThat((List<?>) response.getModelAndView().getModel().get("items")).isEmpty();
    }

    @Test void excessiveOffsetAndInvalidSortAreRejectedByHttpAndService() throws Exception {
        mvc.perform(get(url("detai")).with(user("search-chief").roles("TRUONGKHOA"))
                .param("page", "2147483647").param("size", "100"))
                .andExpect(status().isBadRequest());
        auth("search-chief", "TRUONGKHOA");
        var topicFilter = new DetaiFilterDTO(); topicFilter.setSort("matkhau,asc");
        assertThatThrownBy(() -> service.detai(topicFilter)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void combinedHttpFiltersUseFinalContractNames() throws Exception {
        var expectedTopics = service.detai(topicFilter()).items().stream().map(DetaiRow::id).toList();
        mvc.perform(get(url("detai")).with(user("search-chief").roles("TRUONGKHOA"))
                .param("keyword", "  sinh viên  ").param("dotdangkyid", roundA.getId().toString())
                .param("bomonid", departmentA.getId().toString()).param("loaidot", "MONHOC").param("trangthai", "CONGBO"))
                .andExpect(status().isOk()).andExpect(result -> assertThat((List<?>) result.getModelAndView().getModel().get("items"))
                        .extracting("id").containsExactlyElementsOf(expectedTopics));
    }
    private DetaiFilterDTO topicFilter() {
        var f = new DetaiFilterDTO(); f.setKeyword("sinh viên"); f.setDotdangkyid(roundA.getId());
        f.setBomonid(departmentA.getId()); f.setLoaidot(com.example.quanlydetaisinhvien.entity.Loaidot.MONHOC);
        f.setTrangthai(com.example.quanlydetaisinhvien.entity.TrangthaiDetai.CONGBO); return f;
    }
    private String url(String dataset) { return dataset.equals("detai") ? "/detai" : "/morong/timkiem/" + dataset; }
}
