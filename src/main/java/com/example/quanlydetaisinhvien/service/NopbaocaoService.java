package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.NopBaoCaoDto;
import com.example.quanlydetaisinhvien.dto.NopBaoCaoRequest;
import com.example.quanlydetaisinhvien.entity.Nopbaocao;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface NopbaocaoService extends CrudService<Nopbaocao, Long> {

    List<NopBaoCaoDto> layDanhSachBaoCaoDto();

    List<NopBaoCaoDto> layDanhSachBaoCaoDto(int page);

    List<NopBaoCaoDto> layDanhSachBaoCaoChoUser(String currentUsername);

    NopBaoCaoDto layBaoCaoMoiNhatCuaDangKy(Long dangkydetaiid);

    boolean kiemTraQuyenTruyCapBaoCao(Long baocaoid, String currentUsername);

    Nopbaocao nopBaocao(NopBaoCaoRequest request, String currentUsername);

    Nopbaocao nopBaocaoVoiUpload(Long dangkydetaiid, MultipartFile file, String currentUsername);

    void xoaBaocao(Long id, String currentUsername);
}
