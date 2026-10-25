package com.example.quanlydetaisinhvien.service;

import java.util.List;
import java.util.Optional;

public interface CrudService<T, ID> {
    List<T> findAll();
    default org.springframework.data.domain.Page<T> findPage(org.springframework.data.domain.Pageable pageable) {
        throw new UnsupportedOperationException("Phân trang chưa được hỗ trợ bởi service này");
    }
    Optional<T> findById(ID id);
    T save(T entity);
    void deleteById(ID id);
}
