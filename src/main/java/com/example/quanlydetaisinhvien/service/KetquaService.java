package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.KetquaDoiChieuDto;
import com.example.quanlydetaisinhvien.entity.HinhthucDanhgia;
import com.example.quanlydetaisinhvien.entity.Ketqua;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface KetquaService extends CrudService<Ketqua, Long> {
    List<Ketqua> findVisibleToCurrentUser();
    Page<Ketqua> findVisiblePage(Pageable pageable);
    Ketqua congboKetqua(Long ketquaid);
    void phanCongHoiDong(Long ketquaid, HinhthucDanhgia hinhthuc, Long hoidongid);
    Map<Long, KetquaPublicationInfo> publicationInfo(List<Ketqua> results);

    List<KetquaDoiChieuDto> findDoiChieuDto(List<Ketqua> results);
    List<KetquaDoiChieuDto> findAllDoiChieuDto();
    void tinhLaiKetQua(Long dangkydetaiid);
    void tinhLaiTatCaKetQua();
    boolean doiChieuKetQua(Long dangkydetaiid);
}
