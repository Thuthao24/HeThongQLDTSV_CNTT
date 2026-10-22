package com.example.quanlydetaisinhvien.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ThongkePermissions {
    private ThongkePermissions() {}

    public static void requireChief() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || auth.getAuthorities().stream().noneMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Chỉ Trưởng khoa được xem thống kê.");
        }
    }
}
