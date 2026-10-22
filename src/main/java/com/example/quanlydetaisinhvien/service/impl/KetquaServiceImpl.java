package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Ketqua;
import com.example.quanlydetaisinhvien.entity.HinhthucDanhgia;
import com.example.quanlydetaisinhvien.repository.KetquaRepository;
import com.example.quanlydetaisinhvien.service.KetquaService;
import com.example.quanlydetaisinhvien.service.KetquaPublicationInfo;
import com.example.quanlydetaisinhvien.service.KetquaPublicationService;
import com.example.quanlydetaisinhvien.service.NghiepvuDetaiService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
public class KetquaServiceImpl implements KetquaService {

    private final KetquaRepository repository;
    private final KetquaPublicationService publicationService;
    private final NghiepvuDetaiService businessService;

    public KetquaServiceImpl(KetquaRepository repository, KetquaPublicationService publicationService,
                            NghiepvuDetaiService businessService) {
        this.repository = repository;
        this.publicationService = publicationService;
        this.businessService = businessService;
    }

    @Override
    public Map<Long, KetquaPublicationInfo> publicationInfo(List<Ketqua> results) {
        requireTruongKhoa();
        return publicationService.info(results);
    }

    @Override
    public void phanCongHoiDong(Long id, HinhthucDanhgia form, Long councilId) {
        requireTruongKhoa();
        publicationService.assign(id, form, councilId);
    }

    @Override
    public Ketqua congboKetqua(Long id) {
        requireTruongKhoa();
        return businessService.congboKetqua(id);
    }

    @Override
    public List<Ketqua> findAll() {
        return findVisibleToCurrentUser();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Ketqua> findVisiblePage(
            org.springframework.data.domain.Pageable pageable) {

        Authentication authentication = requireResultReader();

        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "Danh sach ket qua bat buoc phan trang.");
        }

        var request = org.springframework.data.domain.PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(100, Math.max(1, pageable.getPageSize())),
                org.springframework.data.domain.Sort.by("id").descending()
        );

        if (hasRole(authentication, "ROLE_TRUONGKHOA")) {
            return repository.findAll(request);
        }

        return repository.findPublishedPageForStudent(
                authentication.getName(),
                request
        );
    }
    @Override
    public List<Ketqua> findVisibleToCurrentUser() {
        Authentication authentication = requireResultReader();
        if (hasRole(authentication, "ROLE_TRUONGKHOA")) {
            return repository.findAll();
        }
        return repository.findPublishedForStudent(authentication.getName());
    }

    @Override
    public Optional<Ketqua> findById(Long id) {
        Authentication authentication = requireResultReader();
        if (hasRole(authentication, "ROLE_TRUONGKHOA")) {
            return repository.findById(id);
        }
        // Missing, unpublished and another group's results all return Optional.empty().
        return repository.findPublishedByIdForStudent(id, authentication.getName());
    }

    @Override
    public Ketqua save(Ketqua entity) {
        requireTruongKhoa();
        throw new com.example.quanlydetaisinhvien.exception.NghiepvuException("Không ghi kết quả trực tiếp; sử dụng quy trình tính điểm và công bố.");
    }

    @Override
    public void deleteById(Long id) {
        requireTruongKhoa();
        throw new com.example.quanlydetaisinhvien.exception.NghiepvuException("Không xóa kết quả trực tiếp sau khi đã thực hiện đánh giá.");
    }

    private Authentication requireResultReader() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(hasRole(authentication, "ROLE_TRUONGKHOA")
                    || hasRole(authentication, "ROLE_SINHVIEN"))) {
            throw new AccessDeniedException("Bạn không có quyền tra cứu kết quả.");
        }
        return authentication;
    }

    private void requireTruongKhoa() {
        if (!hasRole(requireResultReader(), "ROLE_TRUONGKHOA")) {
            throw new AccessDeniedException("Chỉ Trưởng khoa được quản lý kết quả.");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
