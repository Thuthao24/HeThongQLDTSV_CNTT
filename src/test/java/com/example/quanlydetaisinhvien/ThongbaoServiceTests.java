package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.ThongbaoViewDTO;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.ThongbaoNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ThongbaoServiceTests extends ThongbaoTestSupport {
    @Test void createsUsingAuthenticatedAccountAndServerTime() {
        var before = LocalDateTime.now().minusSeconds(1);
        var saved = service.create(form("  Lịch báo cáo  ", "Dòng 1\nDòng 2"));
        em.clear();
        var row = repository.findById(saved.id()).orElseThrow();
        assertThat(row.getTieude()).isEqualTo("Lịch báo cáo");
        assertThat(row.getNoidung()).isEqualTo("Dòng 1\nDòng 2");
        assertThat(row.getNguoidangid()).isEqualTo(chief.getId());
        assertThat(row.getNgaydang()).isBetween(before, LocalDateTime.now().plusSeconds(1));
        assertThat(saved.nguoidangHoten()).isEqualTo(chief.getHoten());
        assertThat(service.canManage()).isTrue();
    }

    @Test void updatesAnotherAuthorsNoticePreservingOriginalMetadata() {
        var author = accounts.findByTendangnhap("notice-lecturer").orElseThrow();
        var original = notice("Tiêu đề cũ", "Nội dung cũ", author.getId(), oldDate);
        var saved = service.update(original.getId(), form("Tiêu đề mới", "Nội dung mới"));
        em.clear();
        var row = repository.findById(original.getId()).orElseThrow();
        assertThat(row.getNguoidangid()).isEqualTo(author.getId());
        assertThat(row.getNgaydang()).isEqualTo(oldDate);
        assertThat(saved.nguoidangHoten()).isEqualTo(author.getHoten());
        assertThat(saved.tieude()).isEqualTo("Tiêu đề mới");
        assertThat(saved.noidung()).isEqualTo("Nội dung mới");
    }

    @Test void deletesOnlyRequestedNoticeAndMissingRecordsFailClearly() {
        var first = notice("Một", "Nội dung", chief.getId(), oldDate);
        var other = notice("Hai", "Nội dung", chief.getId(), oldDate);
        service.deleteById(first.getId());
        assertThat(repository.findById(first.getId())).isEmpty();
        assertThat(repository.findById(other.getId())).isPresent();
        assertThatThrownBy(() -> service.findById(first.getId())).isInstanceOf(ThongbaoNotFoundException.class);
        assertThatThrownBy(() -> service.update(first.getId(), form("Mới", "Nội dung"))).isInstanceOf(ThongbaoNotFoundException.class);
        assertThatThrownBy(() -> service.deleteById(first.getId())).isInstanceOf(ThongbaoNotFoundException.class);
    }

    @ParameterizedTest @ValueSource(strings = {"TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN"})
    void allFourRolesCanReadSearchAndSeeAuthorNames(String role) {
        var row = notice("Đăng ký", "Lịch học", chief.getId(), oldDate);
        authenticate("reader", role);
        assertThat(service.findAll()).extracting(ThongbaoViewDTO::id).containsExactly(row.getId());
        assertThat(service.search("  LỊCH  ")).extracting(ThongbaoViewDTO::id).containsExactly(row.getId());
        assertThat(service.search(" \t ")).hasSize(1);
        assertThat(service.search(null)).hasSize(1);
        assertThat(service.findById(row.getId()).nguoidangHoten()).isEqualTo(chief.getHoten());
        if (!role.equals("TRUONGKHOA")) assertThat(service.canManage()).isFalse();
    }

    @ParameterizedTest @ValueSource(strings = {"SINHVIEN", "GIANGVIEN", "TRUONGBOMON", "UNKNOWN"})
    void directServiceWritesRejectOtherRolesWithoutChangingAnyNotice(String role) {
        var row = notice("Gốc", "Nội dung", chief.getId(), oldDate);
        var before = noticeSnapshot();
        authenticate("unauthorized", role);
        assertThatThrownBy(() -> service.create(form("Mới", "Nội dung"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.update(row.getId(), form("Sửa", "Nội dung"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.deleteById(row.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(service::requireManageAccess).isInstanceOf(AccessDeniedException.class);
        assertThat(noticeSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest @ValueSource(strings = {"missing", "disabled", "wrong-role"})
    void staleChiefAuthenticationCannotWriteOrGetManagementAccess(String state) {
        String username = "notice-chief";
        if (state.equals("missing")) username = "does-not-exist";
        if (state.equals("disabled")) chief.setKichhoat(false);
        if (state.equals("wrong-role")) chief.setVaitro(Vaitro.SINHVIEN);
        em.flush();
        authenticate(username, "TRUONGKHOA");
        var before = noticeSnapshot();
        assertThat(service.canManage()).isFalse();
        assertThatThrownBy(() -> service.create(form("Mới", "Nội dung"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(service::requireManageAccess).isInstanceOf(AccessDeniedException.class);
        assertThat(noticeSnapshot()).isEqualTo(before);
    }

    @Test void anonymousAndUnauthenticatedUsersCannotCallReadOrWriteServices() {
        var anonymous = new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        for (var authentication : Arrays.asList(null, anonymous,
                UsernamePasswordAuthenticationToken.unauthenticated("notice-chief", "unused"))) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            assertThatThrownBy(service::findAll).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> service.create(form("Mới", "Nội dung"))).isInstanceOf(AccessDeniedException.class);
        }
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"", "   ", "oversize"})
    void invalidTitleOrBodyIsRejectedEvenWhenBypassingController(String invalid) {
        var row = notice("Gốc", "Nội dung", chief.getId(), oldDate);
        var before = noticeSnapshot();
        String title = "oversize".equals(invalid) ? "T".repeat(256) : invalid;
        String body = "oversize".equals(invalid) ? "N".repeat(10001) : invalid;
        assertThatThrownBy(() -> service.create(form(title, "Hợp lệ"))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.update(row.getId(), form("Hợp lệ", body))).isInstanceOf(ConstraintViolationException.class);
        assertThat(noticeSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest @NullSource @ValueSource(longs = {0, -1})
    void invalidIdsFailBeforeAnyMutation(Long id) {
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(id, form("Mới", "Nội dung"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.deleteById(id)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void noticeCrudDoesNotChangeExistingResultsOrEmailOutbox() {
        var result = new Ketqua(); result.setDangkydetaiid(700L); result.setDacongbo(true);
        result.setDiemcuoi(new java.math.BigDecimal("8.50")); result.setNgaycongbo(oldDate); em.persist(result);
        var email = new Emailketqua(); email.setKetquaid(result.getId()); email.setSinhvienid(700L);
        email.setEmail("fixture@example.invalid"); email.setTieude("Kết quả"); email.setNoidung("Snapshot");
        email.setTrangthai(TrangthaiEmailketqua.SENT); email.setNgaytao(oldDate); email.setNgaygui(oldDate); em.persist(email); em.flush();
        var resultsBefore = jdbc.queryForList("select * from ketqua order by Id");
        var emailsBefore = jdbc.queryForList("select * from emailketqua order by Id");
        var notice = service.create(form("Mới", "Nội dung"));
        service.update(notice.id(), form("Sửa", "Nội dung mới"));
        service.deleteById(notice.id());
        assertThat(jdbc.queryForList("select * from ketqua order by Id")).isEqualTo(resultsBefore);
        assertThat(jdbc.queryForList("select * from emailketqua order by Id")).isEqualTo(emailsBefore);
    }
}
