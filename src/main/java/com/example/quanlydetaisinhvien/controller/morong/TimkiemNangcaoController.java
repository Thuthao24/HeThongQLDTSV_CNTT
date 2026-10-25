package com.example.quanlydetaisinhvien.controller.morong;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.security.MorongPermissions;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import com.example.quanlydetaisinhvien.validation.MorongFilterValidator;
import jakarta.validation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/morong/timkiem")
public class TimkiemNangcaoController {
    private final MorongFilterValidator validation;
    public TimkiemNangcaoController(MorongFilterValidator validation) {
        this.validation = validation;
    }

    @InitBinder("filter")
    void filterFields(WebDataBinder binder) {
        binder.setAllowedFields(com.example.quanlydetaisinhvien.service.morong.ExportLinks
                .fields(ExportModels.Dataset.DETAI).toArray(String[]::new));
    }

    @GetMapping("/detai")
    public org.springframework.web.servlet.view.RedirectView detai(@Valid @ModelAttribute("filter") DetaiFilterDTO filter, BindingResult errors,
            jakarta.servlet.http.HttpServletRequest request) {
        MorongPermissions.require(Dataset.DETAI);
        com.example.quanlydetaisinhvien.service.morong.ExportLinks.validateParameters(ExportModels.Dataset.DETAI, request);
        if (errors.hasErrors()) throw new IllegalArgumentException("Bộ lọc tìm kiếm không hợp lệ.");
        validation.validate(Dataset.DETAI, filter);
        // A fixed local destination; no user-controlled redirect target or flash query injection.
        var redirect = new org.springframework.web.servlet.view.RedirectView(
                com.example.quanlydetaisinhvien.service.morong.ExportLinks.detaiPage(filter, filter.getPage()), false);
        redirect.setExposeModelAttributes(false);
        return redirect;
    }

    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalidRequest(Model model) {
        model.addAttribute("message", "Bộ lọc tìm kiếm không hợp lệ.");
        return "error";
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String unavailable(Model model) {
        model.addAttribute("message", "Chưa thể tìm kiếm dữ liệu. Vui lòng thử lại.");
        return "error";
    }
}
