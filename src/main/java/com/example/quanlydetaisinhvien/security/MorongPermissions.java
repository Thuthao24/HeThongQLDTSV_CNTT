package com.example.quanlydetaisinhvien.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Set;

/** Mirrors the verified read permissions; result scope never comes from HTTP parameters. */
public final class MorongPermissions {
    private MorongPermissions() {}
    public enum Dataset { DETAI, DOTDANGKY, KETQUA }
    public record Scope(String username, boolean student) {}

    public static Scope require(Dataset dataset) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) throw denied();
        Set<String> allowed = switch (dataset) {
            case DETAI -> Set.of("ROLE_TRUONGKHOA", "ROLE_TRUONGBOMON", "ROLE_GIANGVIEN");
            case DOTDANGKY -> Set.of("ROLE_TRUONGKHOA");
            case KETQUA -> Set.of("ROLE_TRUONGKHOA", "ROLE_SINHVIEN");
        };
        if (auth.getAuthorities().stream().noneMatch(a -> allowed.contains(a.getAuthority()))) throw denied();
        boolean chief = auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()));
        return new Scope(auth.getName(), dataset == Dataset.KETQUA && !chief);
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Bạn không có quyền truy cập phạm vi tìm kiếm này.");
    }
}
