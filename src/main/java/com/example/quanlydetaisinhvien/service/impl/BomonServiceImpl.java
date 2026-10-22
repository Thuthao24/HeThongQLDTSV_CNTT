package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Bomon;
import com.example.quanlydetaisinhvien.repository.BomonRepository;
import com.example.quanlydetaisinhvien.service.BomonService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BomonServiceImpl implements BomonService {

    private final BomonRepository repository;

    public BomonServiceImpl(BomonRepository repository) {
        this.repository = repository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Bomon> findPage(org.springframework.data.domain.Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<Bomon> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Bomon> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Bomon save(Bomon entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Bomon> searchPage(String keyword, org.springframework.data.domain.Pageable pageable) {
        String q = keyword == null ? "" : keyword.trim();
        return q.isEmpty() ? repository.findAll(pageable) : repository.searchPage(q, pageable);
    }
}
