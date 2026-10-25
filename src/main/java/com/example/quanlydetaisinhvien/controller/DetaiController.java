
package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.entity.Detai;
import com.example.quanlydetaisinhvien.service.DetaiService;
import com.example.quanlydetaisinhvien.service.DisplayLookupService;
import com.example.quanlydetaisinhvien.service.GiaiDoan1Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/detai")
public class DetaiController {

    private final DetaiService service;
    private final GiaiDoan1Service gd1;
    private final DisplayLookupService lookup;

    public DetaiController(
            DetaiService service,
            GiaiDoan1Service gd1,
            DisplayLookupService lookup) {

        this.service = service;
        this.gd1 = gd1;
        this.lookup = lookup;
    }

    @GetMapping
    public String list(
            Model model,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String q) {

        model.addAttribute("title", "Đề tài");

        // 1. Lấy danh sách đề tài có phân trang
        var pageable = PageRequest.of(
                Math.max(0, page),
                25,
                Sort.by("id").descending()
        );

        var result = service.searchPage(q, pageable);
        List<Detai> items = result.getContent();

        model.addAttribute("items", items);
        model.addAttribute("pageData", result);
        model.addAttribute("q", q);

        // 2. Hiển thị TÊN ĐỢT thay vì ID
        model.addAttribute(
                "roundNames",
                lookup.roundNames(
                        items.stream()
                                .map(Detai::getDotdangkyid)
                                .toList()
                )
        );

        // 3. Hiển thị TÊN BỘ MÔN thay vì ID
        model.addAttribute(
                "departmentNames",
                lookup.departmentNames(
                        items.stream()
                                .map(Detai::getBomonid)
                                .toList()
                )
        );

        // 4. Hiển thị HỌ TÊN GIẢNG VIÊN đề xuất
        model.addAttribute(
                "lecturerNames",
                lookup.lecturerNames(
                        items.stream()
                                .map(Detai::getGiangviendexuatid)
                                .toList()
                )
        );

        // 5. Kiểm tra quyền đề xuất đề tài
        model.addAttribute(
                "canProposeTopic",
                gd1.canPropose(authentication.getName())
        );

        // 6. Kiểm tra những đề tài được phép sửa/xóa
        model.addAttribute(
                "editableIds",
                gd1.editableTopicIds(
                        authentication.getName(),
                        items
                )
        );

        // Phải return cuối cùng
        return "detai-list";
    }
}
