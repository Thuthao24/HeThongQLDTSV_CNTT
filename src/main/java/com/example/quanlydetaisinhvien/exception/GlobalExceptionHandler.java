package com.example.quanlydetaisinhvien.exception;

import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String methodNotAllowed(Model model) {
        model.addAttribute("message", "Thao tác này phải được gửi bằng POST từ biểu mẫu hợp lệ.");
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex) {
        return "403";
    }


    @ExceptionHandler(NghiepvuException.class)
    public String handleNghiepvu(
            NghiepvuException ex, Model model) {

        // Ghi nhận lỗi nghiệp vụ để hỗ trợ kiểm tra
        org.slf4j.LoggerFactory
                .getLogger(GlobalExceptionHandler.class)
                .warn("Lỗi nghiệp vụ: {}", ex.getMessage());

        // Chỉ hiển thị thông báo nghiệp vụ đã được kiểm soát
        model.addAttribute(
             "message",
             "Yêu cầu không thể thực hiện. Vui lòng kiểm tra lại dữ liệu."
        );

        return "error";
    }


    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleOther(Exception ex, Model model) {
        // Chi tiết lỗi chỉ ghi ở log phía máy chủ
        org.slf4j.LoggerFactory
                .getLogger(GlobalExceptionHandler.class)
                .error("Lỗi hệ thống không mong muốn", ex);

        // Không hiển thị ex.getMessage() cho người dùng
        model.addAttribute(
                "message",
                "Hệ thống đang gặp sự cố. Vui lòng thử lại sau."
        );

        return "error";
    }
}