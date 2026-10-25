package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Dangkydetai;
import com.example.quanlydetaisinhvien.entity.Diem;
import com.example.quanlydetaisinhvien.entity.Giangvienphanbien;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NghiepvuPhanbienTest {

    @Mock private DotdangkyRepository dotdangkyRepository;
    @Mock private DetaiRepository detaiRepository;
    @Mock private GiangvienhuongdanRepository giangvienhuongdanRepository;
    @Mock private NhomsinhvienRepository nhomsinhvienRepository;
    @Mock private ThanhviennhomRepository thanhviennhomRepository;
    @Mock private DangkydetaiRepository dangkydetaiRepository;
    @Mock private NopbaocaoRepository nopbaocaoRepository;
    @Mock private DiemRepository diemRepository;
    @Mock private KetquaRepository ketquaRepository;
    @Mock private GiangvienphanbienRepository giangvienphanbienRepository;
    @Mock private KetquaPublicationService publicationService;

    @InjectMocks
    private NghiepvuDetaiService nghiepvuDetaiService;

    @Test
    @DisplayName("PASS: Phân GVPB thành công khi giảng viên không trùng với GVHD")
    void testPhanGiangvienPhanbien_ThanhCong() {
        Long dangkydetaiId = 1L;
        Long detaiId = 10L;
        Long giangvienId = 5L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dangkydetaiId);
        dk.setDetaiid(detaiId);

        when(dangkydetaiRepository.findById(dangkydetaiId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(detaiId, giangvienId)).thenReturn(false);
        when(giangvienphanbienRepository.existsByDangkydetaiidAndGiangvienid(dangkydetaiId, giangvienId)).thenReturn(false);
        when(giangvienphanbienRepository.save(any(Giangvienphanbien.class))).thenAnswer(i -> i.getArgument(0));

        Giangvienphanbien result = nghiepvuDetaiService.phanGiangvienPhanbien(dangkydetaiId, giangvienId);

        assertNotNull(result);
        assertEquals(dangkydetaiId, result.getDangkydetaiid());
        assertEquals(giangvienId, result.getGiangvienid());
        verify(giangvienphanbienRepository).save(any(Giangvienphanbien.class));
    }

    @Test
    @DisplayName("FAIL: Chặn phân GVPB khi giảng viên chính là GVHD của đề tài")
    void testPhanGiangvienPhanbien_ThatBai_KhiLaGVHD() {
        Long dangkydetaiId = 1L;
        Long detaiId = 10L;
        Long giangvienId = 2L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dangkydetaiId);
        dk.setDetaiid(detaiId);

        when(dangkydetaiRepository.findById(dangkydetaiId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(detaiId, giangvienId)).thenReturn(true);

        NghiepvuException exception = assertThrows(NghiepvuException.class, () -> {
            nghiepvuDetaiService.phanGiangvienPhanbien(dangkydetaiId, giangvienId);
        });

        assertEquals("Giang vien huong dan khong duoc lam giang vien phan bien cho de tai nay", exception.getMessage());
        verify(giangvienphanbienRepository, never()).save(any());
    }

    @Test
    @DisplayName("FAIL: Chặn phân công khi giảng viên đã được phân biện cho đề tài này rồi")
    void testPhanGiangvienPhanbien_ThatBai_KhiDaPhanBienRoi() {
        Long dangkydetaiId = 1L;
        Long detaiId = 10L;
        Long giangvienId = 5L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dangkydetaiId);
        dk.setDetaiid(detaiId);

        when(dangkydetaiRepository.findById(dangkydetaiId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(detaiId, giangvienId)).thenReturn(false);
        when(giangvienphanbienRepository.existsByDangkydetaiidAndGiangvienid(dangkydetaiId, giangvienId)).thenReturn(true);

        NghiepvuException exception = assertThrows(NghiepvuException.class, () -> {
            nghiepvuDetaiService.phanGiangvienPhanbien(dangkydetaiId, giangvienId);
        });

        assertEquals("Giang vien nay da duoc phan bien cho de tai nay roi", exception.getMessage());
        verify(giangvienphanbienRepository, never()).save(any());
    }

    @Test
    @DisplayName("PASS: Chấm điểm thành công khi giảng viên chấm không phải là GVHD")
    void testChamDiem_ThanhCong() {
        Long dangkydetaiId = 1L;
        Long detaiId = 10L;
        Long giangvienId = 6L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dangkydetaiId);
        dk.setDetaiid(detaiId);

        when(dangkydetaiRepository.findById(dangkydetaiId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(detaiId, giangvienId)).thenReturn(false);
        when(diemRepository.save(any(Diem.class))).thenAnswer(i -> i.getArgument(0));

        Diem diem = nghiepvuDetaiService.chamDiem(dangkydetaiId, giangvienId, new BigDecimal("8.5"), "Đề tài tốt");

        assertNotNull(diem);
        assertEquals(new BigDecimal("8.5"), diem.getDiemso());
        verify(diemRepository).save(any(Diem.class));
    }

    @Test
    @DisplayName("FAIL: Chặn chấm điểm khi GV cố tình chấm đề tài do mình hướng dẫn")
    void testChamDiem_ThatBai_KhiLaGVHD() {
        Long dangkydetaiId = 1L;
        Long detaiId = 10L;
        Long giangvienId = 2L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dangkydetaiId);
        dk.setDetaiid(detaiId);

        when(dangkydetaiRepository.findById(dangkydetaiId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(detaiId, giangvienId)).thenReturn(true);

        NghiepvuException exception = assertThrows(NghiepvuException.class, () -> {
            nghiepvuDetaiService.chamDiem(dangkydetaiId, giangvienId, new BigDecimal("9.0"), "Tự chấm");
        });

        assertEquals("Giang vien khong the cham diem de tai do minh huong dan", exception.getMessage());
        verify(diemRepository, never()).save(any());
    }
}
