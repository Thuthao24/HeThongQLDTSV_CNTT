package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
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
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/403")
    public String forbidden() { return "403"; }
}
