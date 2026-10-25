
package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.entity.Giangvien;
import com.example.quanlydetaisinhvien.service.GiangvienService;
import com.example.quanlydetaisinhvien.service.DisplayLookupService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/giangvien")
public class GiangvienController {

    private final GiangvienService service;
    private final DisplayLookupService lookup;

    public GiangvienController(
            GiangvienService service,
            DisplayLookupService lookup) {

        this.service = service;
        this.lookup = lookup;
    }

    // ========================================
    // 1. DANH SÁCH GIẢNG VIÊN
    // ========================================

    @GetMapping
    public String list(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String q) {

        model.addAttribute("title", "Giảng viên");

        var pageable = PageRequest.of(
                Math.max(0, page),
                25,
                Sort.by("id").descending()
        );

        var result = service.searchPage(q, pageable);

        List<Giangvien> items = result.getContent();

        // Danh sách giảng viên
        model.addAttribute("items", items);

        // Họ và tên từ bảng tài khoản
        model.addAttribute(
                "accountNames",
                lookup.accountNames(
                        items.stream()
                                .map(Giangvien::getTaikhoanid)
                                .toList()
                )
        );

        // Tên đăng nhập, email, vai trò,
        // trạng thái tài khoản
        model.addAttribute(
                "accountProfiles",
                lookup.accountProfiles(
                        items.stream()
                                .map(Giangvien::getTaikhoanid)
                                .toList()
                )
        );

        // Tên bộ môn thay cho ID
        model.addAttribute(
                "departmentNames",
                lookup.departmentNames(
                        items.stream()
                                .map(Giangvien::getBomonid)
                                .toList()
                )
        );

        // Phân trang và tìm kiếm
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);

        return "giangvien-list";
    }

    // ========================================
    // 2. XEM CHI TIẾT GIẢNG VIÊN
    // ========================================

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model) {

        // Kiểm tra giảng viên tồn tại
        Giangvien person = service.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy giảng viên"
                        )
                );

        // Lấy thông tin tài khoản liên kết
        var accounts = lookup.accountProfiles(
                List.of(person.getTaikhoanid())
        );

        // Lấy tên bộ môn
        var departments = lookup.departmentNames(
                List.of(person.getBomonid())
        );

        model.addAttribute(
                "title", "Thông tin giảng viên"
        );

        model.addAttribute(
                "kind", "Giảng viên"
        );

        model.addAttribute(
                "person", person
        );

        model.addAttribute(
                "account",
                accounts.get(person.getTaikhoanid())
        );

        model.addAttribute(
                "departmentName",
                departments.getOrDefault(
                        person.getBomonid(),
                        "Chưa xác định"
                )
        );

        model.addAttribute(
                "sidebarActive", "giangvien"
        );

        model.addAttribute(
                "backUrl", "/giangvien"
        );

        return "person-detail";
    }
}
