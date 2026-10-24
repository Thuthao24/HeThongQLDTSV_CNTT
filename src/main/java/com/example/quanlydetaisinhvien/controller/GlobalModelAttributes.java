
package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.TaikhoanService;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import com.example.quanlydetaisinhvien.service.ThongbaoReadService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Set;

@ControllerAdvice
public class GlobalModelAttributes
        implements WebMvcConfigurer, HandlerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalModelAttributes.class);

    private static final Set<String> HEADER_VIEWS = Set.of(
            "index", "taikhoan-list", "bomon-list",
            "detai-list", "dotdangky-list",
            "giai-doan-1/dot-form",
            "giai-doan-1/detai-form",
            "quanly/bomon-form",
            "quanly/person-form",
            "giangvien-list", "sinhvien-list",
            "nhom-list", "dangkydetai-list",
            "hoidong-list", "diem-list",
            "ketqua-list",
            "thongbao-list", "thongbao-detail",
            "thongbao-form",
            "morong/timkiem-detai",
            "morong/timkiem-dotdangky",
            "morong/timkiem-ketqua",
            "morong/thongke"
    );

    private static final Set<String> NOTICE_READ_ROLES = Set.of(
            "ROLE_TRUONGKHOA",
            "ROLE_TRUONGBOMON",
            "ROLE_GIANGVIEN",
            "ROLE_SINHVIEN"
    );

    private final TaikhoanService taikhoanService;
    private final ThongbaoService thongbaoService;
    private final ThongbaoReadService noticeReads;

    public GlobalModelAttributes(
            TaikhoanService taikhoanService,
            ThongbaoService thongbaoService,
            ThongbaoReadService noticeReads) {

        this.taikhoanService = taikhoanService;
        this.thongbaoService = thongbaoService;
        this.noticeReads = noticeReads;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this);
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {

        if (modelAndView == null
                || modelAndView.getViewName() == null
                || !HEADER_VIEWS.contains(modelAndView.getViewName())
                || response.getStatus() >= 400) {
            return;
        }

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getAuthorities().stream()
                    .noneMatch(a -> NOTICE_READ_ROLES.contains(
                            a.getAuthority()))) {
            return;
        }

        modelAndView.addObject("headerUnreadCount", 0L);
        modelAndView.addObject("headerUnreadIds", Set.of());

        // Truong khoa quan ly thong bao, khong nhan qua chuong.
        if (authentication.getAuthorities().stream()
                .anyMatch(a ->
                    "ROLE_TRUONGKHOA".equals(a.getAuthority()))) {

            modelAndView.addObject("headerThongbaos", List.of());
            return;
        }

        try {
            // Giu logging hieu nang tu main.
            long start = System.nanoTime();

            var notices = thongbaoService.findLatestForHeader();

            log.info("PERF_HEADER_MS={}",
                    (System.nanoTime() - start) / 1_000_000);

            modelAndView.addObject("headerThongbaos", notices);

            // Giu nghiep vu trang thai da doc cua Module 15.
            try {
                var state = noticeReads.headerState(notices);

                modelAndView.addObject(
                        "headerUnreadCount",
                        state.unreadCount()
                );

                modelAndView.addObject(
                        "headerUnreadIds",
                        state.unreadIds()
                );

            } catch (DataAccessException | AccessDeniedException ex) {
                // Khong lam hong trang khi trang thai doc bi loi.
                log.warn(
                    "Unable to load per-account notice read status"
                );
            }

        } catch (DataAccessException ex) {
            // Loi chuong khong duoc chan trang dang render.
            log.warn(
                "Unable to load notices for the portal header"
            );

            modelAndView.addObject(
                    "headerThongbaos",
                    List.of()
            );
        }
    }

    @ModelAttribute
    public void addCurrentUser(
            Model model,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }

        // Giu logging hieu nang tu main.
        long start = System.nanoTime();

        taikhoanService
                .layTheoTenDangNhap(authentication.getName())
                .ifPresent(tk -> {
                    model.addAttribute("hoten", tk.getHoten());
                    model.addAttribute(
                            "tendangnhap",
                            tk.getTendangnhap()
                    );
                    model.addAttribute(
                            "vaitro",
                            tk.getVaitro().name()
                    );
                });

        log.info("PERF_USER_MS={}",
                (System.nanoTime() - start) / 1_000_000);
    }
}
