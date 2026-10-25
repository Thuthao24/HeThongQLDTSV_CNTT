package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.PhanbienFormDto;
import com.example.quanlydetaisinhvien.dto.PhanbienViewDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.impl.GiangvienphanbienServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiangvienphanbienServiceTest {

    @Mock private GiangvienphanbienRepository repository;
    @Mock private DangkydetaiRepository dangkydetaiRepository;
    @Mock private DetaiRepository detaiRepository;
    @Mock private NhomsinhvienRepository nhomsinhvienRepository;
    @Mock private DotdangkyRepository dotdangkyRepository;
    @Mock private GiangvienRepository giangvienRepository;
    @Mock private TaikhoanRepository taikhoanRepository;
    @Mock private GiangvienhuongdanRepository giangvienhuongdanRepository;
    @Mock private NghiepvuDetaiService nghiepvuDetaiService;
    @Mock private DiemRepository diemRepository;
    @Mock private KetquaRepository ketquaRepository;

    @InjectMocks
    private GiangvienphanbienServiceImpl service;

    @Test
    @DisplayName("PASS: findAllViewDto ánh xạ chính xác thông tin đề tài, nhóm, GVHD và GVPB")
    void testFindAllViewDto() {
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setId(1L);
        pb.setDangkydetaiid(100L);
        pb.setGiangvienid(5L);

        Dangkydetai dk = new Dangkydetai();
        dk.setId(100L);
        dk.setDetaiid(10L);
        dk.setNhomid(20L);

        Detai detai = new Detai();
        detai.setId(10L);
        detai.setTendetai("Đề tài ứng dụng AI");
        detai.setDotdangkyid(30L);

        Dotdangky dot = new Dotdangky();
        dot.setId(30L);
        dot.setTendot("Đợt 1 2026");

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setId(20L);
        nhom.setTennhom("Nhóm Chiến Thắng");

        Giangvien gvPb = new Giangvien();
        gvPb.setId(5L);
        gvPb.setMagiangvien("GV005");
        gvPb.setTaikhoanid(50L);

        Taikhoan tkPb = new Taikhoan();
        tkPb.setId(50L);
        tkPb.setHoten("Nguyễn Văn Phản Biện");
        tkPb.setEmail("phanbien@hcmute.edu.vn");

        Giangvienhuongdan gvhd = new Giangvienhuongdan();
        gvhd.setDetaiid(10L);
        gvhd.setGiangvienid(2L);

        Giangvien gvHd = new Giangvien();
        gvHd.setId(2L);
        gvHd.setMagiangvien("GV002");
        gvHd.setTaikhoanid(20L);

        Taikhoan tkHd = new Taikhoan();
        tkHd.setId(20L);
        tkHd.setHoten("Trần Văn Hướng Dẫn");

        when(repository.findAll()).thenReturn(List.of(pb));
        when(dangkydetaiRepository.findAll()).thenReturn(List.of(dk));
        when(detaiRepository.findAll()).thenReturn(List.of(detai));
        when(dotdangkyRepository.findAll()).thenReturn(List.of(dot));
        when(nhomsinhvienRepository.findAll()).thenReturn(List.of(nhom));
        when(giangvienRepository.findAll()).thenReturn(List.of(gvPb, gvHd));
        when(taikhoanRepository.findAll()).thenReturn(List.of(tkPb, tkHd));
        when(giangvienhuongdanRepository.findAll()).thenReturn(List.of(gvhd));

        List<PhanbienViewDto> list = service.findAllViewDto();

        assertNotNull(list);
        assertEquals(1, list.size());
        PhanbienViewDto dto = list.get(0);
        assertEquals(1L, dto.getId());
        assertEquals("Đề tài ứng dụng AI", dto.getTendetai());
        assertEquals("Nhóm Chiến Thắng", dto.getTennhom());
        assertEquals("Nguyễn Văn Phản Biện", dto.getTengiangvien());
        assertEquals("GV005", dto.getMagiangvien());
        assertTrue(dto.getDanhsachGvhd().contains("Trần Văn Hướng Dẫn"));
    }

    @Test
    @DisplayName("PASS: phanCong ủy quyền cho NghiepvuDetaiService khi dữ liệu hợp lệ")
    void testPhanCong_ThanhCong() {
        PhanbienFormDto form = new PhanbienFormDto(100L, 5L);
        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setId(1L);
        pb.setDangkydetaiid(100L);
        pb.setGiangvienid(5L);

        when(nghiepvuDetaiService.phanGiangvienPhanbien(100L, 5L)).thenReturn(pb);

        Giangvienphanbien result = service.phanCong(form);

        assertNotNull(result);
        assertEquals(100L, result.getDangkydetaiid());
        assertEquals(5L, result.getGiangvienid());
        verify(nghiepvuDetaiService).phanGiangvienPhanbien(100L, 5L);
    }

    @Test
    @DisplayName("FAIL: phanCong ném lỗi khi thiếu đề tài hoặc thiếu giảng viên")
    void testPhanCong_ThieuThongTin() {
        PhanbienFormDto form1 = new PhanbienFormDto(null, 5L);
        assertThrows(NghiepvuException.class, () -> service.phanCong(form1));

        PhanbienFormDto form2 = new PhanbienFormDto(100L, null);
        assertThrows(NghiepvuException.class, () -> service.phanCong(form2));
    }
}
