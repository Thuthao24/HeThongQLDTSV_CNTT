package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.controller.ThongbaoController;
import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ThongbaoControllerSecurityTests extends ThongbaoTestSupport {
    Thongbao existing;

    @BeforeEach void seedNotice() {
        existing = notice("Thông báo kiểm thử", "Nội dung ban đầu", chief.getId(), oldDate);
    }

    @ParameterizedTest @ValueSource(strings = {"TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN"})
    void listSearchAndDetailExposeAgreedModelsForAllReaders(String role) throws Exception {
        String username = role.equals("TRUONGKHOA") ? "notice-chief" : "reader";
        mvc.perform(get("/thongbao").with(user(username).roles(role)))
                .andExpect(status().isOk()).andExpect(view().name("thongbao-list"))
                .andExpect(model().attributeExists("title", "items", "keyword", "canManage"))
                .andExpect(model().attribute("canManage", role.equals("TRUONGKHOA")));
        var search = mvc.perform(get("/thongbao").param("keyword", "  BAN ĐẦU  ").with(user(username).roles(role)))
                .andExpect(status().isOk()).andExpect(model().attribute("keyword", "BAN ĐẦU")).andReturn();
        assertThat(search.getModelAndView().getModel().get("items")).isEqualTo(List.of(
                new ThongbaoViewDTO(existing.getId(), existing.getTieude(), existing.getNoidung(), chief.getHoten(), oldDate)));
        mvc.perform(get("/thongbao/" + existing.getId()).with(user(username).roles(role)))
                .andExpect(status().isOk()).andExpect(view().name("thongbao-detail"))
                .andExpect(model().attributeExists("title", "item", "canManage"));
    }

    @Test void chiefFormModelsMatchCreateAndEditContract() throws Exception {
        mvc.perform(get("/thongbao/create").with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name("thongbao-form"))
                .andExpect(model().attributeExists("title", "thongbaoForm"))
                .andExpect(model().attribute("formMode", "CREATE"))
                .andExpect(model().attribute("formAction", "/thongbao"))
                .andExpect(model().attributeDoesNotExist("thongbaoId"));
        var edit = mvc.perform(get("/thongbao/" + existing.getId() + "/edit").with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(view().name("thongbao-form"))
                .andExpect(model().attribute("formMode", "EDIT"))
                .andExpect(model().attribute("formAction", "/thongbao/" + existing.getId() + "/edit"))
                .andExpect(model().attribute("thongbaoId", existing.getId())).andReturn();
        var input = (ThongbaoFormDTO) edit.getModelAndView().getModel().get("thongbaoForm");
        assertThat(input.getTieude()).isEqualTo(existing.getTieude());
        assertThat(input.getNoidung()).isEqualTo(existing.getNoidung());
    }

    @Test void chiefCreatesViaPostWithoutTrustingForgedMetadata() throws Exception {
        var before = LocalDateTime.now().minusSeconds(1);
        var response = mvc.perform(post("/thongbao").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("tieude", "  Thông báo mới  ").param("noidung", "Nội dung mới")
                .param("id", existing.getId().toString()).param("nguoidangid", "999")
                .param("ngaydang", "2000-01-01T00:00:00").param("nguoidangHoten", "Giả mạo"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("successMessage")).andReturn();
        Long id = Long.valueOf(response.getResponse().getRedirectedUrl().substring("/thongbao/".length()));
        assertThat(id).isNotEqualTo(existing.getId());
        em.clear();
        var saved = repository.findById(id).orElseThrow();
        assertThat(saved.getTieude()).isEqualTo("Thông báo mới");
        assertThat(saved.getNguoidangid()).isEqualTo(chief.getId());
        assertThat(saved.getNgaydang()).isBetween(before, LocalDateTime.now().plusSeconds(1));
        assertThat(repository.findById(existing.getId()).orElseThrow().getTieude()).isEqualTo("Thông báo kiểm thử");
    }

    @Test void chiefEditsOnlyPathIdAndPreservesOriginalAuthorAndDate() throws Exception {
        var author = accounts.findByTendangnhap("notice-lecturer").orElseThrow();
        existing.setNguoidangid(author.getId()); em.flush();
        var other = notice("Không sửa", "Nội dung khác", chief.getId(), oldDate);
        mvc.perform(post("/thongbao/" + existing.getId() + "/edit").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                .param("tieude", "Đã sửa").param("noidung", "Nội dung đã sửa")
                .param("id", other.getId().toString()).param("nguoidangid", chief.getId().toString())
                .param("ngaydang", "2000-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/thongbao/" + existing.getId()))
                .andExpect(flash().attributeExists("successMessage"));
        em.clear();
        var saved = repository.findById(existing.getId()).orElseThrow();
        assertThat(saved.getTieude()).isEqualTo("Đã sửa");
        assertThat(saved.getNoidung()).isEqualTo("Nội dung đã sửa");
        assertThat(saved.getNguoidangid()).isEqualTo(author.getId());
        assertThat(saved.getNgaydang()).isEqualTo(oldDate);
        assertThat(repository.findById(other.getId()).orElseThrow().getTieude()).isEqualTo("Không sửa");
    }

    @Test void chiefDeletesWithPostAndCsrfAndRedirectsToList() throws Exception {
        mvc.perform(post("/thongbao/" + existing.getId() + "/delete").with(user("notice-chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/thongbao"))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(repository.findById(existing.getId())).isEmpty();
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(Arguments.of("tieude", null), Arguments.of("tieude", ""),
                Arguments.of("tieude", " \t "), Arguments.of("tieude", "T".repeat(256)),
                Arguments.of("noidung", null), Arguments.of("noidung", ""),
                Arguments.of("noidung", " \t "), Arguments.of("noidung", "N".repeat(10001)));
    }

    @ParameterizedTest @MethodSource("invalidFields")
    void invalidCreateAndEditReturnBindingErrorsWithoutSaving(String field, String invalid) throws Exception {
        var before = noticeSnapshot();
        for (boolean editing : new boolean[]{false, true}) {
            var request = post(editing ? "/thongbao/" + existing.getId() + "/edit" : "/thongbao")
                    .with(user("notice-chief").roles("TRUONGKHOA")).with(csrf());
            if (!field.equals("tieude")) request.param("tieude", "Hợp lệ");
            if (!field.equals("noidung")) request.param("noidung", "Hợp lệ");
            if (invalid != null) request.param(field, invalid);
            mvc.perform(request).andExpect(status().isOk()).andExpect(view().name("thongbao-form"))
                    .andExpect(model().attributeHasFieldErrors("thongbaoForm", field))
                    .andExpect(model().attribute("formMode", editing ? "EDIT" : "CREATE"))
                    .andExpect(model().attribute("formAction", editing ? "/thongbao/" + existing.getId() + "/edit" : "/thongbao"));
            assertThat(noticeSnapshot()).isEqualTo(before);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"SINHVIEN", "GIANGVIEN", "TRUONGBOMON"})
    void otherRolesCannotGetFormsOrWriteEvenWithValidCsrf(String role) throws Exception {
        var before = noticeSnapshot();
        for (String url : List.of("/thongbao/create", "/thongbao/" + existing.getId() + "/edit")) {
            mvc.perform(get(url).with(user("forbidden").roles(role))).andExpect(status().isForbidden());
            assertThat(noticeSnapshot()).isEqualTo(before);
        }
        for (String url : writes()) {
            mvc.perform(post(url).with(user("forbidden").roles(role)).with(csrf())
                    .param("tieude", "Giả mạo").param("noidung", "Không được lưu"))
                    .andExpect(status().isForbidden());
            assertThat(noticeSnapshot()).isEqualTo(before);
        }
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void chiefPostsWithMissingOrInvalidCsrfAreForbiddenWithoutChanges(boolean invalidToken) throws Exception {
        var before = noticeSnapshot();
        for (String url : writes()) {
            var request = post(url).with(user("notice-chief").roles("TRUONGKHOA"))
                    .param("tieude", "Không được lưu").param("noidung", "Nội dung");
            if (invalidToken) request.with(csrf().useInvalidToken());
            mvc.perform(request).andExpect(status().isForbidden());
            assertThat(noticeSnapshot()).isEqualTo(before);
        }
    }

    @Test void anonymousUsersMustLogInAndUnsupportedRolesAreForbidden() throws Exception {
        for (String url : List.of("/thongbao", "/thongbao?keyword=test", "/thongbao/create",
                "/thongbao/" + existing.getId(), "/thongbao/" + existing.getId() + "/edit")) {
            mvc.perform(get(url).with(anonymous())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        }
        mvc.perform(post("/thongbao").with(anonymous()).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/thongbao").with(user("unknown").roles("UNKNOWN"))).andExpect(status().isForbidden());
    }

    @Test void staleChiefSessionCannotOpenFormsOrWrite() throws Exception {
        chief.setKichhoat(false); em.flush();
        var before = noticeSnapshot();
        mvc.perform(get("/thongbao").with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(status().isOk()).andExpect(model().attribute("canManage", false));
        mvc.perform(get("/thongbao/create").with(user("notice-chief").roles("TRUONGKHOA"))).andExpect(status().isForbidden());
        for (String url : writes()) {
            mvc.perform(post(url).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                    .param("tieude", "Không được lưu").param("noidung", "Nội dung"))
                    .andExpect(status().isForbidden());
            assertThat(noticeSnapshot()).isEqualTo(before);
        }
    }

    @Test void missingNoticeIs404ForEveryOperation() throws Exception {
        var before = noticeSnapshot();
        for (String suffix : List.of("", "/edit")) {
            mvc.perform(get("/thongbao/" + Long.MAX_VALUE + suffix).with(user("notice-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isNotFound()).andExpect(view().name("error"));
        }
        for (String suffix : List.of("/edit", "/delete")) {
            mvc.perform(post("/thongbao/" + Long.MAX_VALUE + suffix).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                    .param("tieude", "Mới").param("noidung", "Nội dung"))
                    .andExpect(status().isNotFound());
        }
        assertThat(noticeSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest @ValueSource(strings = {"abc", "0", "-1", "9999999999999999999999999"})
    void malformedIdsAre400WithoutChangingData(String id) throws Exception {
        var before = noticeSnapshot();
        for (String suffix : List.of("", "/edit")) {
            mvc.perform(get("/thongbao/" + id + suffix).with(user("notice-chief").roles("TRUONGKHOA")))
                    .andExpect(status().isBadRequest());
        }
        for (String suffix : List.of("/edit", "/delete")) {
            mvc.perform(post("/thongbao/" + id + suffix).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf())
                    .param("tieude", "Mới").param("noidung", "Nội dung"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(noticeSnapshot()).isEqualTo(before);
    }

    @Test void getCannotDeleteAndUnsupportedHttpMethodsCannotMutate() throws Exception {
        var before = noticeSnapshot();
        mvc.perform(get("/thongbao/" + existing.getId() + "/delete").with(user("notice-chief").roles("TRUONGKHOA")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/thongbao/" + existing.getId()).with(user("notice-chief").roles("TRUONGKHOA")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(noticeSnapshot()).isEqualTo(before);
    }

    @Test void failedCreateAndUpdateKeepFormValuesAndExposeGenericErrors() throws Exception {
        var mocked = mock(ThongbaoService.class);
        when(mocked.create(any())).thenThrow(new DataIntegrityViolationException("PRIVATE_DATABASE_ERROR"));
        when(mocked.update(any(), any())).thenThrow(new DataIntegrityViolationException("PRIVATE_DATABASE_ERROR"));
        MockMvc isolated = MockMvcBuilders.standaloneSetup(new ThongbaoController(mocked))
                .setViewResolvers(new BackendViews()).build();
        for (String url : List.of("/thongbao", "/thongbao/" + existing.getId() + "/edit")) {
            var result = isolated.perform(post(url).param("tieude", "Giữ lại").param("noidung", "Nội dung người dùng"))
                    .andExpect(status().isOk()).andExpect(view().name("thongbao-form"))
                    .andExpect(model().attributeHasErrors("thongbaoForm")).andReturn();
            var model = result.getModelAndView().getModel();
            assertThat(model.get("errorMessage").toString()).doesNotContain("PRIVATE_DATABASE_ERROR");
            assertThat(((ThongbaoFormDTO) model.get("thongbaoForm")).getTieude()).isEqualTo("Giữ lại");
        }
    }

    @Test void failedDeleteUsesErrorFlashAndReadFailureIs503WithoutDatabaseDetails() throws Exception {
        var mocked = mock(ThongbaoService.class);
        doThrow(new DataIntegrityViolationException("PRIVATE_DATABASE_ERROR")).when(mocked).deleteById(existing.getId());
        when(mocked.findById(existing.getId())).thenThrow(new DataIntegrityViolationException("PRIVATE_DATABASE_ERROR"));
        MockMvc isolated = MockMvcBuilders.standaloneSetup(new ThongbaoController(mocked))
                .setViewResolvers(new BackendViews()).build();
        isolated.perform(post("/thongbao/" + existing.getId() + "/delete"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/thongbao/" + existing.getId()))
                .andExpect(flash().attribute("errorMessage", "Chưa thể xóa thông báo. Vui lòng thử lại."));
        isolated.perform(get("/thongbao/" + existing.getId()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(model().attribute("message", "Chưa thể truy cập dữ liệu thông báo. Vui lòng thử lại."));
    }

    private List<String> writes() {
        return List.of("/thongbao", "/thongbao/" + existing.getId() + "/edit", "/thongbao/" + existing.getId() + "/delete");
    }
}
