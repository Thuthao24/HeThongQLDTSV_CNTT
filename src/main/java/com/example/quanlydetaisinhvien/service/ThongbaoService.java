package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.ThongbaoFormDTO;
import com.example.quanlydetaisinhvien.dto.ThongbaoViewDTO;
import java.util.List;

public interface ThongbaoService {
    /** At most five latest notices for the shared header; requires notice read access. */
    List<ThongbaoViewDTO> findLatestForHeader();

    List<ThongbaoViewDTO> findAll();
    List<ThongbaoViewDTO> search(String keyword);

    org.springframework.data.domain.Page<ThongbaoViewDTO> searchPage(
            String keyword,
            org.springframework.data.domain.Pageable pageable);
    ThongbaoViewDTO findById(Long id);
    ThongbaoViewDTO create(ThongbaoFormDTO form);
    ThongbaoViewDTO update(Long id, ThongbaoFormDTO form);
    void deleteById(Long id);
    boolean canManage();
    void requireManageAccess();
}
