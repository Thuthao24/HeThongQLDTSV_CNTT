package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Ketqua;
import com.example.quanlydetaisinhvien.entity.HinhthucDanhgia;

import java.util.List;
import java.util.Map;

/**
 * Reads (including inherited findAll/findById) are scoped to the current user.
 * Students see only published group results; Trưởng khoa sees all results.
 * Inherited write operations require Trưởng khoa.
 */
public interface KetquaService extends CrudService<Ketqua, Long> {
    /** Uses the authenticated account; never accepts a student or group ID from the client. */
    List<Ketqua> findVisibleToCurrentUser();

    org.springframework.data.domain.Page<Ketqua> findVisiblePage(
            org.springframework.data.domain.Pageable pageable);
    Map<Long, KetquaPublicationInfo> publicationInfo(List<Ketqua> results);
    void phanCongHoiDong(Long ketquaid, HinhthucDanhgia hinhthuc, Long hoidongid);
    Ketqua congboKetqua(Long ketquaid);
}
