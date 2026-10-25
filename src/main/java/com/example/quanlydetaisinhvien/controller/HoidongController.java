package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.GiangvienOptionDto;
import com.example.quanlydetaisinhvien.dto.HoidongFormDto;
import com.example.quanlydetaisinhvien.dto.ThanhvienHoidongItemDto;
import com.example.quanlydetaisinhvien.entity.Giangvien;
import com.example.quanlydetaisinhvien.entity.Hoidong;
import com.example.quanlydetaisinhvien.entity.Taikhoan;
import com.example.quanlydetaisinhvien.entity.VaitroHoidong;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.DotdangkyRepository;
import com.example.quanlydetaisinhvien.repository.GiangvienRepository;
import com.example.quanlydetaisinhvien.repository.TaikhoanRepository;
import com.example.quanlydetaisinhvien.service.DisplayLookupService;
import com.example.quanlydetaisinhvien.service.GiaiDoan1Service;
import com.example.quanlydetaisinhvien.service.HoidongMemberService;
import com.example.quanlydetaisinhvien.service.HoidongService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/hoidong")
public class HoidongController {

    private final HoidongService service;
    private final DisplayLookupService lookup;
    private final GiaiDoan1Service choices;
    private final HoidongMemberService members;
    private final DotdangkyRepository dotdangkyRepository;
    private final GiangvienRepository giangvienRepository;
    private final TaikhoanRepository taikhoanRepository;

    public HoidongController(
            HoidongService service,
            HoidongMemberService members,
            DisplayLookupService lookup,
            GiaiDoan1Service choices,
            DotdangkyRepository dotdangkyRepository,
            GiangvienRepository giangvienRepository,
            TaikhoanRepository taikhoanRepository) {
        this.service = service;
        this.members = members;
        this.lookup = lookup;
        this.choices = choices;
        this.dotdangkyRepository = dotdangkyRepository;
        this.giangvienRepository = giangvienRepository;
        this.taikhoanRepository = taikhoanRepository;
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

    @PostMapping("/thanhvien/xoa")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String removeMember(@RequestParam Long hoidongid,
                               @RequestParam Long giangvienid, RedirectAttributes flash) {
        try {
            members.remove(hoidongid, giangvienid);
            flash.addFlashAttribute("successMessage", "Đã xóa thành viên.");
        } catch (NghiepvuException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/hoidong";
    }

    @GetMapping("/them")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String themForm(Model model) {
        model.addAttribute("title", "Thêm mới hội đồng");
        HoidongFormDto form = new HoidongFormDto();

        List<ThanhvienHoidongItemDto> thanhviens = new ArrayList<>();
        ThanhvienHoidongItemDto ct = new ThanhvienHoidongItemDto();
        ct.setVaitro(VaitroHoidong.CHUTICH);
        thanhviens.add(ct);

        ThanhvienHoidongItemDto tk = new ThanhvienHoidongItemDto();
        tk.setVaitro(VaitroHoidong.THUKY);
        thanhviens.add(tk);

        
for (int i = 0; i < 1; i++) {
    ThanhvienHoidongItemDto tv = new ThanhvienHoidongItemDto();
    tv.setVaitro(VaitroHoidong.THANHVIEN);
    thanhviens.add(tv);
}


        form.setThanhviens(thanhviens);
        model.addAttribute("form", form);
        populateFormData(model);
        return "hoidong-form";
    }

    @PostMapping("/them")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String themSubmit(@ModelAttribute("form") HoidongFormDto form,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            service.luuHoidong(form);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hội đồng đánh giá thành công!");
            return "redirect:/hoidong";
        } catch (NghiepvuException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("title", "Thêm mới hội đồng");
            populateFormData(model);
            return "hoidong-form";
        }
    }

    @GetMapping("/sua/{id}")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String suaForm(@PathVariable Long id, Model model) {
        try {
            HoidongFormDto form = service.getFormDtoById(id);
            model.addAttribute("title", "Chỉnh sửa hội đồng");
            model.addAttribute("form", form);
            populateFormData(model);
            return "hoidong-form";
        } catch (NghiepvuException ex) {
            return "redirect:/hoidong";
        }
    }

    @PostMapping("/sua/{id}")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String suaSubmit(@PathVariable Long id,
                            @ModelAttribute("form") HoidongFormDto form,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        form.setId(id);
        try {
            service.luuHoidong(form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hội đồng thành công!");
            return "redirect:/hoidong";
        } catch (NghiepvuException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("title", "Chỉnh sửa hội đồng");
            populateFormData(model);
            return "hoidong-form";
        }
    }

    @PostMapping("/xoa/{id}")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String xoaSubmit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.xoaHoidong(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa hội đồng thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa hội đồng: " + ex.getMessage());
        }
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
    public String list(Model model, Authentication authentication, @RequestParam(defaultValue = "0") int page) {
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
                    teachers.stream().map(Giangvien::getTaikhoanid).toList()));
            model.addAttribute("roundOptions", choices.dsDotChoices());
        }

        model.addAttribute("roundNames", lookup.roundNames(
                councils.stream().map(Hoidong::getDotdangkyid).toList()));

        model.addAttribute("lecturerNames", lookup.lecturerNames(
                roster.values().stream()
                        .flatMap(java.util.Collection::stream)
                        .map(com.example.quanlydetaisinhvien.entity.Thanhvienhoidong::getGiangvienid)
                        .toList()));
        return "hoidong-list";
    }

    private void populateFormData(Model model) {
        model.addAttribute("dotdangkys", dotdangkyRepository.findAll());
        model.addAttribute("vaitros", VaitroHoidong.values());

        List<Giangvien> gvs = giangvienRepository.findAll();
        Map<Long, Taikhoan> tkMap = taikhoanRepository.findAll().stream()
                .collect(Collectors.toMap(Taikhoan::getId, tk -> tk, (a, b) -> a));

        List<GiangvienOptionDto> options = new ArrayList<>();
        for (Giangvien gv : gvs) {
            Taikhoan tk = tkMap.get(gv.getTaikhoanid());
            String hoten = tk != null ? tk.getHoten() : "";
            options.add(new GiangvienOptionDto(gv.getId(), gv.getMagiangvien(), hoten));
        }
        model.addAttribute("giangviens", options);
    }
}
