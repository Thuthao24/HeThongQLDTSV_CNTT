package com.example.quanlydetaisinhvien.controller.morong;

import com.example.quanlydetaisinhvien.dto.NotificationEmailFilterDTO;
import com.example.quanlydetaisinhvien.dto.NotificationEmailModels.Monitor;
import com.example.quanlydetaisinhvien.service.notification.NotificationEmailMonitorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/morong/email-thongbao")
public class NotificationEmailController {
    private final NotificationEmailMonitorService service;
    private static final Set<String> FIELDS=Set.of("thongbaoid","trangthai","page","size");
    public NotificationEmailController(NotificationEmailMonitorService service) { this.service=service; }
    @InitBinder("filter") void fields(WebDataBinder binder) { binder.setAllowedFields(FIELDS.toArray(String[]::new)); }
    @GetMapping
    public ResponseEntity<Monitor> read(@Valid @ModelAttribute("filter") NotificationEmailFilterDTO filter,
            BindingResult errors, HttpServletRequest request) {
        if (errors.hasErrors() || request.getParameterMap().entrySet().stream()
                .anyMatch(e -> !FIELDS.contains(e.getKey()) || e.getValue().length!=1)) throw new IllegalArgumentException();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.read(filter));
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String,String>> invalid() { return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
            .body(Map.of("message","Bộ lọc quản trị email thông báo không hợp lệ.")); }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String,String>> unavailable() { return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .cacheControl(CacheControl.noStore()).body(Map.of("message","Chưa thể đọc hàng đợi email thông báo.")); }
}
