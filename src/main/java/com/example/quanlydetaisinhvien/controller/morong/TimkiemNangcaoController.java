package com.example.quanlydetaisinhvien.controller.morong;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.SearchModels.*;
import com.example.quanlydetaisinhvien.security.MorongPermissions;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import com.example.quanlydetaisinhvien.service.morong.TimkiemNangcaoService;
import com.example.quanlydetaisinhvien.validation.MorongFilterValidator;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/morong/timkiem")
public class TimkiemNangcaoController {
    private final TimkiemNangcaoService service;
    private final MorongFilterValidator validation;
    public TimkiemNangcaoController(TimkiemNangcaoService service, MorongFilterValidator validation) {
        this.service = service; this.validation = validation;
    }

    @InitBinder("filter")
    void filterFields(WebDataBinder binder) {
        binder.setAllowedFields("keyword", "dotdangkyid", "loaidot", "page", "size", "sort",
                "bomonid", "trangthai", "giangviendexuatid", "giangvienhuongdanid",
                "mocThoiGian", "thoiGianTu", "thoiGianDen", "dacongbo", "diemTu", "diemDen",
                "ngayCongboTu", "ngayCongboDen");
    }

    @GetMapping("/detai")
    public String detai(@Valid @ModelAttribute("filter") DetaiFilterDTO filter, BindingResult errors,
            Model model, HttpServletResponse response) {
        if (invalid(Dataset.DETAI, filter, errors, response))
            return page(Dataset.DETAI, filter, new Result<>(List.of(), new PageInfo(0, 20, 0, 0, false, false)), model, false);
        return page(Dataset.DETAI, filter, service.detai(filter), model, true);
    }

    @GetMapping("/dotdangky")
    public String dotdangky(@Valid @ModelAttribute("filter") DotdangkyFilterDTO filter, BindingResult errors,
            Model model, HttpServletResponse response) {
        if (invalid(Dataset.DOTDANGKY, filter, errors, response))
            return page(Dataset.DOTDANGKY, filter, new Result<>(List.of(), new PageInfo(0, 20, 0, 0, false, false)), model, false);
        return page(Dataset.DOTDANGKY, filter, service.dotdangky(filter), model, true);
    }

    @GetMapping("/ketqua")
    public String ketqua(@Valid @ModelAttribute("filter") KetquaFilterDTO filter, BindingResult errors,
            Model model, HttpServletResponse response) {
        if (invalid(Dataset.KETQUA, filter, errors, response))
            return page(Dataset.KETQUA, filter, new Result<>(List.of(), new PageInfo(0, 20, 0, 0, false, false)), model, false);
        return page(Dataset.KETQUA, filter, service.ketqua(filter), model, true);
    }

    private boolean invalid(Dataset dataset, SearchFilterDTO filter, BindingResult errors, HttpServletResponse response) {
        MorongPermissions.require(dataset);
        if (!errors.hasFieldErrors("sort")) {
            try { validation.ordering(dataset, filter.getSort()); }
            catch (IllegalArgumentException ex) { errors.rejectValue("sort", "sort.invalid", ex.getMessage()); }
        }
        if (errors.hasErrors()) { response.setStatus(HttpStatus.BAD_REQUEST.value()); return true; }
        return false;
    }

    private String page(Dataset dataset, SearchFilterDTO filter, Result<?> result, Model model, boolean allowExport) {
        model.addAttribute("title", switch (dataset) {
            case DETAI -> "Tìm kiếm nâng cao đề tài";
            case DOTDANGKY -> "Tìm kiếm nâng cao đợt đăng ký";
            case KETQUA -> "Tìm kiếm nâng cao kết quả";
        });
        model.addAttribute("filter", filter);
        model.addAttribute("items", result.items());
        model.addAttribute("pageInfo", result.pageInfo());
        model.addAttribute("filterOptions", service.options(dataset));
        model.addAttribute("exportOptions", ExportOptions.unavailable());
        if (allowExport) {
            model.addAttribute("exportOptions", com.example.quanlydetaisinhvien.service.morong.ExportLinks.search(
                    com.example.quanlydetaisinhvien.dto.morong.ExportModels.Dataset.valueOf(dataset.name()), filter));
        }
        return "morong/timkiem-" + dataset.name().toLowerCase(java.util.Locale.ROOT);
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
