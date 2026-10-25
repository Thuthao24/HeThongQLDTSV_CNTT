package com.example.quanlydetaisinhvien.controller.morong;

import com.example.quanlydetaisinhvien.dto.morong.ThongkeFilterDTO;
import com.example.quanlydetaisinhvien.exception.ThongkeRoundNotFoundException;
import com.example.quanlydetaisinhvien.service.morong.ThongkeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/morong/thongke")
public class ThongkeController {
    private final ThongkeService service;
    public ThongkeController(ThongkeService service) { this.service = service; }

    @InitBinder("filter")
    void allowedFields(WebDataBinder binder) { binder.setAllowedFields("dotdangkyid"); }

    @GetMapping
    public String statistics(@Valid @ModelAttribute("filter") ThongkeFilterDTO filter,
            BindingResult errors, Model model, HttpServletRequest request) {
        var values = request.getParameterValues("dotdangkyid");
        if (errors.hasErrors() || (values != null && values.length != 1)) {
            throw new IllegalArgumentException("Bộ lọc thống kê không hợp lệ.");
        }
        var report = service.statistics(filter);
        model.addAttribute("title", "Thống kê tổng quan hệ thống");
        model.addAttribute("summary", report.summary());
        model.addAttribute("byRound", report.byRound());
        model.addAttribute("detaiByStatus", report.detaiByStatus());
        model.addAttribute("dangkyByStatus", report.dangkyByStatus());
        model.addAttribute("ketquaByStatus", report.ketquaByStatus());
        model.addAttribute("filterOptions", report.filterOptions());
        model.addAttribute("exportOptions", com.example.quanlydetaisinhvien.service.morong.ExportLinks.statistics(filter));
        return "morong/thongke";
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalid(Model model) {
        model.addAttribute("message", "Bộ lọc thống kê không hợp lệ. ID đợt phải là số nguyên dương.");
        return "error";
    }

    @ExceptionHandler(ThongkeRoundNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(Model model) {
        model.addAttribute("message", "Không tìm thấy đợt đăng ký.");
        return "error";
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String unavailable(Model model) {
        model.addAttribute("message", "Chưa thể truy cập dữ liệu thống kê. Vui lòng thử lại.");
        return "error";
    }
}
