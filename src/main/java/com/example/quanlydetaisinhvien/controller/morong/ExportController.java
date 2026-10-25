package com.example.quanlydetaisinhvien.controller.morong;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.ExportModels.*;
import com.example.quanlydetaisinhvien.exception.*;
import com.example.quanlydetaisinhvien.service.morong.ExportLinks;
import com.example.quanlydetaisinhvien.service.morong.export.ReportExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/morong/xuat")
public class ExportController {
    private final ReportExportService service;
    public ExportController(ReportExportService service) { this.service = service; }

    @InitBinder("filter")
    void fields(WebDataBinder binder) {
        // MVC may initialize the binder before a DTO target exists; per-dataset rejection is in download().
        binder.setAllowedFields(java.util.Arrays.stream(Dataset.values()).flatMap(d -> ExportLinks.fields(d).stream())
                .distinct().toArray(String[]::new));
    }
    @GetMapping({"/detai.xlsx", "/detai.pdf"})
    public ResponseEntity<byte[]> detai(@Valid @ModelAttribute("filter") DetaiFilterDTO filter, BindingResult errors,
            HttpServletRequest request) throws IOException { return download(Dataset.DETAI, filter, errors, request); }
    @GetMapping({"/dotdangky.xlsx", "/dotdangky.pdf"})
    public ResponseEntity<byte[]> dotdangky(@Valid @ModelAttribute("filter") DotdangkyFilterDTO filter, BindingResult errors,
            HttpServletRequest request) throws IOException { return download(Dataset.DOTDANGKY, filter, errors, request); }
    @GetMapping({"/ketqua.xlsx", "/ketqua.pdf"})
    public ResponseEntity<byte[]> ketqua(@Valid @ModelAttribute("filter") KetquaFilterDTO filter, BindingResult errors,
            HttpServletRequest request) throws IOException { return download(Dataset.KETQUA, filter, errors, request); }
    @GetMapping({"/thongke.xlsx", "/thongke.pdf"})
    public ResponseEntity<byte[]> thongke(@Valid @ModelAttribute("filter") ThongkeFilterDTO filter, BindingResult errors,
            HttpServletRequest request) throws IOException { return download(Dataset.THONGKE, filter, errors, request); }

    @GetMapping("/{dataset}.{format}")
    public ResponseEntity<Map<String, String>> unsupported() { return error(HttpStatus.NOT_FOUND, "Dataset hoặc định dạng xuất không hỗ trợ."); }

    private ResponseEntity<byte[]> download(Dataset dataset, Object filter, BindingResult errors, HttpServletRequest request) throws IOException {
        dataset.requireAccess();
        if (errors.hasErrors()) throw new IllegalArgumentException("Bộ lọc xuất không hợp lệ.");
        request.getParameterMap().forEach((name, values) -> {
            if (!ExportLinks.fields(dataset).contains(name) || values.length != 1) {
                throw new IllegalArgumentException("Tham số không hỗ trợ hoặc bị lặp: " + name);
            }
        });
        Format format = request.getRequestURI().endsWith(".pdf") ? Format.PDF : Format.XLSX;
        var download = service.export(dataset, filter, format);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.mime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.filename()).build().toString())
                .cacheControl(CacheControl.noStore()).contentLength(download.bytes().length).body(download.bytes());
    }
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, String>> invalid() { return error(HttpStatus.BAD_REQUEST, "Bộ lọc xuất không hợp lệ."); }
    @ExceptionHandler(ThongkeRoundNotFoundException.class)
    public ResponseEntity<Map<String, String>> missing() { return error(HttpStatus.NOT_FOUND, "Không tìm thấy đợt đăng ký."); }
    @ExceptionHandler(ExportLimitException.class)
    public ResponseEntity<Map<String, String>> limit(ExportLimitException ex) { return error(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage()); }
    @ExceptionHandler({DataAccessException.class, IOException.class})
    public ResponseEntity<Map<String, String>> unavailable() { return error(HttpStatus.SERVICE_UNAVAILABLE, "Chưa thể xuất dữ liệu. Vui lòng thử lại."); }
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message", message));
    }
}
