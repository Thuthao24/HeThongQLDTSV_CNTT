package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Sinhvien;
import com.example.quanlydetaisinhvien.repository.SinhvienRepository;
import com.example.quanlydetaisinhvien.service.SinhvienService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SinhvienServiceImpl implements SinhvienService {

    private final SinhvienRepository repository;

    public SinhvienServiceImpl(SinhvienRepository repository) {
        this.repository = repository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Sinhvien> findPage(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Sinhvien> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Sinhvien> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Sinhvien save(Sinhvien entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Sinhvien> searchPage(String keyword, org.springframework.data.domain.Pageable pageable) {
        String q = keyword == null ? "" : keyword.trim();
        return q.isEmpty() ? repository.findAll(pageable) : repository.searchPage(q, pageable);
    }
}
