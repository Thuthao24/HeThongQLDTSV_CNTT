package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.DotdangkyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dotdangky")
public class DotdangkyController {

    private final DotdangkyService service;

    public DotdangkyController(DotdangkyService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model, @org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page, @org.springframework.web.bind.annotation.RequestParam(defaultValue="") String q) {
        model.addAttribute("title", "Đợt đăng ký");
        long start = System.nanoTime();
        var result = service.searchPage(q, org.springframework.data.domain.PageRequest.of(Math.max(0, page), 25, org.springframework.data.domain.Sort.by("id").descending()));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        org.slf4j.LoggerFactory
            .getLogger(DotdangkyController.class)
            .info("PERF_DOT_SEARCH_MS={}", elapsedMs);
        model.addAttribute("items", result.getContent());
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);
        return "dotdangky-list";
    }
}
