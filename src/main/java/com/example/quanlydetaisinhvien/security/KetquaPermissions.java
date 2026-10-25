package com.example.quanlydetaisinhvien.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public final class KetquaPermissions {
    private KetquaPermissions() {}

    public static void requireTruongKhoa() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getAuthorities().stream()
                    .noneMatch(authority -> "ROLE_TRUONGKHOA".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Chỉ Trưởng khoa được phân công và công bố kết quả.");
        }
    }
}
