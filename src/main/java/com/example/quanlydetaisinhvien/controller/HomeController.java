package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.DashboardService;
import com.example.quanlydetaisinhvien.service.NhomsinhvienService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final DashboardService dashboardService;
    private final NhomsinhvienService nhomsinhvienService;

    public HomeController(DashboardService dashboardService, NhomsinhvienService nhomsinhvienService) {
        this.dashboardService = dashboardService;
        this.nhomsinhvienService = nhomsinhvienService;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        dashboardService.themSoLieuTheoVaiTro(model, authentication);
        if (authentication != null) {
            model.addAttribute("tendangnhap", authentication.getName());
            String role = authentication.getAuthorities().stream()
                    .filter(a -> a.getAuthority().startsWith("ROLE_"))
                    .map(a -> a.getAuthority().substring(5))
                    .findFirst().orElse("");
            model.addAttribute("vaitro", role);
            if ("SINHVIEN".equals(role)) {
                model.addAttribute("soLoiMoiDangCho", nhomsinhvienService.demSoLoiMoiDangCho(authentication.getName()));
            }
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/403")
    public String forbidden() { return "403"; }
}
