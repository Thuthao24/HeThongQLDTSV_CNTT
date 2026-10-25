package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Giangvien;
import com.example.quanlydetaisinhvien.repository.GiangvienRepository;
import com.example.quanlydetaisinhvien.service.GiangvienService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GiangvienServiceImpl implements GiangvienService {

    private final GiangvienRepository repository;

    public GiangvienServiceImpl(GiangvienRepository repository) {
        this.repository = repository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Giangvien> findPage(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Giangvien> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Giangvien> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Giangvien save(Giangvien entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Giangvien> searchPage(String keyword, org.springframework.data.domain.Pageable pageable) {
        String q = keyword == null ? "" : keyword.trim();
        return q.isEmpty() ? repository.findAll(pageable) : repository.searchPage(q, pageable);
    }
}
