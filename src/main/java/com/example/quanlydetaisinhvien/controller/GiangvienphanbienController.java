package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.PhanbienFormDto;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.GiangvienphanbienService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/phanbien")
public class GiangvienphanbienController {

    private final GiangvienphanbienService service;

    public GiangvienphanbienController(GiangvienphanbienService service) {
        this.service = service;
    }

    // 1. Xem danh sách phân công (TRUONGKHOA, TRUONGBOMON, GIANGVIEN đều xem được)
    @GetMapping
    @PreAuthorize("hasAnyRole('TRUONGKHOA', 'TRUONGBOMON', 'GIANGVIEN')")
    public String list(Model model) {
        model.addAttribute("title", "Giảng viên phản biện");
        model.addAttribute("items", service.findAllViewDto());
        return "phanbien-list";
    }

    // 2. Mở form phân công mới (CHỈ TRƯỞNG KHOA & TRƯỞNG BỘ MÔN MỚI ĐƯỢC VÀO)
    @GetMapping("/them")
    @PreAuthorize("hasAnyRole('TRUONGKHOA', 'TRUONGBOMON')")
    public String themForm(@RequestParam(value = "dangkydetaiid", required = false) Long dangkydetaiid,
                           Model model) {
        model.addAttribute("title", "Phân công giảng viên phản biện");
        PhanbienFormDto form = new PhanbienFormDto();
        if (dangkydetaiid != null) {
            form.setDangkydetaiid(dangkydetaiid);
        }
        model.addAttribute("form", form);
        populateOptions(model);
        return "phanbien-form";
    }

    // 3. Thực hiện phân công (CHỈ TRƯỞNG KHOA & TRƯỞNG BỘ MÔN)
    @PostMapping("/them")
    @PreAuthorize("hasAnyRole('TRUONGKHOA', 'TRUONGBOMON')")
    public String themSubmit(@ModelAttribute("form") PhanbienFormDto form,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            service.phanCong(form);
            redirectAttributes.addFlashAttribute("successMessage", "Phân công giảng viên phản biện thành công!");
            return "redirect:/phanbien";
        } catch (NghiepvuException ex) {
            model.addAttribute("title", "Phân công giảng viên phản biện");
            model.addAttribute("errorMessage", ex.getMessage());
            populateOptions(model);
            return "phanbien-form";
        }
    }

    // 4. Xóa phân công (CHỈ TRƯỞNG KHOA & TRƯỞNG BỘ MÔN)
    @PostMapping("/xoa/{id}")
    @PreAuthorize("hasAnyRole('TRUONGKHOA', 'TRUONGBOMON')")
    public String xoaSubmit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phân công giảng viên phản biện thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa phân công này: " + ex.getMessage());
        }
        return "redirect:/phanbien";
    }

    private void populateOptions(Model model) {
        model.addAttribute("dangkydetaiOptions", service.getDangkydetaiOptions());
        model.addAttribute("giangvienOptions", service.getGiangvienOptions());
    }
}