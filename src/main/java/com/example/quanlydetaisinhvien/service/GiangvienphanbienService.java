package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.DangkydetaiOptionDto;
import com.example.quanlydetaisinhvien.dto.GiangvienOptionDto;
import com.example.quanlydetaisinhvien.dto.PhanbienFormDto;
import com.example.quanlydetaisinhvien.dto.PhanbienViewDto;
import com.example.quanlydetaisinhvien.entity.Giangvienphanbien;

import java.util.List;

public interface GiangvienphanbienService extends CrudService<Giangvienphanbien, Long> {

    List<PhanbienViewDto> findAllViewDto();

    List<DangkydetaiOptionDto> getDangkydetaiOptions();

    List<GiangvienOptionDto> getGiangvienOptions();

    Giangvienphanbien phanCong(PhanbienFormDto form);
}
