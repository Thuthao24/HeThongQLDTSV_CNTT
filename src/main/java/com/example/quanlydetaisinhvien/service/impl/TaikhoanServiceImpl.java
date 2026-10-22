package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Taikhoan;
import com.example.quanlydetaisinhvien.repository.TaikhoanRepository;
import com.example.quanlydetaisinhvien.service.TaikhoanService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaikhoanServiceImpl implements TaikhoanService {
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Optional<Taikhoan> layTheoTenDangNhap(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return repository.findByTendangnhap(username);
    }


    private final TaikhoanRepository repository;

    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    public TaikhoanServiceImpl(
            TaikhoanRepository repository,
            org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Taikhoan> findPage(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Taikhoan> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Taikhoan> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('TRUONGKHOA')")
    public Taikhoan save(Taikhoan entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Tai khoan khong hop le");
        }

        String password = entity.getMatkhau();

        if (entity.getId() != null) {
            Taikhoan existing = repository.findById(entity.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Tai khoan khong ton tai"));

            if (password == null || password.isBlank()) {
                entity.setMatkhau(existing.getMatkhau());
            } else if (password.equals(existing.getMatkhau())) {
                // Giu hash cu khi chi sua thong tin
            } else {
                if (password.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) {
                    throw new IllegalArgumentException("Khong cho phep nhap BCrypt hash truc tiep");
                }
                if (password.length() < 8) {
                    throw new IllegalArgumentException("Mat khau phai co it nhat 8 ky tu");
                }
                entity.setMatkhau(encoder.encode(password));
            }
        } else {
            if (password == null || password.length() < 8) {
                throw new IllegalArgumentException("Mat khau phai co it nhat 8 ky tu");
            }
            if (password.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) {
                throw new IllegalArgumentException("Khong cho phep nhap BCrypt hash truc tiep");
            }
            entity.setMatkhau(encoder.encode(password));
        }

        return repository.save(entity);
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('TRUONGKHOA')")
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Taikhoan> searchPage(String keyword, org.springframework.data.domain.Pageable pageable) {
        String q = keyword == null ? "" : keyword.trim();
        return q.isEmpty() ? repository.findAll(pageable) : repository.searchPage(q, pageable);
    }
}
