package com.example.quanlydetaisinhvien.service;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

public interface DashboardService {
    void themSoLieuTheoVaiTro(Model model, Authentication authentication);
}
