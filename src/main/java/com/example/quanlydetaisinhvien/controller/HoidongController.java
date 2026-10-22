package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.HoidongService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.quanlydetaisinhvien.entity.Hoidong;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;

@Controller
@RequestMapping("/hoidong")
public class HoidongController {

    private final HoidongService service;
    private final com.example.quanlydetaisinhvien.service.DisplayLookupService lookup;
    private final com.example.quanlydetaisinhvien.service.GiaiDoan1Service choices;
    private final com.example.quanlydetaisinhvien.service.HoidongMemberService members;

    public HoidongController(HoidongService service, com.example.quanlydetaisinhvien.service.HoidongMemberService members, com.example.quanlydetaisinhvien.service.DisplayLookupService lookup, com.example.quanlydetaisinhvien.service.GiaiDoan1Service choices) {
        this.lookup = lookup;
        this.service = service;
        this.members = members; this.choices = choices;
    }

    @PostMapping("/tao")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String create(@ModelAttribute Hoidong council, RedirectAttributes flash) {
        try {
            service.save(council);
            flash.addFlashAttribute("successMessage", "Đã tạo hội đồng.");
        } catch (NghiepvuException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hoidong";
    }

    @PostMapping("/thanhvien")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String addMember(@org.springframework.web.bind.annotation.RequestParam Long hoidongid,
            @org.springframework.web.bind.annotation.RequestParam Long giangvienid,
            @org.springframework.web.bind.annotation.RequestParam com.example.quanlydetaisinhvien.entity.VaitroHoidong vaitro,
            RedirectAttributes flash) {
        try { members.add(hoidongid, giangvienid, vaitro); flash.addFlashAttribute("successMessage", "Đã thêm thành viên."); }
        catch (NghiepvuException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/hoidong";
    }

    @PostMapping("/thanhvien/xoa")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String removeMember(@org.springframework.web.bind.annotation.RequestParam Long hoidongid,
            @org.springframework.web.bind.annotation.RequestParam Long giangvienid, RedirectAttributes flash) {
        try { members.remove(hoidongid, giangvienid); flash.addFlashAttribute("successMessage", "Đã xóa thành viên."); }
        catch (NghiepvuException ex) { flash.addFlashAttribute("errorMessage", ex.getMessage()); }
        return "redirect:/hoidong";
    }

    @org.springframework.web.bind.annotation.ExceptionHandler({
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    public String invalidForm(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "ID hoặc vai trò thành viên không hợp lệ.");
        return "redirect:/hoidong";
    }

    @GetMapping
    public String list(Model model, Authentication authentication, @org.springframework.web.bind.annotation.RequestParam(defaultValue="0") int page) {
        model.addAttribute("canManage", authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority())));
        model.addAttribute("title", "Hội đồng");
        var result = service.findPage(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 25, org.springframework.data.domain.Sort.by("id").descending()));
        var councils = result.getContent();
        model.addAttribute("pageData", result);
        model.addAttribute("items", councils);
        var roster = members.forCouncils(councils.stream().map(Hoidong::getId).toList());
        model.addAttribute("roster", roster);

        if (Boolean.TRUE.equals(model.getAttribute("canManage"))) {
            var teachers = choices.dsGiangVienChoices();
            model.addAttribute("teacherOptions", teachers);
            model.addAttribute("teacherLabels", lookup.accountLabels(
                    teachers.stream().map(g -> g.getTaikhoanid()).toList()));
            model.addAttribute("roundOptions", choices.dsDotChoices());
        }

        model.addAttribute("roundNames", lookup.roundNames(
                councils.stream().map(Hoidong::getDotdangkyid).toList()));

        model.addAttribute("lecturerNames", lookup.lecturerNames(
                roster.values().stream()
                        .flatMap(java.util.Collection::stream)
                        .map(m -> m.getGiangvienid())
                        .toList()));
        return "hoidong-list";
    }
}
