
package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.entity.Sinhvien;
import com.example.quanlydetaisinhvien.service.SinhvienService;
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
@RequestMapping("/sinhvien")
public class SinhvienController {

    private final SinhvienService service;
    private final DisplayLookupService lookup;

    public SinhvienController(
            SinhvienService service,
            DisplayLookupService lookup) {

        this.service = service;
        this.lookup = lookup;
    }

    // ========================================
    // 1. DANH SÁCH SINH VIÊN
    // ========================================

    @GetMapping
    public String list(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String q) {

        model.addAttribute("title", "Sinh viên");

        var pageable = PageRequest.of(
                Math.max(0, page),
                25,
                Sort.by("id").descending()
        );

        var result = service.searchPage(q, pageable);

        List<Sinhvien> items = result.getContent();

        model.addAttribute("items", items);

        // Lấy danh sách ID tài khoản
        List<Long> accountIds = items.stream()
                .map(Sinhvien::getTaikhoanid)
                .toList();

        // Lấy danh sách ID bộ môn
        List<Long> departmentIds = items.stream()
                .map(Sinhvien::getBomonid)
                .toList();

        // Họ tên sinh viên
        model.addAttribute(
                "accountNames",
                lookup.accountNames(accountIds)
        );

        // Tên đăng nhập, email,
        // vai trò và tình trạng tài khoản
        model.addAttribute(
                "accountProfiles",
                lookup.accountProfiles(accountIds)
        );

        // Tên bộ môn thay vì ID
        model.addAttribute(
                "departmentNames",
                lookup.departmentNames(departmentIds)
        );

        // Tìm kiếm và phân trang
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);

        return "sinhvien-list";
    }

    // ========================================
    // 2. XEM CHI TIẾT SINH VIÊN
    // ========================================

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            Model model) {

        Sinhvien person = service.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Không tìm thấy sinh viên"
                        )
                );

        // Lấy tài khoản liên kết
        var accounts = lookup.accountProfiles(
                List.of(person.getTaikhoanid())
        );

        // Lấy tên bộ môn
        var departments = lookup.departmentNames(
                List.of(person.getBomonid())
        );

        model.addAttribute(
                "title",
                "Thông tin sinh viên"
        );

        model.addAttribute(
                "kind",
                "Sinh viên"
        );

        model.addAttribute(
                "person",
                person
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
                "sidebarActive",
                "sinhvien"
        );

        model.addAttribute(
                "backUrl",
                "/sinhvien"
        );

        return "person-detail";
    }
}
