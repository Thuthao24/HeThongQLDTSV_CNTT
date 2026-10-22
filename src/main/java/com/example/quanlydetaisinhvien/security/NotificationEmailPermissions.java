package com.example.quanlydetaisinhvien.security;

import com.example.quanlydetaisinhvien.entity.Vaitro;
import com.example.quanlydetaisinhvien.repository.TaikhoanRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class NotificationEmailPermissions {
    private final TaikhoanRepository accounts;
    public NotificationEmailPermissions(TaikhoanRepository accounts) { this.accounts = accounts; }
    public void requireChief() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || auth.getAuthorities().stream().noneMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()))
                || accounts.findByTendangnhap(auth.getName()).filter(a -> a.getVaitro() == Vaitro.TRUONGKHOA
                    && Boolean.TRUE.equals(a.getKichhoat())).isEmpty()) {
            throw new AccessDeniedException("Chỉ Trưởng khoa đang hoạt động được quản trị email thông báo.");
        }
    }
}
