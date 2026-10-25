package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.controller.GlobalModelAttributes;
import com.example.quanlydetaisinhvien.service.TaikhoanService;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalModelAttributesTests {
    final ThongbaoService service = mock(ThongbaoService.class);
    final com.example.quanlydetaisinhvien.service.ThongbaoReadService reads=mock(com.example.quanlydetaisinhvien.service.ThongbaoReadService.class);
    final GlobalModelAttributes attributes = new GlobalModelAttributes(mock(TaikhoanService.class), service, reads);
    @org.junit.jupiter.api.BeforeEach void readStatus() {
        when(reads.headerState(anyList())).thenReturn(new com.example.quanlydetaisinhvien.dto.ThongbaoBellDTO.State(0,java.util.Set.of()));
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    void reader() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "reader", "unused", List.of(new SimpleGrantedAuthority("ROLE_SINHVIEN"))));
    }

    @Test void chiefIsManagerAndNeverReceivesHeaderNotices() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "chief", "unused", List.of(new SimpleGrantedAuthority("ROLE_TRUONGKHOA"))));
        var view=new ModelAndView("thongbao-list");populate(view);
        assertThat(view.getModel()).containsEntry("headerThongbaos",List.of());verifyNoInteractions(service);
    }

    void populate(ModelAndView view) {
        attributes.postHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object(), view);
    }

    @ParameterizedTest @ValueSource(strings = {"index", "taikhoan-list", "bomon-list", "detai-list",
            "dotdangky-list", "giangvien-list", "sinhvien-list", "nhom-list", "dangkydetai-list",
            "hoidong-list", "diem-list", "ketqua-list", "thongbao-list", "thongbao-detail", "thongbao-form",
            "morong/timkiem-detai", "morong/thongke"})
    void everyExistingPortalViewUsesTheSameHeaderAttribute(String name) {
        reader();
        when(service.findLatestForHeader()).thenReturn(List.of());
        var view = new ModelAndView(name);
        populate(view);
        assertThat(view.getModel()).containsEntry("headerThongbaos", List.of());
        verify(service).findLatestForHeader();
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"login", "403", "error", "redirect:/thongbao",
            "forward:/login", "unknown-view", "morong/timkiem-dotdangky", "morong/timkiem-ketqua"})
    void noNoticeQueryForViewsWithoutPortalHeader(String name) {
        reader();
        var view = new ModelAndView(); view.setViewName(name);
        populate(view);
        assertThat(view.getModel()).doesNotContainKey("headerThongbaos");
        verifyNoInteractions(service);
    }

    @Test void responseBodiesDoNotLoadNotices() {
        reader(); populate(null); verifyNoInteractions(service);
    }

    @Test void anonymousUnauthenticatedAndUnknownRolesDoNotLoadNotices() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        var unknown = UsernamePasswordAuthenticationToken.authenticated("unknown", "unused",
                List.of(new SimpleGrantedAuthority("ROLE_UNKNOWN")));
        for (var authentication : Arrays.asList(null, anonymous, unknown,
                UsernamePasswordAuthenticationToken.unauthenticated("reader", "unused"))) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            var view = new ModelAndView("index");
            populate(view);
            assertThat(view.getModel()).doesNotContainKey("headerThongbaos");
        }
        verifyNoInteractions(service);
    }

    @Test void failedResponsesDoNotLoadNoticesEvenIfTheyNameAPortalView() {
        reader();
        var response = new MockHttpServletResponse(); response.setStatus(503);
        var view = new ModelAndView("thongbao-form");
        attributes.postHandle(new MockHttpServletRequest(), response, new Object(), view);
        assertThat(view.getModel()).doesNotContainKey("headerThongbaos");
        verifyNoInteractions(service);
    }

    @Test void optionalHeaderDatabaseFailurePreservesThePageAndItsExistingModel() {
        reader();
        when(service.findLatestForHeader()).thenThrow(new DataAccessResourceFailureException("PRIVATE_DATABASE_ERROR"));
        var view = new ModelAndView("thongbao-form"); view.addObject("formMode", "CREATE");
        populate(view);
        assertThat(view.getViewName()).isEqualTo("thongbao-form");
        assertThat(view.getModel()).containsEntry("formMode", "CREATE").containsEntry("headerThongbaos", List.of());
        assertThat(view.getModel().toString()).doesNotContain("PRIVATE_DATABASE_ERROR");
    }
}
