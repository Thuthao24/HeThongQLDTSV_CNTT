package com.example.quanlydetaisinhvien.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Set;

/** Read permission for the integrated topic search and its exports. */
public final class MorongPermissions {
    private MorongPermissions() {}
    public enum Dataset { DETAI }

    public static void require(Dataset dataset) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) throw denied();
        Set<String> allowed = switch (dataset) {
            case DETAI -> Set.of("ROLE_TRUONGKHOA", "ROLE_TRUONGBOMON", "ROLE_GIANGVIEN");
        };
        if (auth.getAuthorities().stream().noneMatch(a -> allowed.contains(a.getAuthority()))) throw denied();
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Bạn không có quyền truy cập phạm vi tìm kiếm này.");
    }
}
