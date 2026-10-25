package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Dangkydetai;
import com.example.quanlydetaisinhvien.entity.Diem;
import com.example.quanlydetaisinhvien.entity.Ketqua;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChamDiemVaKetQuaTest {

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

    // --- TEST PASS: Nhập điểm 0-10 hợp lệ ---
    @Test
    @DisplayName("PASS: Nhập điểm hợp lệ 8.5 trong khoảng 0-10 kèm nhận xét")
    void testChamDiem_ThanhCong_TrongKhoang0Den10() {
        Long dkId = 1L;
        Long gvId = 5L;
        BigDecimal diem = new BigDecimal("8.5");
        String nhanxet = "Nội dung báo cáo tốt, trả lời lưu loát";

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dkId);
        dk.setDetaiid(10L);

        when(dangkydetaiRepository.findById(dkId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(10L, gvId)).thenReturn(false);
        when(diemRepository.findByDangkydetaiidAndGiangvienid(dkId, gvId)).thenReturn(Optional.empty());
        when(diemRepository.save(any(Diem.class))).thenAnswer(i -> i.getArgument(0));

        Diem result = nghiepvuDetaiService.chamDiem(dkId, gvId, diem, nhanxet);

        assertNotNull(result);
        assertEquals(diem, result.getDiemso());
        assertEquals(nhanxet, result.getNhanxet());
        verify(diemRepository).save(any(Diem.class));
    }

    @Test
    @DisplayName("PASS: Nhập điểm biên 0.00 thành công")
    void testChamDiem_ThanhCong_DiemBang0() {
        Long dkId = 1L;
        Long gvId = 5L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dkId);
        dk.setDetaiid(10L);

        when(dangkydetaiRepository.findById(dkId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(10L, gvId)).thenReturn(false);
        when(diemRepository.findByDangkydetaiidAndGiangvienid(dkId, gvId)).thenReturn(Optional.empty());
        when(diemRepository.save(any(Diem.class))).thenAnswer(i -> i.getArgument(0));

        Diem result = nghiepvuDetaiService.chamDiem(dkId, gvId, BigDecimal.ZERO, "Không tham gia");
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getDiemso());
    }

    @Test
    @DisplayName("PASS: Nhập điểm biên 10.00 thành công")
    void testChamDiem_ThanhCong_DiemBang10() {
        Long dkId = 1L;
        Long gvId = 5L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dkId);
        dk.setDetaiid(10L);

        when(dangkydetaiRepository.findById(dkId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(10L, gvId)).thenReturn(false);
        when(diemRepository.findByDangkydetaiidAndGiangvienid(dkId, gvId)).thenReturn(Optional.empty());
        when(diemRepository.save(any(Diem.class))).thenAnswer(i -> i.getArgument(0));

        Diem result = nghiepvuDetaiService.chamDiem(dkId, gvId, new BigDecimal("10.0"), "Xuất sắc");
        assertNotNull(result);
        assertEquals(new BigDecimal("10.0"), result.getDiemso());
    }

    // --- TEST FAIL: Điểm ngoài 0-10 bị từ chối ---
    @Test
    @DisplayName("FAIL: Điểm âm (< 0) bị từ chối")
    void testChamDiem_ThatBai_DiemAm() {
        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> {
            nghiepvuDetaiService.chamDiem(1L, 5L, new BigDecimal("-1.5"), "Sai");
        });
        assertEquals("Điểm phải nằm trong thang điểm từ 0 đến 10", ex.getMessage());
        verify(diemRepository, never()).save(any());
    }

    @Test
    @DisplayName("FAIL: Điểm lớn hơn 10 (> 10) bị từ chối")
    void testChamDiem_ThatBai_DiemLonHon10() {
        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> {
            nghiepvuDetaiService.chamDiem(1L, 5L, new BigDecimal("10.5"), "Vượt khung");
        });
        assertEquals("Điểm phải nằm trong thang điểm từ 0 đến 10", ex.getMessage());
        verify(diemRepository, never()).save(any());
    }

    @Test
    @DisplayName("FAIL: Chặn giảng viên hướng dẫn chấm đề tài mình hướng dẫn")
    void testChamDiem_ThatBai_GVHDCham() {
        Long dkId = 1L;
        Long gvId = 2L;

        Dangkydetai dk = new Dangkydetai();
        dk.setId(dkId);
        dk.setDetaiid(10L);

        when(dangkydetaiRepository.findById(dkId)).thenReturn(Optional.of(dk));
        when(giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(10L, gvId)).thenReturn(true);

        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> {
            nghiepvuDetaiService.chamDiem(dkId, gvId, new BigDecimal("8.0"), "Nhận xét");
        });
        assertEquals("Giang vien khong the cham diem de tai do minh huong dan", ex.getMessage());
        verify(diemRepository, never()).save(any());
    }

    // --- TEST PASS: Tính trung bình cộng khớp dữ liệu thành phần ---
    @Test
    @DisplayName("PASS: Tính trung bình cộng khớp dữ liệu các điểm thành phần (7.0, 8.0, 9.0 -> 8.00)")
    void testTinhVaLuuKetqua_ThanhCong_KhopDiemTrungBinh() {
        Long dkId = 1L;

        List<Diem> ds = List.of(
                createDiem(dkId, 1L, new BigDecimal("7.0")),
                createDiem(dkId, 2L, new BigDecimal("8.0")),
                createDiem(dkId, 3L, new BigDecimal("9.0"))
        );

        when(diemRepository.findByDangkydetaiid(dkId)).thenReturn(ds);
        when(ketquaRepository.findByDangkydetaiid(dkId)).thenReturn(Optional.empty());
        when(ketquaRepository.save(any(Ketqua.class))).thenAnswer(i -> i.getArgument(0));

        Ketqua kq = nghiepvuDetaiService.tinhVaLuuKetqua(dkId);

        assertNotNull(kq);
        assertEquals(new BigDecimal("8.00"), kq.getDiemcuoi());
        assertEquals(dkId, kq.getDangkydetaiid());
        assertFalse(kq.getDacongbo());
        verify(ketquaRepository).save(any(Ketqua.class));
    }

    @Test
    @DisplayName("PASS: Tính trung bình cộng làm tròn 2 chữ số thập phân (7.5, 8.0, 9.2 -> 8.23)")
    void testTinhVaLuuKetqua_ThanhCong_LamTron2ChuSo() {
        Long dkId = 1L;

        List<Diem> ds = List.of(
                createDiem(dkId, 1L, new BigDecimal("7.5")),
                createDiem(dkId, 2L, new BigDecimal("8.0")),
                createDiem(dkId, 3L, new BigDecimal("9.2"))
        );

        when(diemRepository.findByDangkydetaiid(dkId)).thenReturn(ds);
        when(ketquaRepository.findByDangkydetaiid(dkId)).thenReturn(Optional.empty());
        when(ketquaRepository.save(any(Ketqua.class))).thenAnswer(i -> i.getArgument(0));

        Ketqua kq = nghiepvuDetaiService.tinhVaLuuKetqua(dkId);

        assertNotNull(kq);
        assertEquals(new BigDecimal("8.23"), kq.getDiemcuoi());
    }

    // --- TEST FAIL: Chưa có điểm thành phần nào ---
    @Test
    @DisplayName("FAIL: Chặn tính kết quả khi chưa có điểm thành phần nào")
    void testTinhVaLuuKetqua_ThatBai_ChuaCoDiem() {
        Long dkId = 1L;
        when(diemRepository.findByDangkydetaiid(dkId)).thenReturn(Collections.emptyList());

        NghiepvuException ex = assertThrows(NghiepvuException.class, () -> {
            nghiepvuDetaiService.tinhVaLuuKetqua(dkId);
        });
        assertEquals("Chưa có điểm thành phần để tính kết quả", ex.getMessage());
    }

    // --- TEST PASS: Query / Logic đối chiếu khớp dữ liệu thành phần ---
    @Test
    @DisplayName("PASS: Đối chiếu kết quả xác nhận Diemcuoi khớp 100% với trung bình cộng thành phần")
    void testDoiChieuKetQua_ThanhCong_KhopDuLieu() {
        Long dkId = 1L;

        Ketqua kq = new Ketqua();
        kq.setDangkydetaiid(dkId);
        kq.setDiemcuoi(new BigDecimal("8.00"));

        List<Diem> ds = List.of(
                createDiem(dkId, 1L, new BigDecimal("7.0")),
                createDiem(dkId, 2L, new BigDecimal("8.0")),
                createDiem(dkId, 3L, new BigDecimal("9.0"))
        );

        when(ketquaRepository.findByDangkydetaiid(dkId)).thenReturn(Optional.of(kq));
        when(diemRepository.findByDangkydetaiid(dkId)).thenReturn(ds);

        boolean khop = nghiepvuDetaiService.doiChieuKetQua(dkId);
        assertTrue(khop, "Điểm cuối phải khớp chính xác với trung bình cộng các điểm thành phần");
    }

    @Test
    @DisplayName("FAIL: Đối chiếu kết quả phát hiện Diemcuoi bị lệch so với dữ liệu thành phần")
    void testDoiChieuKetQua_ThatBai_LechDuLieu() {
        Long dkId = 1L;

        Ketqua kq = new Ketqua();
        kq.setDangkydetaiid(dkId);
        kq.setDiemcuoi(new BigDecimal("9.50")); // Lệch với trung bình thật 8.00

        List<Diem> ds = List.of(
                createDiem(dkId, 1L, new BigDecimal("7.0")),
                createDiem(dkId, 2L, new BigDecimal("8.0")),
                createDiem(dkId, 3L, new BigDecimal("9.0"))
        );

        when(ketquaRepository.findByDangkydetaiid(dkId)).thenReturn(Optional.of(kq));
        when(diemRepository.findByDangkydetaiid(dkId)).thenReturn(ds);

        boolean khop = nghiepvuDetaiService.doiChieuKetQua(dkId);
        assertFalse(khop, "Phải phát hiện kết quả bị lệch dữ liệu");
    }

    private Diem createDiem(Long dkId, Long gvId, BigDecimal score) {
        Diem d = new Diem();
        d.setDangkydetaiid(dkId);
        d.setGiangvienid(gvId);
        d.setDiemso(score);
        return d;
    }
}
