package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.HoidongFormDto;
import com.example.quanlydetaisinhvien.dto.HoidongViewDto;
import com.example.quanlydetaisinhvien.entity.Hoidong;
import com.example.quanlydetaisinhvien.entity.Thanhvienhoidong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HoidongService extends CrudService<Hoidong, Long> {

    Page<Hoidong> findPage(Pageable pageable);

    List<HoidongViewDto> findAllViewDto();

    HoidongFormDto getFormDtoById(Long id);

    Hoidong luuHoidong(HoidongFormDto form);

    void xoaHoidong(Long id);

    void kiemtraHoidongHopLe(List<Thanhvienhoidong> thanhviens);
}
