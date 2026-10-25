package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Dotdangky;
import com.example.quanlydetaisinhvien.repository.DotdangkyRepository;
import com.example.quanlydetaisinhvien.service.DotdangkyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DotdangkyServiceImpl implements DotdangkyService {
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Dotdangky> layDotMoiNhat(int limit) {
        int size = Math.min(100, Math.max(1, limit));
        return repository.findAll(org.springframework.data.domain.PageRequest.of(0, size,
                org.springframework.data.domain.Sort.by("id").descending())).getContent();
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Dotdangky> layDotDangKyNhom() {
        return repository.findByTrangthaiIn(java.util.List.of(
                com.example.quanlydetaisinhvien.entity.TrangthaiDotdangky.CONGBO,
                com.example.quanlydetaisinhvien.entity.TrangthaiDotdangky.GIAIDOAN2));
    }


    private final DotdangkyRepository repository;

    public DotdangkyServiceImpl(DotdangkyRepository repository) {
        this.repository = repository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Dotdangky> findPage(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Dotdangky> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Dotdangky> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Dotdangky save(Dotdangky entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Dotdangky> searchPage(String keyword, org.springframework.data.domain.Pageable pageable) {
        String q = keyword == null ? "" : keyword.trim();
        return q.isEmpty() ? repository.findAll(pageable) : repository.searchPage(q, pageable);
    }
}
