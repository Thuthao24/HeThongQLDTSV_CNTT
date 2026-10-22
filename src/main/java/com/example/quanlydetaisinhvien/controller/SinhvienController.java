package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.SinhvienService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/sinhvien")
public class SinhvienController {

    private final SinhvienService service;
    private final com.example.quanlydetaisinhvien.service.DisplayLookupService lookup;

    public SinhvienController(SinhvienService service, com.example.quanlydetaisinhvien.service.DisplayLookupService lookup) {
        this.lookup = lookup;
        this.service = service;
    }

    @GetMapping
    public String list(Model model, @org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page, @org.springframework.web.bind.annotation.RequestParam(defaultValue="") String q) {
        model.addAttribute("title", "Sinh viên");
        var result = service.searchPage(q, org.springframework.data.domain.PageRequest.of(Math.max(0, page), 25, org.springframework.data.domain.Sort.by("id").descending()));
        model.addAttribute("items", result.getContent());
        model.addAttribute("accountNames", lookup.accountNames(
                result.getContent().stream().map(s -> s.getTaikhoanid()).toList()));
        model.addAttribute("departmentNames", lookup.departmentNames(
                result.getContent().stream().map(s -> s.getBomonid()).toList()));
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);
        return "sinhvien-list";
    }
}
