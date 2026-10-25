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
        if (role.equals("TRUONGKHOA")) assertThat(service.dotdangky(new DotdangkyFilterDTO()).items()).hasSize(5);
        else {
            assertThatThrownBy(() -> service.dotdangky(new DotdangkyFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.dotdangky(new DotdangkyFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.options(Dataset.DOTDANGKY)).isInstanceOf(AccessDeniedException.class);
        }
        if (role.equals("TRUONGKHOA")) assertThat(service.ketqua(new KetquaFilterDTO()).items()).hasSize(4);
        else if (role.equals("SINHVIEN")) assertThat(service.ketqua(new KetquaFilterDTO()).items()).hasSize(2);
        else {
            assertThatThrownBy(() -> service.ketqua(new KetquaFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.ketqua(new KetquaFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.options(Dataset.KETQUA)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test void studentResultsCountAndPaginationOnlyContainPublishedMembershipRows() {
        auth("search-student-a", "SINHVIEN");
        var f = new KetquaFilterDTO(); f.setSort("id,asc"); f.setSize(1);
        var first = service.ketqua(f);
        assertThat(first.items()).extracting(KetquaRow::id).containsExactly(resultA.getId());
        assertThat(first.pageInfo()).isEqualTo(new PageInfo(0, 1, 2, 2, false, true));
        f.setPage(1);
        assertThat(service.ketqua(f).items()).extracting(KetquaRow::id).containsExactly(resultB.getId());
        f.setPage(2);
        assertThat(service.ketqua(f).items()).isEmpty();
        assertThat(service.ketqua(f).pageInfo().totalElements()).isEqualTo(2);
    }

    @ParameterizedTest @ValueSource(strings = {"own-draft", "other-round", "unpublished", "other-keyword", "hidden-type", "hidden-status"})
    void forgedButValidFilterCannotRevealDraftsOrOtherStudents(String attempt) {
        auth("search-student-a", "SINHVIEN");
        var f = new KetquaFilterDTO();
        switch (attempt) {
            case "own-draft" -> f.setDotdangkyid(roundD.getId());
            case "other-round" -> f.setDotdangkyid(roundC.getId());
            case "unpublished" -> f.setDacongbo(false);
            case "other-keyword" -> f.setKeyword("Nhóm Khác");
            case "hidden-type" -> f.setLoaidot(com.example.quanlydetaisinhvien.entity.Loaidot.NCKH);
            case "hidden-status" -> f.setTrangthai(com.example.quanlydetaisinhvien.entity.TrangthaiDangkydetai.TUCHOI);
        }
        var result = repository.ketqua(f);
        assertThat(result.items()).isEmpty();
        assertThat(result.pageInfo().totalElements()).isZero();
        var opts = repository.options(Dataset.KETQUA);
        assertThat(opts.dotdangky()).extracting(Option::value).containsExactlyInAnyOrder(roundA.getId().toString(), roundB.getId().toString());
        assertThat(opts.loaidot()).extracting(Option::value).containsExactlyInAnyOrder("MONHOC", "KLTN");
        assertThat(opts.trangthai()).extracting(Option::value).doesNotContain("TUCHOI");
        assertThat(opts.dacongbo()).extracting(Option::value).containsExactly("true");
        assertThat(opts.bomon()).isEmpty();
        assertThat(opts.giangviendexuat()).isEmpty();
        assertThat(opts.giangvienhuongdan()).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"search-no-group", "missing-profile"})
    void studentWithoutMembershipHasNoRowsCountsOrDataDerivedOptions(String username) {
        auth(username, "SINHVIEN");
        var result = service.ketqua(new KetquaFilterDTO());
        assertThat(result.items()).isEmpty(); assertThat(result.pageInfo().totalElements()).isZero();
        var options = service.options(Dataset.KETQUA);
        assertThat(options.dotdangky()).isEmpty(); assertThat(options.loaidot()).isEmpty();
        assertThat(options.trangthai()).isEmpty(); assertThat(options.dacongbo()).isEmpty();
    }

    @Test void ordinaryMemberCanSearchAndMembershipRemovalTakesEffectImmediately() {
        auth("search-member", "SINHVIEN");
        assertThat(service.ketqua(new KetquaFilterDTO()).items()).extracting(KetquaRow::id).containsExactly(resultA.getId());
        em.createQuery("delete from Thanhviennhom t where t.nhomid=:g and t.sinhvienid=:s")
                .setParameter("g", registrationA.getNhomid()).setParameter("s", studentA.getId()).executeUpdate();
        auth("search-student-a", "SINHVIEN");
        assertThat(service.ketqua(new KetquaFilterDTO()).items()).extracting(KetquaRow::id).containsExactly(resultB.getId());
        assertThat(service.options(Dataset.KETQUA).dotdangky()).extracting(Option::value).containsExactly(roundB.getId().toString());
    }

    @Test void publicationWithdrawalImmediatelyRemovesRowsAndFilterOptions() {
        auth("search-student-a", "SINHVIEN");
        resultB.setDacongbo(false); em.flush();
        assertThat(service.ketqua(new KetquaFilterDTO()).items()).extracting(KetquaRow::id).containsExactly(resultA.getId());
        assertThat(service.options(Dataset.KETQUA).loaidot()).extracting(Option::value).containsExactly("MONHOC");
    }

    @Test void anonymousUnauthenticatedAndUnknownRolesCannotCallRepositoryOrService() {
        var anonymous = new AnonymousAuthenticationToken("test", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        for (var auth : Arrays.asList(null, anonymous, UsernamePasswordAuthenticationToken.unauthenticated("search-chief", "unused"))) {
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThatThrownBy(() -> service.ketqua(new KetquaFilterDTO())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> repository.ketqua(new KetquaFilterDTO())).isInstanceOf(AccessDeniedException.class);
            for (var dataset : Dataset.values()) assertThatThrownBy(() -> service.options(dataset)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test void searchAndOptionsNeverMutateAnyDatabaseRows() {
        em.flush();
        List<String> tables = List.of("detai", "dotdangky", "dangkydetai", "ketqua", "thongbao", "emailketqua", "taikhoan");
        var before = tables.stream().map(t -> jdbc.queryForList("select * from " + t + " order by Id")).toList();
        service.detai(new DetaiFilterDTO()); service.dotdangky(new DotdangkyFilterDTO()); service.ketqua(new KetquaFilterDTO());
        for (var dataset : Dataset.values()) service.options(dataset);
        em.flush();
        var after = tables.stream().map(t -> jdbc.queryForList("select * from " + t + " order by Id")).toList();
        assertThat(after).isEqualTo(before);
    }
}
