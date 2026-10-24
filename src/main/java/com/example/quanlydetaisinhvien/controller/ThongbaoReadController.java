package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.service.ThongbaoReadService;
import com.example.quanlydetaisinhvien.exception.ThongbaoNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/thongbao")
public class ThongbaoReadController {
    private final ThongbaoReadService service;
    public ThongbaoReadController(ThongbaoReadService service) { this.service=service; }
    @GetMapping("/chuong") public ResponseEntity<?> bell() { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.bell()); }
    @PostMapping("/{id}/dadoc") public ResponseEntity<?> markRead(@PathVariable Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.markRead(id));
    }
    @ExceptionHandler(ThongbaoNotFoundException.class) public ResponseEntity<?> notFound() {
        return error(404,"Không tìm thấy thông báo.");
    }
    @ExceptionHandler({IllegalArgumentException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid() { return error(400,"ID thông báo không hợp lệ."); }
    @ExceptionHandler(DataAccessException.class) public ResponseEntity<?> unavailable() {
        return error(503,"Chưa thể cập nhật trạng thái đọc. Vui lòng tải lại trang.");
    }
    private ResponseEntity<?> error(int status,String message) { return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message",message)); }
}
