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
    private final com.example.quanlydetaisinhvien.service.morong.TimkiemNangcaoService search;

    public DetaiServiceImpl(DetaiRepository repository, com.example.quanlydetaisinhvien.service.morong.TimkiemNangcaoService search) {
        this.repository = repository;
        this.search = search;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public org.springframework.data.domain.Page<Detai> searchPage(com.example.quanlydetaisinhvien.dto.morong.DetaiFilterDTO filter) {
        var result = search.detai(filter);
        // Bounded page lookup preserves the existing GĐ1 permission API without per-row queries.
        var ids = result.items().stream().map(com.example.quanlydetaisinhvien.dto.morong.SearchModels.DetaiRow::id).toList();
        var byId = repository.findAllById(ids).stream().collect(java.util.stream.Collectors.toMap(Detai::getId, d -> d));
        var items = ids.stream().map(byId::get).toList();
        return new org.springframework.data.domain.PageImpl<>(items,
                org.springframework.data.domain.PageRequest.of(filter.getPage(), filter.getSize()), result.pageInfo().totalElements());
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
