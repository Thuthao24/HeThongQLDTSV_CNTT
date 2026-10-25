package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.DiemFormDto;
import com.example.quanlydetaisinhvien.dto.GiangvienOptionDto;
import com.example.quanlydetaisinhvien.entity.Dangkydetai;
import com.example.quanlydetaisinhvien.entity.Detai;
import com.example.quanlydetaisinhvien.entity.Diem;
import com.example.quanlydetaisinhvien.entity.Giangvien;
import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;
import com.example.quanlydetaisinhvien.entity.Taikhoan;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.DangkydetaiRepository;
import com.example.quanlydetaisinhvien.repository.DetaiRepository;
import com.example.quanlydetaisinhvien.repository.GiangvienRepository;
import com.example.quanlydetaisinhvien.repository.NhomsinhvienRepository;
import com.example.quanlydetaisinhvien.repository.TaikhoanRepository;
import com.example.quanlydetaisinhvien.service.DiemService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/diem")
public class DiemController {

    private final DiemService service;
    private final DangkydetaiRepository dangkydetaiRepository;
    private final DetaiRepository detaiRepository;
    private final NhomsinhvienRepository nhomsinhvienRepository;
    private final GiangvienRepository giangvienRepository;
    private final TaikhoanRepository taikhoanRepository;

    public DiemController(
            DiemService service,
            DangkydetaiRepository dangkydetaiRepository,
            DetaiRepository detaiRepository,
            NhomsinhvienRepository nhomsinhvienRepository,
            GiangvienRepository giangvienRepository,
            TaikhoanRepository taikhoanRepository) {
        this.service = service;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.detaiRepository = detaiRepository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
        this.giangvienRepository = giangvienRepository;
        this.taikhoanRepository = taikhoanRepository;
    }

    @PostMapping("/tao")
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON','GIANGVIEN')")
    public String create(@ModelAttribute Diem score, RedirectAttributes flash) {
        try {
            service.save(score);
            flash.addFlashAttribute("successMessage", "Đã nhập điểm thành phần.");
        } catch (NghiepvuException | org.springframework.security.access.AccessDeniedException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/diem";
    }

    @GetMapping({"/them", "/nhap"})
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON','GIANGVIEN')")
    public String themForm(Model model) {
        model.addAttribute("title", "Nhập điểm thành phần");
        DiemFormDto form = new DiemFormDto();
        try {
            form.setGiangvienid(service.currentLecturerId());
        } catch (Exception ignored) {
        }
        model.addAttribute("form", form);
        populateDropdowns(model);
        return "diem-form";
    }

    @PostMapping({"/them", "/nhap"})
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON','GIANGVIEN')")
    public String themSubmit(@ModelAttribute("form") DiemFormDto form,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        try {
            Long curId = null;
            try {
                curId = service.currentLecturerId();
            } catch (Exception ignored) {
            }
            if (curId != null) {
                if (form.getGiangvienid() != null && !form.getGiangvienid().equals(curId)) {
                    model.addAttribute("errorMessage", "Giảng viên chỉ được nhập điểm cho chính mình, không được chấm thay giảng viên khác.");
                    model.addAttribute("title", "Nhập điểm thành phần");
                    populateDropdowns(model);
                    return "diem-form";
                }
                form.setGiangvienid(curId);
            }
            service.nhapDiem(form);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu điểm thành phần thành công!");
            return "redirect:/diem";
        } catch (NghiepvuException | org.springframework.security.access.AccessDeniedException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("title", "Nhập điểm thành phần");
            populateDropdowns(model);
            return "diem-form";
        }
    }

    @GetMapping("/sua/{id}")
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON','GIANGVIEN')")
    public String suaForm(@PathVariable Long id, Model model) {
        try {
            DiemFormDto form = service.getFormDtoById(id);
            model.addAttribute("title", "Chỉnh sửa điểm thành phần");
            model.addAttribute("form", form);
            populateDropdowns(model);
            return "diem-form";
        } catch (NghiepvuException | org.springframework.security.access.AccessDeniedException ex) {
            return "redirect:/diem";
        }
    }

    @PostMapping("/sua/{id}")
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON','GIANGVIEN')")
    public String suaSubmit(@PathVariable Long id,
                            @ModelAttribute("form") DiemFormDto form,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        form.setId(id);
        try {
            Long curId = null;
            try {
                curId = service.currentLecturerId();
            } catch (Exception ignored) {
            }
            if (curId != null) {
                if (form.getGiangvienid() != null && !form.getGiangvienid().equals(curId)) {
                    model.addAttribute("errorMessage", "Giảng viên chỉ được nhập điểm cho chính mình, không được chấm thay giảng viên khác.");
                    model.addAttribute("title", "Chỉnh sửa điểm thành phần");
                    populateDropdowns(model);
                    return "diem-form";
                }
                form.setGiangvienid(curId);
            }
            service.nhapDiem(form);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật điểm thành công!");
            return "redirect:/diem";
        } catch (NghiepvuException | org.springframework.security.access.AccessDeniedException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("title", "Chỉnh sửa điểm thành phần");
            populateDropdowns(model);
            return "diem-form";
        }
    }

    @PostMapping("/xoa/{id}")
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON')")
    public String xoaSubmit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.xoaDiem(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa điểm thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa điểm: " + ex.getMessage());
        }
        return "redirect:/diem";
    }

    @PostMapping("/tinh-ket-qua/{dangkydetaiid}")
    @PreAuthorize("hasAnyRole('TRUONGKHOA','TRUONGBOMON')")
    public String tinhKetQuaSubmit(@PathVariable Long dangkydetaiid, RedirectAttributes redirectAttributes) {
        try {
            service.tinhKetQuaTrungGian(dangkydetaiid);
            redirectAttributes.addFlashAttribute("successMessage", "Tính trung bình cộng và lưu kết quả trung gian thành công!");
            return "redirect:/ketqua";
        } catch (NghiepvuException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/diem";
        }
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
    public String list(Model model, Authentication authentication, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("canManage", authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority())));
        model.addAttribute("title", "Chấm điểm");
        try {
            Long lecturerId = service.currentLecturerId();
            model.addAttribute("lecturerId", lecturerId);

            var gradeOptions = service.availableGradeOptions();
            model.addAttribute("gradeRounds", gradeOptions.rounds());
            model.addAttribute("gradeTopics", gradeOptions.topics());
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            model.addAttribute("lecturerId", null);
            model.addAttribute("gradeRounds", List.of());
            model.addAttribute("gradeTopics", List.of());
        }
        var result = service.findPage(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 25, org.springframework.data.domain.Sort.by("id").descending()));
        model.addAttribute("items", result.getContent());
        model.addAttribute("pageData", result);

        Map<Long, Dangkydetai> dkMap = dangkydetaiRepository.findAll().stream()
                .collect(Collectors.toMap(Dangkydetai::getId, dk -> dk, (a, b) -> a));
        Map<Long, Detai> dtMap = detaiRepository.findAll().stream()
                .collect(Collectors.toMap(Detai::getId, dt -> dt, (a, b) -> a));
        Map<Long, Nhomsinhvien> nhomMap = nhomsinhvienRepository.findAll().stream()
                .collect(Collectors.toMap(Nhomsinhvien::getId, nh -> nh, (a, b) -> a));
        Map<Long, Giangvien> gvMap = giangvienRepository.findAll().stream()
                .collect(Collectors.toMap(Giangvien::getId, g -> g, (a, b) -> a));
        Map<Long, Taikhoan> tkMap = taikhoanRepository.findAll().stream()
                .collect(Collectors.toMap(Taikhoan::getId, tk -> tk, (a, b) -> a));

        Map<Long, String> topicNames = new HashMap<>();
        Map<Long, String> groupNames = new HashMap<>();
        for (var entry : dkMap.entrySet()) {
            Detai dt = dtMap.get(entry.getValue().getDetaiid());
            if (dt != null) topicNames.put(entry.getKey(), dt.getTendetai());
            Nhomsinhvien nh = nhomMap.get(entry.getValue().getNhomid());
            if (nh != null) groupNames.put(entry.getKey(), nh.getTennhom());
        }
        Map<Long, String> lecturerNames = new HashMap<>();
        for (var gv : gvMap.values()) {
            Taikhoan tk = tkMap.get(gv.getTaikhoanid());
            lecturerNames.put(gv.getId(), (tk != null ? tk.getHoten() : gv.getMagiangvien()) + " (" + gv.getMagiangvien() + ")");
        }
        model.addAttribute("topicNames", topicNames);
        model.addAttribute("groupNames", groupNames);
        model.addAttribute("lecturerNames", lecturerNames);

        return "diem-list";
    }

    private void populateDropdowns(Model model) {
        Long currentGvId = null;
        try {
            currentGvId = service.currentLecturerId();
        } catch (Exception ignored) {
        }

        if (currentGvId != null) {
            var gradeOptions = service.availableGradeOptions();
            List<Map<String, Object>> dkOptions = new ArrayList<>();
            for (var opt : gradeOptions.topics()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", opt.registrationId());
                map.put("label", "[ĐK #" + opt.registrationId() + "] " + opt.topicName() + " (" + opt.groupName() + ")");
                dkOptions.add(map);
            }
            model.addAttribute("dangkydetais", dkOptions);

            Giangvien gv = giangvienRepository.findById(currentGvId).orElse(null);
            if (gv != null) {
                Taikhoan tk = taikhoanRepository.findById(gv.getTaikhoanid()).orElse(null);
                model.addAttribute("currentLecturerId", gv.getId());
                model.addAttribute("currentLecturerCode", gv.getMagiangvien());
                model.addAttribute("currentLecturerName", tk != null ? tk.getHoten() : gv.getMagiangvien());
            }
        } else {
            model.addAttribute("dangkydetais", List.of());
            model.addAttribute("currentLecturerId", null);
            model.addAttribute("currentLecturerCode", "");
            model.addAttribute("currentLecturerName", "Chưa xác định");
        }
    }
}
