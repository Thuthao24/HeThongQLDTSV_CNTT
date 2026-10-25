package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.KetquaService;
import com.example.quanlydetaisinhvien.entity.HinhthucDanhgia;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.mail.KetquaEmailOutbox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.Map;

@Controller
@RequestMapping("/ketqua")
public class KetquaController {
    private static final Logger log = LoggerFactory.getLogger(KetquaController.class);

    private final KetquaService service;
    private final KetquaEmailOutbox outbox;
    private final boolean emailEnabled;
    private final com.example.quanlydetaisinhvien.service.GiangvienphanbienService giangvienphanbienService;
    private final com.example.quanlydetaisinhvien.service.NghiepvuDetaiService nghiepvuDetaiService;
    private final com.example.quanlydetaisinhvien.service.GiaiDoan1Service choices;
    private final com.example.quanlydetaisinhvien.service.DisplayLookupService lookup;

    public KetquaController(KetquaService service, KetquaEmailOutbox outbox,
            @Value("${app.ketqua.email.enabled:false}") boolean emailEnabled,
            com.example.quanlydetaisinhvien.service.GiangvienphanbienService giangvienphanbienService,
            com.example.quanlydetaisinhvien.service.NghiepvuDetaiService nghiepvuDetaiService,
            com.example.quanlydetaisinhvien.service.GiaiDoan1Service choices,
            com.example.quanlydetaisinhvien.service.DisplayLookupService lookup) {
        this.service = service;
        this.outbox = outbox;
        this.emailEnabled = emailEnabled;
        this.giangvienphanbienService = giangvienphanbienService;
        this.nghiepvuDetaiService = nghiepvuDetaiService;
        this.choices = choices;
        this.lookup = lookup;
    }

    @GetMapping
    public String list(
            Model model,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("title", "Kết quả");
        var pageData = service.findVisiblePage(
                org.springframework.data.domain.PageRequest.of(
                        Math.max(0, page), 20
                )
        );

        var items = pageData.getContent();

        model.addAttribute("pageData", pageData);
        boolean canManage = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_TRUONGKHOA".equals(authority.getAuthority()));
        model.addAttribute("items", items);
        model.addAttribute("canManage", canManage);
        if (canManage) {
            model.addAttribute("publicationInfo", service.publicationInfo(items));
            var emailStatuses = outbox.statuses(items.stream().map(item -> item.getId()).toList());
            model.addAttribute("emailStatuses", emailStatuses);
            model.addAttribute("recipientLabels", outbox.recipientLabels(emailStatuses.values().stream().flatMap(Collection::stream).toList()));
            model.addAttribute("emailStatusLabels", Map.of("PENDING", "Chờ gửi", "SENDING", "Đang gửi",
                    "SENT", "Đã gửi", "MISSING_EMAIL", "Thiếu hoặc sai email", "FAILED", "Chưa gửi được",
                    "UNCERTAIN", "Cần đối soát trước khi gửi lại"));
            model.addAttribute("emailEnabled", emailEnabled);

            java.util.Map<Long, java.util.List<com.example.quanlydetaisinhvien.entity.Giangvienphanbien>> gvpbMap = new java.util.HashMap<>();
            for (com.example.quanlydetaisinhvien.entity.Ketqua kq : items) {
                gvpbMap.put(kq.getId(), giangvienphanbienService.findByDangkydetaiid(kq.getDangkydetaiid()));
            }
            model.addAttribute("gvpbMap", gvpbMap);

            var teachers = choices.dsGiangVienChoices();
            model.addAttribute("teacherOptions", teachers);
            model.addAttribute("teacherLabels", lookup.accountLabels(
                    teachers.stream().map(com.example.quanlydetaisinhvien.entity.Giangvien::getTaikhoanid).toList()));
            model.addAttribute("lecturerNames", lookup.lecturerNames(
                    teachers.stream().map(com.example.quanlydetaisinhvien.entity.Giangvien::getId).toList()));
        }
        return "ketqua-list";
    }

    @PostMapping("/{id}/congbo")
    public String publish(@PathVariable Long id, RedirectAttributes attributes) {
        return perform(attributes, () -> service.congboKetqua(id),
                "Công bố kết quả thành công. Trạng thái email được ghi nhận riêng.");
    }

    @PostMapping("/{id}/phancong")
    public String assign(@PathVariable Long id, @RequestParam HinhthucDanhgia hinhthuc,
            @RequestParam(required = false) Long hoidongid, RedirectAttributes attributes) {
        return perform(attributes, () -> service.phanCongHoiDong(id, hinhthuc, hoidongid),
                "Đã lưu xác nhận hình thức đánh giá và phân công hội đồng.");
    }

    @PostMapping("/{id}/phanbien")
    public String assignReviewer(@PathVariable Long id, @RequestParam Long giangvienid, RedirectAttributes attributes) {
        return perform(attributes, () -> {
            com.example.quanlydetaisinhvien.entity.Ketqua kq = service.findById(id)
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy kết quả."));
            giangvienphanbienService.phanCong(kq.getDangkydetaiid(), giangvienid,
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        }, "Đã phân công giảng viên phản biện thành công.");
    }

    @PostMapping("/{id}/phanbien/xoa")
    public String removeReviewer(@PathVariable Long id, @RequestParam Long giangvienid, RedirectAttributes attributes) {
        return perform(attributes, () -> {
            com.example.quanlydetaisinhvien.entity.Ketqua kq = service.findById(id)
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy kết quả."));
            giangvienphanbienService.xoaPhanCong(kq.getDangkydetaiid(), giangvienid,
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName());
        }, "Đã xóa phân công giảng viên phản biện.");
    }

    @PostMapping("/{id}/tonghop")
    public String tongHopKetqua(@PathVariable Long id, RedirectAttributes attributes) {
        return perform(attributes, () -> {
            com.example.quanlydetaisinhvien.entity.Ketqua kq = service.findById(id)
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy kết quả."));
            nghiepvuDetaiService.tinhVaLuuKetqua(kq.getDangkydetaiid());
        }, "Đã tổng hợp điểm trung bình cộng cho kết quả thành công.");
    }

    @PostMapping("/email/{id}/thulai")
    public String retry(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean xacnhanChuaGui,
            @RequestParam(required = false) Integer solanthu, RedirectAttributes attributes) {
        return perform(attributes, () -> outbox.retry(id, xacnhanChuaGui, solanthu),
                "Đã đưa email vào hàng đợi xử lý lại.");
    }

    private String perform(RedirectAttributes attributes, Runnable action, String success) {
        try {
            action.run();
            attributes.addFlashAttribute("successMessage", success);
        } catch (NghiepvuException ex) {
            attributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataAccessException ex) {
            log.warn("Thao tác kết quả chưa hoàn tất do lỗi xử lý dữ liệu.");
            attributes.addFlashAttribute("errorMessage", "Chưa thể hoàn tất thao tác. Vui lòng tải lại danh sách và thử lại.");
        }
        return "redirect:/ketqua";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidInput(Model model) {
        model.addAttribute("message", "ID, hình thức đánh giá hoặc tham số yêu cầu không hợp lệ.");
        return "error";
    }
}
