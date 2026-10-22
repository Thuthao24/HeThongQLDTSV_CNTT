package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.DangKyDeTaiDto;
import com.example.quanlydetaisinhvien.dto.DangKyDeTaiRequest;
import com.example.quanlydetaisinhvien.dto.NhomSinhVienDto;
import com.example.quanlydetaisinhvien.entity.Detai;
import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.DetaiService;
import com.example.quanlydetaisinhvien.service.DangkydetaiService;
import com.example.quanlydetaisinhvien.service.NhomsinhvienService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/dangkydetai")
public class DangkydetaiController {

    private final DangkydetaiService service;
    private final DetaiService detaiService;
    private final NhomsinhvienService nhomsinhvienService;

    public DangkydetaiController(
            DangkydetaiService service,
            DetaiService detaiService,
            NhomsinhvienService nhomsinhvienService) {
        this.service = service;
        this.detaiService = detaiService;
        this.nhomsinhvienService = nhomsinhvienService;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(value = "detaiId", required = false) Long preSelectedDetaiId,
                       Authentication authentication,
                       @RequestParam(value = "page", defaultValue = "0") int page) {
        page = Math.max(0, page);
        model.addAttribute("page", page);
        model.addAttribute("title", "Đăng ký đề tài");

        String username = null;
        String cleanRole = "";
        if (authentication != null) {
            username = authentication.getName();
            model.addAttribute("tendangnhap", username);
            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("");
            cleanRole = role.replace("ROLE_", "");
            model.addAttribute("vaitro", cleanRole);
        }

        DangKyDeTaiRequest dangKyRequest = new DangKyDeTaiRequest();
        if (preSelectedDetaiId != null) {
            dangKyRequest.setDetaiid(preSelectedDetaiId);
        }

        if ("TRUONGKHOA".equals(cleanRole)) {
            model.addAttribute("items", service.layDanhSachDangKyDto(page));
            model.addAttribute("dsNhom", nhomsinhvienService.layNhomChoQuanTri(100));
            model.addAttribute("dsDetaiCongBo", detaiService.layDetaiCongBo());
            model.addAttribute("daCoNhom", true);
            model.addAttribute("laNhomTruong", true);
            model.addAttribute("nhomCuaToi", null);
            model.addAttribute("dangKyCuaNhom", null);
        } else {
            NhomSinhVienDto nhomCuaToi = nhomsinhvienService.layNhomCuaSinhVien(username);
            model.addAttribute("nhomCuaToi", nhomCuaToi);
            boolean daCoNhom = (nhomCuaToi != null);
            model.addAttribute("daCoNhom", daCoNhom);

            DangKyDeTaiDto dangKyCuaNhom = null;
            boolean laNhomTruong = false;

            if (daCoNhom) {
                laNhomTruong = (username != null && username.equals(nhomCuaToi.getMasinhvienNhomtruong()));
                dangKyRequest.setNhomid(nhomCuaToi.getId());

                dangKyCuaNhom = service.layDangKyCuaSinhVien(username);

                // Chỉ lấy đề tài công bố thuộc cùng đợt của nhóm
                List<Detai> dsDetaiTheoDot = detaiService.layDetaiCongBoTheoDot(nhomCuaToi.getDotdangkyid());
                model.addAttribute("dsDetaiCongBo", dsDetaiTheoDot);

                List<Nhomsinhvien> dsNhomMotPhan = new ArrayList<>();
                nhomsinhvienService.findById(nhomCuaToi.getId()).ifPresent(dsNhomMotPhan::add);
                model.addAttribute("dsNhom", dsNhomMotPhan);
            } else {
                model.addAttribute("dsDetaiCongBo", new ArrayList<Detai>());
                model.addAttribute("dsNhom", new ArrayList<Nhomsinhvien>());
            }

            model.addAttribute("laNhomTruong", laNhomTruong);
            model.addAttribute("dangKyCuaNhom", dangKyCuaNhom);
            model.addAttribute("items", service.layDanhSachDangKyChoUser(username));
        }

        model.addAttribute("dangKyRequest", dangKyRequest);
        return "dangkydetai-list";
    }

    @PostMapping("/dangky")
    public String dangky(@Valid @ModelAttribute("dangKyRequest") DangKyDeTaiRequest request,
                         BindingResult bindingResult,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null ?
                    bindingResult.getFieldError().getDefaultMessage() : "Dữ liệu đăng ký không hợp lệ";
            redirectAttributes.addFlashAttribute("errorMessage", msg);
            return "redirect:/dangkydetai";
        }

        try {
            String username = authentication != null ? authentication.getName() : null;
            service.dangkyDetai(request, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký đề tài thành công! Chờ xét duyệt.");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dangkydetai";
    }

    @PostMapping("/{id}/huy")
    public String huyDangky(@PathVariable("id") Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.huyDangky(id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đăng ký đề tài thành công!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dangkydetai";
    }

    @PostMapping("/{id}/duyet")
    public String duyetDangky(@PathVariable("id") Long id,
                              @RequestParam("chapnhan") boolean chapnhan,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.duyetDangky(id, chapnhan, username);
            redirectAttributes.addFlashAttribute("successMessage",
                    chapnhan ? "Đã duyệt đề tài cho nhóm thành công!" : "Đã từ chối đăng ký đề tài!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/dangkydetai";
    }
}
