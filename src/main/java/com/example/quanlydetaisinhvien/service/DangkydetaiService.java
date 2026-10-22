package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.DangKyDeTaiDto;
import com.example.quanlydetaisinhvien.dto.DangKyDeTaiRequest;
import com.example.quanlydetaisinhvien.entity.Dangkydetai;
import java.util.List;

public interface DangkydetaiService extends CrudService<Dangkydetai, Long> {

    List<DangKyDeTaiDto> layDanhSachDangKyDto();

    List<DangKyDeTaiDto> layDanhSachDangKyDto(int page);

    List<DangKyDeTaiDto> layDanhSachDangKyChoUser(String currentUsername);

    DangKyDeTaiDto layDangKyCuaSinhVien(String currentUsername);

    Dangkydetai dangkyDetai(DangKyDeTaiRequest request, String currentUsername);

    void huyDangky(Long dangkydetaiid, String currentUsername);

    Dangkydetai duyetDangky(Long dangkydetaiid, boolean chapNhan, String currentUsername);
}
