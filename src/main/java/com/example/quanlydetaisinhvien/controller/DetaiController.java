package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.DetaiService;
import com.example.quanlydetaisinhvien.service.GiaiDoan1Service;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/detai")
public class DetaiController {

    private final DetaiService service;
    private final GiaiDoan1Service gd1;

    public DetaiController(DetaiService service, GiaiDoan1Service gd1) {
        this.service = service;
        this.gd1=gd1;
    }

    @GetMapping
    public String list(Model model, Authentication authentication, @org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page, @org.springframework.web.bind.annotation.RequestParam(defaultValue="") String q) {
        model.addAttribute("title", "Đề tài");
        var result = service.searchPage(q, org.springframework.data.domain.PageRequest.of(Math.max(0, page), 25, org.springframework.data.domain.Sort.by("id").descending()));
        var items = result.getContent();
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        model.addAttribute("canProposeTopic", gd1.canPropose(authentication.getName()));
        model.addAttribute("editableIds", gd1.editableTopicIds(authentication.getName(), items));
        return "detai-list";
        
model.addAttribute("roundNames",
    lookup.roundNames(items.stream()
        .map(Detai::getDotdangkyid).toList()));

model.addAttribute("departmentNames",
    lookup.departmentNames(items.stream()
        .map(Detai::getBomonid).toList()));

model.addAttribute("lecturerNames",
    lookup.lecturerNames(items.stream()
        .map(Detai::getGiangviendexuatid).toList()));

    }
}
