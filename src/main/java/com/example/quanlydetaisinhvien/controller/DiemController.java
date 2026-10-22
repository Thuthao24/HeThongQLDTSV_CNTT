package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.DiemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.quanlydetaisinhvien.entity.Diem;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;

@Controller
@RequestMapping("/diem")
public class DiemController {

    private final DiemService service;

    public DiemController(DiemService service) {
        this.service = service;
    }

    @PostMapping("/tao")
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON','GIANGVIEN')")
    public String create(@ModelAttribute Diem score, RedirectAttributes flash) {
        try {
            service.save(score);
            flash.addFlashAttribute("successMessage", "Đã nhập điểm thành phần.");
        } catch (NghiepvuException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/diem";
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    public String invalidForm(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "ID đăng ký, giảng viên hoặc điểm không hợp lệ.");
        return "redirect:/diem";
    }

    @GetMapping
    public String list(Model model, Authentication authentication, @org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page) {
        model.addAttribute("canManage", authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority())));
        model.addAttribute("title", "Điểm");
        try {
            model.addAttribute("lecturerId", service.currentLecturerId());

            var gradeOptions = service.availableGradeOptions();
            model.addAttribute("gradeRounds", gradeOptions.rounds());
            model.addAttribute("gradeTopics", gradeOptions.topics());
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            model.addAttribute("lecturerId", null);
        }
        var result = service.findPage(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 25, org.springframework.data.domain.Sort.by("id").descending()));
        model.addAttribute("items", result.getContent());
        model.addAttribute("pageData", result);
        return "diem-list";
    }
}
