
package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.entity.Taikhoan;
import com.example.quanlydetaisinhvien.entity.Giangvien;
import com.example.quanlydetaisinhvien.entity.Sinhvien;
import com.example.quanlydetaisinhvien.service.TaikhoanService;
import com.example.quanlydetaisinhvien.service.DisplayLookupService;
import com.example.quanlydetaisinhvien.repository.GiangvienRepository;
import com.example.quanlydetaisinhvien.repository.SinhvienRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/taikhoan")
public class TaikhoanController {

    private final TaikhoanService service;
    private final GiangvienRepository giangviens;
    private final SinhvienRepository sinhviens;
    private final DisplayLookupService lookup;

    public TaikhoanController(
            TaikhoanService service,
            GiangvienRepository giangviens,
            SinhvienRepository sinhviens,
            DisplayLookupService lookup) {
        this.service = service;
        this.giangviens = giangviens;
        this.sinhviens = sinhviens;
        this.lookup = lookup;
    }

    // Trang tài khoản của người đang đăng nhập
    @GetMapping({"", "/canhan"})
    public String myProfile(
            Authentication authentication,
            Model model) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Taikhoan profile = service
                .layTheoTenDangNhap(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tài khoản"
                ));

        Giangvien teacher = giangviens
                .findByTaikhoanid(profile.getId())
                .orElse(null);

        Sinhvien student = sinhviens
                .findByTaikhoanid(profile.getId())
                .orElse(null);

        String departmentName = "Chưa xác định";

        Long departmentId = teacher != null
                ? teacher.getBomonid()
                : student != null
                    ? student.getBomonid()
                    : null;

        if (departmentId != null) {
            departmentName = lookup.departmentNames(
                    List.of(departmentId)
            ).getOrDefault(departmentId, "Chưa xác định");
        }

        model.addAttribute("title", "Tài khoản của tôi");
        model.addAttribute("profile", profile);
        model.addAttribute("teacher", teacher);
        model.addAttribute("student", student);
        model.addAttribute("departmentName", departmentName);

        return "taikhoan-canhan";
    }

    // Chỉ Trưởng khoa quản lý danh sách tài khoản
    @GetMapping("/quanly")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String list(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String q) {

        var result = service.searchPage(
                q,
                PageRequest.of(
                        Math.max(0, page),
                        25,
                        Sort.by("id").descending()
                )
        );

        model.addAttribute("title", "Quản lý tài khoản");
        model.addAttribute("items", result.getContent());
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);

        return "taikhoan-list";
    }
}
