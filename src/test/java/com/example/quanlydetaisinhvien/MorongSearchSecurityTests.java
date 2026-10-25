package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.morong.*;
import com.example.quanlydetaisinhvien.dto.morong.SearchModels.*;
import com.example.quanlydetaisinhvien.security.MorongPermissions.Dataset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MorongSearchSecurityTests extends MorongSearchTestSupport {
    @ParameterizedTest @ValueSource(strings = {"TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN", "UNKNOWN"})
    void serviceAndRepositoryEnforceVerifiedRoleMatrix(String role) {
        auth(role.equals("SINHVIEN") ? "search-student-a" : "search-chief", role);
        if (Set.of("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN").contains(role)) {
            assertThat(service.detai(new DetaiFilterDTO()).items()).hasSize(4);
            assertThat(repository.options(Dataset.DETAI).dotdangky()).hasSize(4);
        } else {
            assertThatThrownBy(() -> service.detai(new DetaiFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.detai(new DetaiFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.options(Dataset.DETAI)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test void anonymousUnauthenticatedAndUnknownRolesCannotCallRepositoryOrService() {
        var anonymous = new AnonymousAuthenticationToken("test", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        for (var auth : Arrays.asList(null, anonymous, UsernamePasswordAuthenticationToken.unauthenticated("search-chief", "unused"))) {
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThatThrownBy(() -> service.detai(new DetaiFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.detai(new DetaiFilterDTO())).isInstanceOf(AccessDeniedException.class);
            for (var dataset : Dataset.values()) assertThatThrownBy(() -> service.options(dataset)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test void searchAndOptionsNeverMutateAnyDatabaseRows() {
        em.flush();
        List<String> tables = List.of("detai", "dotdangky", "dangkydetai", "ketqua", "thongbao", "emailketqua", "taikhoan");
        var before = tables.stream().map(t -> jdbc.queryForList("select * from " + t + " order by Id")).toList();
        service.detai(new DetaiFilterDTO());
        for (var dataset : Dataset.values()) service.options(dataset);
        em.flush();
        var after = tables.stream().map(t -> jdbc.queryForList("select * from " + t + " order by Id")).toList();
        assertThat(after).isEqualTo(before);
    }
}
