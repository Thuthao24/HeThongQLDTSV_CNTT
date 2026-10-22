package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Hoidong;
import com.example.quanlydetaisinhvien.repository.HoidongRepository;
import com.example.quanlydetaisinhvien.service.HoidongService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.DotdangkyRepository;

import java.util.List;
import java.util.Optional;

@Service
public class HoidongServiceImpl implements HoidongService {

    private final HoidongRepository repository;
    private final DotdangkyRepository rounds;
    private final com.example.quanlydetaisinhvien.repository.TaikhoanRepository accounts;
    private final com.example.quanlydetaisinhvien.repository.GiangvienRepository lecturers;

    public HoidongServiceImpl(
            HoidongRepository repository,
            DotdangkyRepository rounds,
            com.example.quanlydetaisinhvien.repository.TaikhoanRepository accounts,
            com.example.quanlydetaisinhvien.repository.GiangvienRepository lecturers) {
        this.repository = repository;
        this.rounds = rounds;
        this.accounts = accounts;
        this.lecturers = lecturers;
    }

    private boolean isChiefForCouncil() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chua dang nhap.");
        }

        return auth.getAuthorities().stream().anyMatch(
                a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()));
    }

    private Long currentLecturerIdForCouncil() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Chua dang nhap.");
        }

        var account = accounts.findByTendangnhap(auth.getName())
                .orElseThrow(() ->
                        new org.springframework.security.access.AccessDeniedException(
                                "Tai khoan khong ton tai."));

        if (!Boolean.TRUE.equals(account.getKichhoat())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Tai khoan da khoa.");
        }

        return lecturers.findByTaikhoanid(account.getId())
                .orElseThrow(() ->
                        new org.springframework.security.access.AccessDeniedException(
                                "Tai khoan chua lien ket giang vien."))
                .getId();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Hoidong> findPage(org.springframework.data.domain.Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException("Danh sach hoi dong phai phan trang.");
        }

        var request = org.springframework.data.domain.PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(100, Math.max(1, pageable.getPageSize())),
                pageable.getSort().isUnsorted()
                    ? org.springframework.data.domain.Sort.by("id").descending()
                    : pageable.getSort()
        );

        if (isChiefForCouncil()) {
            return repository.findAll(request);
        }

        return repository.findVisibleForLecturer(
                currentLecturerIdForCouncil(), request);
    }

    @Override
    public List<Hoidong> findAll() {
        if (isChiefForCouncil()) {
            return repository.findAll();
        }

        return repository.findAllVisibleForLecturer(
                currentLecturerIdForCouncil());
    }

    @Override
    public Optional<Hoidong> findById(Long id) {
        if (isChiefForCouncil()) {
            return repository.findById(id);
        }

        return repository.findVisibleById(
                id, currentLecturerIdForCouncil());
    }

    @Override
    @Transactional
    public Hoidong save(Hoidong entity) {
        if (entity.getDotdangkyid() == null || !rounds.existsById(entity.getDotdangkyid()))
            throw new NghiepvuException("Đợt đăng ký không tồn tại.");
        if (entity.getTenhoidong() == null || entity.getTenhoidong().isBlank())
            throw new NghiepvuException("Vui lòng nhập tên hội đồng.");
        entity.setTenhoidong(entity.getTenhoidong().trim());
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // Xóa hội đồng đang có thành viên/phân công phải qua quy trình quản lý riêng.
        throw new NghiepvuException("Không hỗ trợ xóa hội đồng trực tiếp; cần xử lý phân công và thành viên trước.");
    }
}
