package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.ThongbaoNotFoundException;
import com.example.quanlydetaisinhvien.exception.ThongbaoConflictException;
import com.example.quanlydetaisinhvien.service.ThongbaoRevision;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.ThongbaoService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ThongbaoServiceImpl implements ThongbaoService {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("ngaydang"), Sort.Order.desc("id"));
    private static final Set<String> READ_ROLES = Set.of("ROLE_TRUONGKHOA", "ROLE_TRUONGBOMON",
            "ROLE_GIANGVIEN", "ROLE_SINHVIEN");
    private final ThongbaoRepository repository;
    private final TaikhoanRepository accounts;
    private final Validator validator;

    public ThongbaoServiceImpl(ThongbaoRepository repository, TaikhoanRepository accounts, Validator validator) {
        this.repository = repository; this.accounts = accounts; this.validator = validator;
    }

    @Override
    public List<ThongbaoViewDTO> findAll() { return search(""); }

    @Override
    public List<ThongbaoViewDTO> findLatestForHeader() {
        var auth=requireReader();
        if(hasManagerRole(auth)) return List.of();
        return views(repository.visibleLatest(false,readerRole(auth),"%",
                org.springframework.data.domain.PageRequest.of(0,5,NEWEST_FIRST)));
    }

    @Override
    public List<ThongbaoViewDTO> search(String keyword) {
        var auth=requireReader();
        String query = keyword == null ? "" : keyword.strip();
        var rows = repository.visible(hasManagerRole(auth),readerRole(auth),pattern(query),NEWEST_FIRST);
        return views(rows);
    }

    @Override
    public org.springframework.data.domain.Page<ThongbaoViewDTO> searchPage(
            String keyword,
            org.springframework.data.domain.Pageable pageable) {

        var auth=requireReader();

        String query = keyword == null ? "" : keyword.strip();

        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "Danh sach thong bao bat buoc phan trang.");
        }

        var safePageable = org.springframework.data.domain.PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(100, Math.max(1, pageable.getPageSize())),
                NEWEST_FIRST
        );

        var page = repository.visible(hasManagerRole(auth),readerRole(auth),pattern(query),safePageable);

        if(safePageable.getPageNumber()>0 && page.getContent().isEmpty()) {
            int last=Math.max(0,page.getTotalPages()-1);
            safePageable=org.springframework.data.domain.PageRequest.of(last,safePageable.getPageSize(),NEWEST_FIRST);
            page=repository.visible(hasManagerRole(auth),readerRole(auth),pattern(query),safePageable);
        }

        return new org.springframework.data.domain.PageImpl<>(
                views(page.getContent()),
                safePageable,
                page.getTotalElements()
        );
    }
    private List<ThongbaoViewDTO> views(List<Thongbao> rows) {
        if (rows.isEmpty()) return List.of();
        repository.withAudience(rows.stream().map(Thongbao::getId).toList());
        // One batch lookup avoids fetching an account for every notice.
        var ids = rows.stream().map(Thongbao::getNguoidangid).collect(Collectors.toSet());
        Map<Long, String> authors = new HashMap<>();
        accounts.findAllById(ids).forEach(account -> authors.put(account.getId(), account.getHoten()));
        return rows.stream().map(row -> view(row, authors.get(row.getNguoidangid()))).toList();
    }

    @Override
    public ThongbaoViewDTO findById(Long id) {
        var auth=requireReader();
        var row=existing(id);
        if(!hasManagerRole(auth) && !row.getRecipientRoles().isEmpty()
                && !row.getRecipientRoles().contains(readerRole(auth)))
            throw new ThongbaoNotFoundException(); // Same 404 as an absent ID: do not reveal another role's notice.
        return view(row);
    }

    @Override
    @Transactional
    public ThongbaoViewDTO create(ThongbaoFormDTO form) {
        Taikhoan author = requireManager();
        validate(form);
        Thongbao row = new Thongbao();
        row.setTieude(form.getTieude().strip());
        row.setNoidung(form.getNoidung());
        row.setNguoidangid(author.getId());
        row.setRecipientRoles(form.getRecipientRoles());
        row.setNgaydang(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0));
        repository.saveAndFlush(row);
        return view(row, author.getHoten());
    }

    @Override
    @Transactional
    public ThongbaoViewDTO update(Long id, ThongbaoFormDTO form) {
        requireManager();
        Thongbao row = locked(id);
        validate(form);
        // An identical POST replay is harmless. A stale edit with other content must not overwrite a newer save.
        if(row.getTieude().equals(form.getTieude().strip()) && row.getNoidung().equals(form.getNoidung())
                && effectiveRoles(row).equals(form.getRecipientRoles())) return view(row);
        if(form.getRevision()!=null && !ThongbaoRevision.digest(row.getTieude(),row.getNoidung(),effectiveRoles(row)).equals(form.getRevision()))
            throw new ThongbaoConflictException();
        row.setTieude(form.getTieude().strip());
        row.setNoidung(form.getNoidung());
        row.setRecipientRoles(form.getRecipientRoles());
        // Keep original author/time even when a different dean edits the notice.
        repository.saveAndFlush(row);
        return view(row);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        requireManager();
        repository.delete(locked(id));
        repository.flush();
    }

    @Override
    public boolean canManage() {
        Authentication authentication = requireReader();
        return hasManagerRole(authentication) && accounts.findByTendangnhap(authentication.getName())
                .filter(this::isActiveManager).isPresent();
    }

    @Override
    public void requireManageAccess() { requireManager(); }

    private Authentication requireReader() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getAuthorities().stream().noneMatch(a -> READ_ROLES.contains(a.getAuthority()))) {
            throw new AccessDeniedException("Bạn không có quyền xem thông báo.");
        }
        return authentication;
    }

    private DoituongThongbao readerRole(Authentication auth) {
        return Arrays.stream(DoituongThongbao.values()).filter(role->auth.getAuthorities().stream()
                .anyMatch(a->a.getAuthority().equals("ROLE_"+role.name()))).findFirst().orElse(DoituongThongbao.SINHVIEN);
    }
    private String pattern(String keyword) {
        return "%"+keyword.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
    }
    private Set<DoituongThongbao> effectiveRoles(Thongbao row) {
        return row.getRecipientRoles().isEmpty()?DoituongThongbao.all():Set.copyOf(row.getRecipientRoles());
    }

    private Taikhoan requireManager() {
        Authentication authentication = requireReader();
        if (!hasManagerRole(authentication)) throw denied();
        return accounts.findByTendangnhap(authentication.getName()).filter(this::isActiveManager)
                .orElseThrow(this::denied);
    }

    private boolean hasManagerRole(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()));
    }

    private boolean isActiveManager(Taikhoan account) {
        return account.getVaitro() == Vaitro.TRUONGKHOA && Boolean.TRUE.equals(account.getKichhoat());
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("Chỉ Trưởng khoa đang hoạt động được quản lý thông báo.");
    }

    private Thongbao existing(Long id) {
        if (id == null || id <= 0) throw new IllegalArgumentException("ID thông báo không hợp lệ.");
        return repository.findById(id).orElseThrow(ThongbaoNotFoundException::new);
    }
    private Thongbao locked(Long id) {
        if(id==null || id<=0) throw new IllegalArgumentException("ID thông báo không hợp lệ.");
        return repository.findForUpdate(id).orElseThrow(ThongbaoNotFoundException::new);
    }

    private void validate(ThongbaoFormDTO form) {
        if (form == null) throw new IllegalArgumentException("Biểu mẫu thông báo không hợp lệ.");
        var violations = validator.validate(form);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
    }

    private ThongbaoViewDTO view(Thongbao row) {
        return view(row, accounts.findById(row.getNguoidangid()).map(Taikhoan::getHoten).orElse(null));
    }

    private ThongbaoViewDTO view(Thongbao row, String author) {
        return new ThongbaoViewDTO(row.getId(), row.getTieude(), row.getNoidung(),
                author == null || author.isBlank() ? "Không xác định" : author, row.getNgaydang(),effectiveRoles(row));
    }
}
