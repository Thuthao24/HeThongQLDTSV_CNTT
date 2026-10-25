package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Detai;
import com.example.quanlydetaisinhvien.repository.DetaiRepository;
import com.example.quanlydetaisinhvien.service.DetaiService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DetaiServiceImpl implements DetaiService {
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Detai> layDetaiCongBo() {
        return repository.findByTrangthai(com.example.quanlydetaisinhvien.entity.TrangthaiDetai.CONGBO);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Detai> layDetaiCongBoTheoDot(Long dotId) {
        return repository.findByDotdangkyidAndTrangthai(dotId,
                com.example.quanlydetaisinhvien.entity.TrangthaiDetai.CONGBO);
    }


    private final DetaiRepository repository;

    public DetaiServiceImpl(DetaiRepository repository) {
        this.repository = repository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Detai> findPage(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Detai> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Detai> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Detai save(Detai entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Detai> searchPage(String keyword, org.springframework.data.domain.Pageable pageable) {
        String q = keyword == null ? "" : keyword.trim();
        return q.isEmpty() ? repository.findAll(pageable) : repository.searchPage(q, pageable);
    }
}
