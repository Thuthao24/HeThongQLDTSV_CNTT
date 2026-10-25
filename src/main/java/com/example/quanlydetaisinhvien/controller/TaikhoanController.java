
package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.entity.Taikhoan;
import com.example.quanlydetaisinhvien.service.TaikhoanService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/taikhoan")
public class TaikhoanController {

    private final TaikhoanService service;

    public TaikhoanController(TaikhoanService service) {
        this.service = service;
    }

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
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy tài khoản"
                        )
                );

        model.addAttribute("title", "Thông tin cá nhân");
        model.addAttribute("profile", profile);

        return "taikhoan-canhan";
    }
}
