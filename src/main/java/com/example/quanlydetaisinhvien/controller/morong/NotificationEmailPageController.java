package com.example.quanlydetaisinhvien.controller.morong;

import com.example.quanlydetaisinhvien.security.NotificationEmailPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Renders the portal shell; queue data remains on the separate JSON monitor API. */
@Controller
public class NotificationEmailPageController {
    private final NotificationEmailPermissions permissions;

    public NotificationEmailPageController(NotificationEmailPermissions permissions) {
        this.permissions = permissions;
    }

    @GetMapping("/morong/email-thongbao/quanly")
    public String management(Model model) {
        permissions.requireChief();
        model.addAttribute("title", "Giám sát Email thông báo");
        return "morong/email-thongbao";
    }
}
