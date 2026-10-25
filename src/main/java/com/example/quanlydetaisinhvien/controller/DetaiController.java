package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.DetaiService;
import com.example.quanlydetaisinhvien.service.GiaiDoan1Service;
import com.example.quanlydetaisinhvien.service.DisplayLookupService;
import com.example.quanlydetaisinhvien.entity.Detai;
import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.service.morong.*;
import com.example.quanlydetaisinhvien.security.MorongPermissions;
import com.example.quanlydetaisinhvien.validation.MorongFilterValidator;
import jakarta.servlet.http.*;
import jakarta.validation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/detai")
public class DetaiController {

    private final DetaiService service;
    private final GiaiDoan1Service gd1;

    private final DisplayLookupService lookup;
    private final TimkiemNangcaoService search;
    private final MorongFilterValidator validation;

    public DetaiController(DetaiService service, GiaiDoan1Service gd1,
            TimkiemNangcaoService search, MorongFilterValidator validation, DisplayLookupService lookup) {
        this.service = service;
        this.gd1=gd1;
        this.lookup = lookup;
        this.search = search;
        this.validation = validation;
    }

    @ModelAttribute("filter")
    DetaiFilterDTO defaultFilter() {
        var filter = new DetaiFilterDTO();
        filter.setSize(25);
        return filter;
    }

    @InitBinder("filter")
    void filterFields(WebDataBinder binder) {
        binder.setAllowedFields(ExportLinks.fields(ExportModels.Dataset.DETAI).toArray(String[]::new));
    }

    @GetMapping
    public String list(@Valid @ModelAttribute("filter") DetaiFilterDTO filter, BindingResult errors,
            Model model, Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        MorongPermissions.require(MorongPermissions.Dataset.DETAI);
        ExportLinks.validateParameters(ExportModels.Dataset.DETAI, request);
        if (!errors.hasFieldErrors("sort")) {
            try { validation.ordering(MorongPermissions.Dataset.DETAI, filter.getSort()); }
            catch (IllegalArgumentException ex) { errors.rejectValue("sort", "sort.invalid", ex.getMessage()); }
        }
        boolean valid = !errors.hasErrors();
        if (!valid) response.setStatus(400);
        var result = valid ? service.searchPage(filter)
                : new org.springframework.data.domain.PageImpl<com.example.quanlydetaisinhvien.entity.Detai>(java.util.List.of());
        var items = result.getContent();
        model.addAttribute("title", "Đề tài");
        model.addAttribute("pageData", result);
        model.addAttribute("pageInfo", valid ? SearchModels.PageInfo.of(filter, result.getTotalElements())
                : new SearchModels.PageInfo(0, 25, 0, 0, false, false));
        model.addAttribute("q", filter.getQ());
        model.addAttribute("items", items);
        // 2. Hiển thị TÊN ĐỢT thay vì ID
        model.addAttribute(
                "roundNames",
                lookup.roundNames(
                        items.stream()
                                .map(Detai::getDotdangkyid)
                                .toList()
                )
        );

        // 3. Hiển thị TÊN BỘ MÔN thay vì ID
        model.addAttribute(
                "departmentNames",
                lookup.departmentNames(
                        items.stream()
                                .map(Detai::getBomonid)
                                .toList()
                )
        );

        // 4. Hiển thị HỌ TÊN GIẢNG VIÊN đề xuất
        model.addAttribute(
                "lecturerNames",
                lookup.lecturerNames(
                        items.stream()
                                .map(Detai::getGiangviendexuatid)
                                .toList()
                )
        );


        model.addAttribute("filterOptions", search.options(MorongPermissions.Dataset.DETAI));
        model.addAttribute("exportOptions", valid ? ExportLinks.search(ExportModels.Dataset.DETAI, filter)
                : SearchModels.ExportOptions.unavailable());
        if (valid) {
            model.addAttribute("previousUrl", ExportLinks.detaiPage(filter, Math.max(0, filter.getPage() - 1)));
            model.addAttribute("nextUrl", ExportLinks.detaiPage(filter, filter.getPage() + 1));
        }
        model.addAttribute("canProposeTopic", gd1.canPropose(authentication.getName()));
        model.addAttribute("editableIds", gd1.editableTopicIds(authentication.getName(), items));
        return "detai-list";
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public String invalidRequest(Model model) {
        model.addAttribute("message", "Bộ lọc tìm kiếm không hợp lệ.");
        return "error";
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
    public String unavailable(Model model) {
        model.addAttribute("message", "Chưa thể tìm kiếm dữ liệu. Vui lòng thử lại.");
        return "error";
    }
}
