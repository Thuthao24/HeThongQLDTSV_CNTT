package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.BomonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/bomon")
public class BomonController {

    private final BomonService service;

    public BomonController(BomonService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model, @org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page, @org.springframework.web.bind.annotation.RequestParam(defaultValue="") String q) {
        model.addAttribute("title", "Bộ môn");
        var result = service.searchPage(q, org.springframework.data.domain.PageRequest.of(Math.max(0, page), 25, org.springframework.data.domain.Sort.by("id").descending()));
        model.addAttribute("items", result.getContent());
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);
        return "bomon-list";
    }
}
