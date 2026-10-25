package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.NhomSinhVienDto;
import com.example.quanlydetaisinhvien.dto.TaoNhomRequest;
import com.example.quanlydetaisinhvien.dto.ThemThanhVienRequest;
import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.DotdangkyService;
import com.example.quanlydetaisinhvien.service.NhomsinhvienService;
import com.example.quanlydetaisinhvien.entity.TrangthaiDotdangky;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/nhom")
public class NhomsinhvienController {

    private final NhomsinhvienService service;
    private final DotdangkyService dotdangkyService;

    public NhomsinhvienController(NhomsinhvienService service, DotdangkyService dotdangkyService) {
        this.service = service;
        this.dotdangkyService = dotdangkyService;
    }

    @GetMapping
    public String list(Model model, Authentication authentication,
                       @RequestParam(value = "page", defaultValue = "0") int page) {
        page = Math.max(0, page);
        model.addAttribute("page", page);
        model.addAttribute("title", "Nhóm sinh viên");
        model.addAttribute("taoNhomRequest", new TaoNhomRequest());

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

        if ("TRUONGKHOA".equals(cleanRole)) {
            model.addAttribute("items", service.layDanhSachNhomDto(page));
            model.addAttribute("dotdangkyList", dotdangkyService.layDotMoiNhat(100));
            model.addAttribute("daCoNhom", false);
            model.addAttribute("nhomCuaToi", null);
            model.addAttribute("danhSachLoiMoi", java.util.List.of());
        } else {
            List<NhomSinhVienDto> userGroups = service.layDanhSachNhomChoUser(username);
            model.addAttribute("items", userGroups);
            NhomSinhVienDto nhomCuaToi = service.layNhomCuaSinhVien(username);
            model.addAttribute("nhomCuaToi", nhomCuaToi);
            model.addAttribute("daCoNhom", nhomCuaToi != null);
            model.addAttribute("dotdangkyList", dotdangkyService.layDotDangKyNhom());
            model.addAttribute("danhSachLoiMoi", service.layDanhSachLoiMoiCuaSinhVien(username));
        }

        return "nhom-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id,
                         Model model,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            String role = authentication != null ? authentication.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("") : "";

            if (!role.contains("TRUONGKHOA") && !service.kiemTraSinhVienThuocNhom(id, username)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền truy cập thông tin của nhóm khác!");
                return "redirect:/nhom";
            }

            NhomSinhVienDto nhom = service.layChiTietNhomDto(id);
            model.addAttribute("nhom", nhom);
            model.addAttribute("title", "Chi tiết " + nhom.getTennhom());
            model.addAttribute("themThanhVienRequest", new ThemThanhVienRequest());

            if (authentication != null) {
                model.addAttribute("tendangnhap", username);
                model.addAttribute("vaitro", role.replace("ROLE_", ""));
                boolean laNhomTruong = (username != null && username.equals(nhom.getMasinhvienNhomtruong()))
                        || role.contains("TRUONGKHOA");
                model.addAttribute("laNhomTruong", laNhomTruong);
            }
            return "nhom-detail";
        } catch (NghiepvuException e) {
            model.addAttribute("message", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/{id}/roi-nhom")
    public String roiNhom(@PathVariable("id") Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.roiNhom(id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã rời khỏi nhóm thành công!");
            return "redirect:/nhom";
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/nhom/" + id;
        }
    }

    @PostMapping("/tao")
    public String taoNhom(@Valid @ModelAttribute("taoNhomRequest") TaoNhomRequest request,
                          BindingResult bindingResult,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError() != null ?
                    bindingResult.getFieldError().getDefaultMessage() : "Dữ liệu nhập không hợp lệ";
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/nhom";
        }

        try {
            String username = authentication != null ? authentication.getName() : null;
            Nhomsinhvien nhom = service.taoNhom(request, username);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm '" + nhom.getTennhom() + "' thành công!");
            return "redirect:/nhom/" + nhom.getId();
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/nhom";
        }
    }

    @PostMapping("/{id}/capnhat")
    public String capNhat(@PathVariable("id") Long id,
                          @RequestParam("tennhom") String tennhom,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.capNhatThongTinNhom(id, tennhom, username);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tên nhóm thành công!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhom/" + id;
    }

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable("id") Long id,
                      Authentication authentication,
                      RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.xoaNhom(id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa nhóm thành công!");
            return "redirect:/nhom";
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/nhom/" + id;
        }
    }

    @PostMapping("/{id}/them-thanh-vien")
    public String themThanhVien(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("themThanhVienRequest") ThemThanhVienRequest request,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null ?
                    bindingResult.getFieldError().getDefaultMessage() : "Mã số sinh viên không hợp lệ";
            redirectAttributes.addFlashAttribute("errorMessage", msg);
            return "redirect:/nhom/" + id;
        }

        try {
            String username = authentication != null ? authentication.getName() : null;
            String role = authentication != null ? authentication.getAuthorities().stream().findFirst().map(Object::toString).orElse("") : "";
            service.themThanhVien(id, request.getMasinhvien(), username);
            if (role.contains("TRUONGKHOA")) {
                redirectAttributes.addFlashAttribute("successMessage", "Thêm sinh viên " + request.getMasinhvien() + " vào nhóm thành công!");
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Đã gửi lời mời tham gia nhóm đến sinh viên " + request.getMasinhvien() + ". Sinh viên cần xác nhận đồng ý để chính thức tham gia nhóm.");
            }
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhom/" + id;
    }

    @PostMapping("/{id}/xoa-thanh-vien")
    public String xoaThanhVien(@PathVariable("id") Long id,
                               @RequestParam("sinhvienid") Long sinhvienid,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.xoaThanhVien(id, sinhvienid, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sinh viên khỏi nhóm!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhom/" + id;
    }

    @PostMapping("/{id}/doi-nhom-truong")
    public String doiNhomTruong(@PathVariable("id") Long id,
                                @RequestParam("sinhvienid") Long sinhvienid,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.doiNhomTruong(id, sinhvienid, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật nhóm trưởng mới thành công!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhom/" + id;
    }

    @PostMapping("/loi-moi/{id}/dong-y")
    public String dongYLoiMoi(@PathVariable("id") Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.dongYLoiMoi(id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã đồng ý tham gia nhóm thành công!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhom";
    }

    @PostMapping("/loi-moi/{id}/tu-choi")
    public String tuChoiLoiMoi(@PathVariable("id") Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.tuChoiLoiMoi(id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã từ chối lời mời tham gia nhóm.");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhom";
    }

    @PostMapping("/{nhomid}/huy-loi-moi")
    public String huyLoiMoi(@PathVariable("nhomid") Long nhomid,
                            @RequestParam("loiMoiId") Long loiMoiId,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.huyLoiMoi(nhomid, loiMoiId, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy lời mời tham gia nhóm!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nhom/" + nhomid;
    }
}