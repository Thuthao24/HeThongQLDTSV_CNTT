package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;

import com.example.quanlydetaisinhvien.dto.NhomSinhVienDto;
import com.example.quanlydetaisinhvien.dto.TaoNhomRequest;
import com.example.quanlydetaisinhvien.entity.Nhomsinhvien;
import java.util.List;

public interface NhomsinhvienService extends CrudService<Nhomsinhvien, Long> {
    java.util.List<Nhomsinhvien> layNhomChoQuanTri(int limit);

    List<NhomSinhVienDto> layDanhSachNhomDto();

    List<NhomSinhVienDto> layDanhSachNhomDto(int page);

    List<NhomSinhVienDto> layDanhSachNhomChoUser(String currentUsername);

    NhomSinhVienDto layNhomCuaSinhVien(String currentUsername);

    boolean kiemTraSinhVienThuocNhom(Long nhomid, String currentUsername);

    NhomSinhVienDto layChiTietNhomDto(Long id);

    Nhomsinhvien taoNhom(TaoNhomRequest request, String currentUsername);

    Nhomsinhvien capNhatThongTinNhom(Long nhomid, String tenNhomMoi, String currentUsername);

    void xoaNhom(Long nhomid, String currentUsername);

    void themThanhVien(Long nhomid, String masinhvien, String currentUsername);

    void xoaThanhVien(Long nhomid, Long sinhvienid, String currentUsername);

    void doiNhomTruong(Long nhomid, Long sinhvienidMoi, String currentUsername);

    void roiNhom(Long nhomid, String currentUsername);
}
