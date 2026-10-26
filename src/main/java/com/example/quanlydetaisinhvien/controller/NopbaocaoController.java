package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.DangKyDeTaiDto;
import com.example.quanlydetaisinhvien.dto.NopBaoCaoDto;
import com.example.quanlydetaisinhvien.dto.NopBaoCaoRequest;
import com.example.quanlydetaisinhvien.entity.Nopbaocao;
import com.example.quanlydetaisinhvien.entity.TrangthaiDangkydetai;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.DangkydetaiService;
import com.example.quanlydetaisinhvien.service.NopbaocaoService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@Controller
@RequestMapping("/nopbaocao")
public class NopbaocaoController {

    private final NopbaocaoService service;
    private final DangkydetaiService dangkydetaiService;

    @Value("${app.upload.dir:uploads/baocao}")
    private String uploadDirectory;

    public NopbaocaoController(NopbaocaoService service, DangkydetaiService dangkydetaiService) {
        this.service = service;
        this.dangkydetaiService = dangkydetaiService;
    }

    @GetMapping
    public String list(Model model, Authentication authentication,
                       @RequestParam(value = "page", defaultValue = "0") int page) {
        page = Math.max(0, page);
        model.addAttribute("page", page);
        model.addAttribute("title", "Nộp báo cáo");

        String username = authentication != null ? authentication.getName() : null;
        String role = authentication != null ? authentication.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("") : "";
        String cleanRole = role.replace("ROLE_", "");
        model.addAttribute("tendangnhap", username);
        model.addAttribute("vaitro", cleanRole);

        List<NopBaoCaoDto> items;
        List<DangKyDeTaiDto> dsDangKy;
        boolean laNhomTruong = false;
        boolean deTaiDaDuyet = false;
        DangKyDeTaiDto dangKyCuaNhom = null;
        NopBaoCaoDto baoCaoDaNop = null;

        if ("SINHVIEN".equals(cleanRole)) {
            items = service.layDanhSachBaoCaoChoUser(username);
            dsDangKy = dangkydetaiService.layDanhSachDangKyChoUser(username);
            if (!dsDangKy.isEmpty()) {
                // Ưu tiên chọn theo ngữ cảnh người dùng:
                // 1. Nhóm mà sinh viên là NHÓM TRƯỞNG và đề tài ĐÃ DUYỆT
                // 2. Nhóm mà sinh viên là NHÓM TRƯỞNG và đề tài CHỜ DUYỆT
                // 3. Đăng ký ĐÃ DUYỆT gần nhất
                // 4. Bản ghi mới nhất
                dangKyCuaNhom = dsDangKy.stream()
                        .filter(dk -> username != null && username.equals(dk.getMasinhvienNhomtruong()) && dk.getTrangthai() == TrangthaiDangkydetai.DADUYET)
                        .findFirst()
                        .orElseGet(() -> dsDangKy.stream()
                                .filter(dk -> username != null && username.equals(dk.getMasinhvienNhomtruong()) && dk.getTrangthai() == TrangthaiDangkydetai.CHODUYET)
                                .findFirst()
                                .orElseGet(() -> dsDangKy.stream()
                                        .filter(dk -> dk.getTrangthai() == TrangthaiDangkydetai.DADUYET)
                                        .reduce((first, second) -> second)
                                        .orElse(dsDangKy.get(dsDangKy.size() - 1))));

                if (username != null && username.equals(dangKyCuaNhom.getMasinhvienNhomtruong())) {
                    laNhomTruong = true;
                }
                if (dangKyCuaNhom.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                    deTaiDaDuyet = true;
                }
            }
        } else {
            items = service.layDanhSachBaoCaoDto(page);
            dsDangKy = dangkydetaiService.layDanhSachDangKyDto();
            laNhomTruong = true;
            deTaiDaDuyet = true;
            if (!dsDangKy.isEmpty()) {
                dangKyCuaNhom = dsDangKy.get(0);
            }
        }

        boolean daKhoaNopBaoCao = false;
        String loaidotHienThi = null;
        if (dangKyCuaNhom != null) {
            Long dkId = dangKyCuaNhom.getId();
            daKhoaNopBaoCao = service.kiemTraDaKhoaNopBaoCao(dkId);
            if (dangKyCuaNhom.getTendot() != null) {
                loaidotHienThi = dangKyCuaNhom.getTendot().toUpperCase();
            }
            for (NopBaoCaoDto bc : items) {
                if (dkId.equals(bc.getDangkydetaiid())) {
                    baoCaoDaNop = bc;
                    break;
                }
            }
        }

        model.addAttribute("items", items);
        model.addAttribute("dsDangKy", dsDangKy);
        model.addAttribute("nopRequest", new NopBaoCaoRequest());
        model.addAttribute("laNhomTruong", laNhomTruong);
        model.addAttribute("deTaiDaDuyet", deTaiDaDuyet);
        model.addAttribute("dangKyCuaNhom", dangKyCuaNhom);
        model.addAttribute("baoCaoDaNop", baoCaoDaNop);
        model.addAttribute("daKhoaNopBaoCao", daKhoaNopBaoCao);
        model.addAttribute("loaidotHienThi", loaidotHienThi);
        return "nopbaocao-list";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadBaocao(@PathVariable("id") Long id,
                                                    Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = authentication.getName();
        if (!service.kiemTraQuyenTruyCapBaoCao(id, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Nopbaocao nb = service.findById(id).orElse(null);
        if (nb == null || nb.getDuongdanfile() == null || nb.getDuongdanfile().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        String pathStr = nb.getDuongdanfile().trim();
        if (pathStr.startsWith("http://") || pathStr.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(pathStr)).build();
        }

        String decoded;
        try {
            decoded = java.net.URLDecoder.decode(pathStr, java.nio.charset.StandardCharsets.UTF_8).replace('\\', '/');
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Only serve application-managed uploads. Never trust a DB-stored path as an
        // arbitrary filesystem location (including ../ or an absolute path).
        String prefix = "/uploads/baocao/";
        if (!decoded.startsWith(prefix) && !decoded.startsWith("uploads/baocao/")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String fileName = decoded.startsWith(prefix) ? decoded.substring(prefix.length()) : decoded.substring("uploads/baocao/".length());
        while (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }
        if (fileName.isBlank() || fileName.equals(".") || fileName.equals("..")
                || fileName.contains("../") || fileName.contains("..\\")
                || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Path uploadRoot = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        Path filePath = uploadRoot.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadRoot) || !Files.isRegularFile(filePath)
                || !Files.isReadable(filePath) || Files.isSymbolicLink(filePath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/nop")
    public String nopBaocao(@Valid @ModelAttribute("nopRequest") NopBaoCaoRequest request,
                            BindingResult bindingResult,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            String msg = bindingResult.getFieldError() != null ?
                    bindingResult.getFieldError().getDefaultMessage() : "Dữ liệu nộp báo cáo không hợp lệ";
            redirectAttributes.addFlashAttribute("errorMessage", msg);
            return "redirect:/nopbaocao";
        }

        try {
            String username = authentication != null ? authentication.getName() : null;
            service.nopBaocao(request, username);
            redirectAttributes.addFlashAttribute("successMessage", "Nộp báo cáo thành công!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nopbaocao";
    }

    @PostMapping("/upload")
    public String uploadBaocao(@RequestParam("dangkydetaiid") Long dangkydetaiid,
                               @RequestParam("file") MultipartFile file,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.nopBaocaoVoiUpload(dangkydetaiid, file, username);
            redirectAttributes.addFlashAttribute("successMessage", "Tải lên báo cáo thành công!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nopbaocao";
    }

    @PostMapping("/{id}/xoa")
    public String xoaBaocao(@PathVariable("id") Long id,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = authentication != null ? authentication.getName() : null;
            service.xoaBaocao(id, username);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bản ghi nộp báo cáo!");
        } catch (NghiepvuException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nopbaocao";
    }
}
