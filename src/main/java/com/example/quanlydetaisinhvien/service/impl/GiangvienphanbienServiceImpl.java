package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.Giangvienphanbien;
import com.example.quanlydetaisinhvien.repository.GiangvienphanbienRepository;
import com.example.quanlydetaisinhvien.service.GiangvienphanbienService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GiangvienphanbienServiceImpl implements GiangvienphanbienService {

    private final GiangvienphanbienRepository repository;

    public GiangvienphanbienServiceImpl(GiangvienphanbienRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Giangvienphanbien> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Giangvienphanbien> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Giangvienphanbien save(Giangvienphanbien entity) {
        throw new com.example.quanlydetaisinhvien.exception.NghiepvuException("Không phân công phản biện bằng CRUD trực tiếp; cần kiểm tra GVHD và quyền phân công.");
    }

    @Override
    public void deleteById(Long id) {
        throw new com.example.quanlydetaisinhvien.exception.NghiepvuException("Không xóa phân công phản biện trực tiếp.");
    }
}
