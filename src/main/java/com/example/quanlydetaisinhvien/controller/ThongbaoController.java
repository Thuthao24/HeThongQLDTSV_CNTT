package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.exception.ThongbaoNotFoundException;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/thongbao")
public class ThongbaoController {

    private final ThongbaoService service;

    public ThongbaoController(ThongbaoService service) {
        this.service = service;
    }

    @InitBinder("thongbaoForm")
    void bindEditableFields(WebDataBinder binder) {
        binder.setAllowedFields("tieude", "noidung");
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("title", "Thông báo");
        var pageData = service.searchPage(
                keyword,
                org.springframework.data.domain.PageRequest.of(
                        Math.max(0, page), 20));

        model.addAttribute("items", pageData.getContent());
        model.addAttribute("pageData", pageData);
        model.addAttribute("keyword", keyword.strip());
        model.addAttribute("canManage", service.canManage());
        return "thongbao-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        service.requireManageAccess();
        model.addAttribute("thongbaoForm", new ThongbaoFormDTO());
        return form(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("thongbaoForm") ThongbaoFormDTO input,
            BindingResult errors, Model model, RedirectAttributes flash) {
        service.requireManageAccess();
        if (errors.hasErrors()) return form(model, null);
        try {
            var saved = service.create(input);
            flash.addFlashAttribute("successMessage", "Đã tạo thông báo.");
            return "redirect:/thongbao/" + saved.id();
        } catch (DataAccessException ex) {
            saveError(errors, model);
            return form(model, null);
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("title", "Chi tiết thông báo");
        model.addAttribute("item", service.findById(id));
        model.addAttribute("canManage", service.canManage());
        return "thongbao-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        service.requireManageAccess();
        var existing = service.findById(id);
        var input = new ThongbaoFormDTO();
        input.setTieude(existing.tieude());
        input.setNoidung(existing.noidung());
        model.addAttribute("thongbaoForm", input);
        return form(model, id);
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("thongbaoForm") ThongbaoFormDTO input,
            BindingResult errors, Model model, RedirectAttributes flash) {
        service.requireManageAccess();
        if (errors.hasErrors()) {
            service.findById(id); // Missing/invalid IDs still produce 404/400 on an invalid form.
            return form(model, id);
        }
        try {
            var saved = service.update(id, input);
            flash.addFlashAttribute("successMessage", "Đã cập nhật thông báo.");
            return "redirect:/thongbao/" + saved.id();
        } catch (DataAccessException ex) {
            saveError(errors, model);
            return form(model, id);
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            service.deleteById(id);
            flash.addFlashAttribute("successMessage", "Đã xóa thông báo.");
            return "redirect:/thongbao";
        } catch (DataAccessException ex) {
            flash.addFlashAttribute("errorMessage", "Chưa thể xóa thông báo. Vui lòng thử lại.");
            return "redirect:/thongbao/" + id;
        }
    }

    private String form(Model model, Long id) {
        model.addAttribute("title", id == null ? "Thêm thông báo" : "Sửa thông báo");
        model.addAttribute("formMode", id == null ? "CREATE" : "EDIT");
        model.addAttribute("formAction", id == null ? "/thongbao" : "/thongbao/" + id + "/edit");
        if (id != null) model.addAttribute("thongbaoId", id);
        return "thongbao-form";
    }

    private void saveError(BindingResult errors, Model model) {
        String message = "Chưa thể lưu thông báo. Vui lòng thử lại.";
        errors.reject("thongbao.save", message);
        model.addAttribute("errorMessage", message);
    }

    @ExceptionHandler(ThongbaoNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("message", "Không tìm thấy thông báo.");
        return "error";
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidRequest(Model model) {
        model.addAttribute("message", "ID hoặc dữ liệu thông báo không hợp lệ.");
        return "error";
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String dataUnavailable(Model model) {
        model.addAttribute("message", "Chưa thể truy cập dữ liệu thông báo. Vui lòng thử lại.");
        return "error";
    }
}
