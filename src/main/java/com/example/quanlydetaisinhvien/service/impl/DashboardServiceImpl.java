package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final DetaiRepository detai;
    private final HoidongRepository hoidong;
    private final SinhvienRepository sinhvien;
    private final GiangvienRepository giangvien;
    private final DotdangkyRepository dot;

    public DashboardServiceImpl(DetaiRepository detai, HoidongRepository hoidong,
            SinhvienRepository sinhvien, GiangvienRepository giangvien, DotdangkyRepository dot) {
        this.detai = detai; this.hoidong = hoidong;
        this.sinhvien = sinhvien; this.giangvien = giangvien; this.dot = dot;
    }

    @Override
    @Transactional(readOnly = true)
    public void themSoLieuTheoVaiTro(Model model, Authentication authentication) {
        if (authentication == null) return;
        boolean truongKhoa = has(authentication, "TRUONGKHOA");
        boolean truongBoMon = has(authentication, "TRUONGBOMON");
        boolean gv = has(authentication, "GIANGVIEN");
        if (truongKhoa || truongBoMon || gv) {
            model.addAttribute("soDetai", detai.count());
            model.addAttribute("soHoidong", hoidong.count());
        }
        if (truongKhoa || truongBoMon) {
            model.addAttribute("soSinhvien", sinhvien.count());
            model.addAttribute("soGiangvien", giangvien.count());
        }
        if (truongKhoa) model.addAttribute("soDotdangky", dot.count());
    }
    private boolean has(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
