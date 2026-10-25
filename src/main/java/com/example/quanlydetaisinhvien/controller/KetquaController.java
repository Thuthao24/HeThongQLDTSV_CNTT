package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.entity.HinhthucDanhgia;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.KetquaService;
import com.example.quanlydetaisinhvien.service.mail.KetquaEmailOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

@Controller
@RequestMapping("/ketqua")
public class KetquaController {
    private static final Logger log = LoggerFactory.getLogger(KetquaController.class);

    private final KetquaService service;
    private final KetquaEmailOutbox outbox;
    private final boolean emailEnabled;
    private final com.example.quanlydetaisinhvien.repository.DangkydetaiRepository dangkydetaiRepository;
    private final com.example.quanlydetaisinhvien.repository.DetaiRepository detaiRepository;
    private final com.example.quanlydetaisinhvien.repository.NhomsinhvienRepository nhomsinhvienRepository;
    private final com.example.quanlydetaisinhvien.repository.HoidongRepository hoidongRepository;

    public KetquaController(KetquaService service, KetquaEmailOutbox outbox,
            @Value("${app.ketqua.email.enabled:false}") boolean emailEnabled,
            com.example.quanlydetaisinhvien.repository.DangkydetaiRepository dangkydetaiRepository,
            com.example.quanlydetaisinhvien.repository.DetaiRepository detaiRepository,
            com.example.quanlydetaisinhvien.repository.NhomsinhvienRepository nhomsinhvienRepository,
            com.example.quanlydetaisinhvien.repository.HoidongRepository hoidongRepository) {
        this.service = service;
        this.outbox = outbox;
        this.emailEnabled = emailEnabled;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.detaiRepository = detaiRepository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
        this.hoidongRepository = hoidongRepository;
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

        // Batch lookups scoped to the current page items
        Set<Long> dkIds = items.stream()
                .map(com.example.quanlydetaisinhvien.entity.Ketqua::getDangkydetaiid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<com.example.quanlydetaisinhvien.entity.Dangkydetai> dangKyList = dkIds.isEmpty()
                ? List.of()
                : dangkydetaiRepository.findAllById(dkIds);

        Set<Long> dtIds = dangKyList.stream()
                .map(com.example.quanlydetaisinhvien.entity.Dangkydetai::getDetaiid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, com.example.quanlydetaisinhvien.entity.Detai> dtMap = dtIds.isEmpty()
                ? Map.of()
                : detaiRepository.findAllById(dtIds).stream()
                .collect(Collectors.toMap(com.example.quanlydetaisinhvien.entity.Detai::getId, dt -> dt, (a, b) -> a));

        Set<Long> nhomIds = dangKyList.stream()
                .map(com.example.quanlydetaisinhvien.entity.Dangkydetai::getNhomid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, com.example.quanlydetaisinhvien.entity.Nhomsinhvien> nhomMap = nhomIds.isEmpty()
                ? Map.of()
                : nhomsinhvienRepository.findAllById(nhomIds).stream()
                .collect(Collectors.toMap(com.example.quanlydetaisinhvien.entity.Nhomsinhvien::getId, nh -> nh, (a, b) -> a));

        Map<Long, String> topicNames = new HashMap<>();
        Map<Long, String> groupNames = new HashMap<>();
        for (var dk : dangKyList) {
            if (dk.getDetaiid() != null) {
                var dt = dtMap.get(dk.getDetaiid());
                if (dt != null && dt.getTendetai() != null) {
                    topicNames.put(dk.getId(), dt.getTendetai());
                }
            }
            if (dk.getNhomid() != null) {
                var nh = nhomMap.get(dk.getNhomid());
                if (nh != null && nh.getTennhom() != null) {
                    groupNames.put(dk.getId(), nh.getTennhom());
                }
            }
        }
        model.addAttribute("topicNames", topicNames);
        model.addAttribute("groupNames", groupNames);

        Map<Long, String> councilNames = new HashMap<>();
        if (canManage) {
            Set<Long> dotIds = dtMap.values().stream()
                    .map(com.example.quanlydetaisinhvien.entity.Detai::getDotdangkyid)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (Long dotId : dotIds) {
                for (var hd : hoidongRepository.findByDotdangkyid(dotId)) {
                    if (hd.getId() != null && hd.getTenhoidong() != null) {
                        councilNames.put(hd.getId(), hd.getTenhoidong());
                    }
                }
            }

            var publicationInfo = service.publicationInfo(items);
            model.addAttribute("publicationInfo", publicationInfo);

            for (var info : publicationInfo.values()) {
                if (info.councils() != null) {
                    for (var c : info.councils()) {
                        if (c.getId() != null && c.getTenhoidong() != null) {
                            councilNames.put(c.getId(), c.getTenhoidong());
                        }
                    }
                }
            }

            var emailStatuses = outbox.statuses(items.stream().map(com.example.quanlydetaisinhvien.entity.Ketqua::getId).toList());
            model.addAttribute("emailStatuses", emailStatuses);
            model.addAttribute("recipientLabels", outbox.recipientLabels(emailStatuses.values().stream().flatMap(Collection::stream).toList()));
            model.addAttribute("emailStatusLabels", Map.of("PENDING", "Chờ gửi", "SENDING", "Đang gửi",
                    "SENT", "Đã gửi", "MISSING_EMAIL", "Thiếu hoặc sai email", "FAILED", "Chưa gửi được",
                    "UNCERTAIN", "Cần đối soát trước khi gửi lại"));
            model.addAttribute("emailEnabled", emailEnabled);
            model.addAttribute("doiChieuItems", service.findDoiChieuDto(items));
        }
        model.addAttribute("councilNames", councilNames);
        return "ketqua-list";
    }

    @PostMapping("/{id}/congbo")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String publish(@PathVariable Long id, RedirectAttributes attributes) {
        return perform(attributes, () -> service.congboKetqua(id),
                "Công bố kết quả thành công. Trạng thái email được ghi nhận riêng.");
    }

    @PostMapping("/cong-bo/{id}")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String publishAlias(@PathVariable Long id, RedirectAttributes attributes) {
        return publish(id, attributes);
    }

    @PostMapping("/{id}/phancong")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String assign(@PathVariable Long id, @RequestParam HinhthucDanhgia hinhthuc,
            @RequestParam(required = false) Long hoidongid, RedirectAttributes attributes) {
        return perform(attributes, () -> service.phanCongHoiDong(id, hinhthuc, hoidongid),
                "Đã lưu xác nhận hình thức đánh giá và phân công hội đồng.");
    }

    @PostMapping("/tinh-lai/{dangkydetaiid}")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String tinhLai(@PathVariable Long dangkydetaiid, RedirectAttributes attributes) {
        return perform(attributes, () -> service.tinhLaiKetQua(dangkydetaiid),
                "Đã cập nhật lại kết quả cho đề tài đăng ký #" + dangkydetaiid);
    }

    @PostMapping("/tinh-tat-ca")
    @PreAuthorize("hasRole('TRUONGKHOA')")
    public String tinhTatCa(RedirectAttributes attributes) {
        return perform(attributes, service::tinhLaiTatCaKetQua,
                "Đã tính toán lại toàn bộ kết quả từ các điểm thành phần!");
    }

    @PostMapping("/email/{id}/thulai")
    @PreAuthorize("hasRole('TRUONGKHOA')")
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
