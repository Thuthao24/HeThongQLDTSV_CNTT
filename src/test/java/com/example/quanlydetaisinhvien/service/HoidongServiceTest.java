
package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.HoidongFormDto;
import com.example.quanlydetaisinhvien.dto.ThanhvienHoidongItemDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.impl.HoidongServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoidongServiceTest {

    @Mock
    private HoidongRepository hoidongRepository;

    @Mock
    private ThanhvienhoidongRepository thanhvienhoidongRepository;

    @Mock
    private DotdangkyRepository dotdangkyRepository;

    @Mock
    private GiangvienRepository giangvienRepository;

    @Mock
    private TaikhoanRepository taikhoanRepository;

    @Mock
    private PhanconghoidongRepository assignments;

    @Mock
    private KetquaRepository results;

    @InjectMocks
    private HoidongServiceImpl hoidongService;

    // Gia lap giang vien da ton tai trong CSDL.
    private void mockValidLecturers() {
        when(giangvienRepository.existsById(anyLong()))
            .thenReturn(true);
    }

    private Thanhvienhoidong createMember(
            Long giangvienId, VaitroHoidong role) {

        Thanhvienhoidong tv = new Thanhvienhoidong();
        tv.setGiangvienid(giangvienId);
        tv.setVaitro(role);
        return tv;
    }

    @Test
    @DisplayName("PASS: Hoi dong 3 giang vien hop le")
    void testKiemtraHoidongHopLe_ThanhCong_3GV() {
        mockValidLecturers();

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(2L, VaitroHoidong.THUKY),
            createMember(3L, VaitroHoidong.THANHVIEN)
        );

        assertDoesNotThrow(() ->
            hoidongService.kiemtraHoidongHopLe(members));
    }

    @Test
    @DisplayName("PASS: Hoi dong 5 giang vien hop le")
    void testKiemtraHoidongHopLe_ThanhCong_5GV() {
        mockValidLecturers();

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(2L, VaitroHoidong.THUKY),
            createMember(3L, VaitroHoidong.THANHVIEN),
            createMember(4L, VaitroHoidong.THANHVIEN),
            createMember(5L, VaitroHoidong.THANHVIEN)
        );

        assertDoesNotThrow(() ->
            hoidongService.kiemtraHoidongHopLe(members));
    }

    @Test
    @DisplayName("FAIL: Hoi dong duoi 3 giang vien")
    void testKiemtraHoidongHopLe_ThatBai_Duoi3GV() {

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(2L, VaitroHoidong.THUKY)
        );

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.kiemtraHoidongHopLe(members)
        );

        assertEquals(
            "Hội đồng phải có từ 3 đến 5 giảng viên",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("FAIL: Hoi dong tren 5 giang vien")
    void testKiemtraHoidongHopLe_ThatBai_Tren5GV() {

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(2L, VaitroHoidong.THUKY),
            createMember(3L, VaitroHoidong.THANHVIEN),
            createMember(4L, VaitroHoidong.THANHVIEN),
            createMember(5L, VaitroHoidong.THANHVIEN),
            createMember(6L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.kiemtraHoidongHopLe(members)
        );

        assertEquals(
            "Hội đồng phải có từ 3 đến 5 giảng viên",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("FAIL: Khong co chu tich")
    void testKiemtraHoidongHopLe_ThatBai_KhongCoChuTich() {
        mockValidLecturers();

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.THANHVIEN),
            createMember(2L, VaitroHoidong.THUKY),
            createMember(3L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.kiemtraHoidongHopLe(members)
        );

        assertEquals(
            "Hội đồng phải có đúng 1 Chủ tịch",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("FAIL: Hai chu tich")
    void testKiemtraHoidongHopLe_ThatBai_HaiChuTich() {
        mockValidLecturers();

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(2L, VaitroHoidong.CHUTICH),
            createMember(3L, VaitroHoidong.THUKY)
        );

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.kiemtraHoidongHopLe(members)
        );

        assertEquals(
            "Hội đồng phải có đúng 1 Chủ tịch",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("FAIL: Khong co thu ky")
    void testKiemtraHoidongHopLe_ThatBai_KhongCoThuKy() {
        mockValidLecturers();

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(2L, VaitroHoidong.THANHVIEN),
            createMember(3L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.kiemtraHoidongHopLe(members)
        );

        assertEquals(
            "Hội đồng phải có đúng 1 Thư ký",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("FAIL: Trung giang vien")
    void testKiemtraHoidongHopLe_ThatBai_TrungGiangVien() {
        mockValidLecturers();

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(1L, VaitroHoidong.THUKY),
            createMember(2L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.kiemtraHoidongHopLe(members)
        );

        assertEquals(
            "Các giảng viên trong hội đồng không được trùng nhau",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("PASS: Luu hoi dong thanh cong")
    void testLuuHoidong_ThanhCong() {
        mockValidLecturers();

        when(dotdangkyRepository.existsById(1L))
            .thenReturn(true);

        HoidongFormDto form = new HoidongFormDto();
        form.setTenhoidong("Hội đồng Đánh giá KLTN 01");
        form.setDotdangkyid(1L);
        form.setPhong("A201");
        form.setNgaybaocao(LocalDateTime.now().plusDays(5));

        List<ThanhvienHoidongItemDto> tvList =
            new ArrayList<>();

        tvList.add(new ThanhvienHoidongItemDto(
            1L, VaitroHoidong.CHUTICH));

        tvList.add(new ThanhvienHoidongItemDto(
            2L, VaitroHoidong.THUKY));

        tvList.add(new ThanhvienHoidongItemDto(
            3L, VaitroHoidong.THANHVIEN));

        form.setThanhviens(tvList);

        when(hoidongRepository.save(any(Hoidong.class)))
            .thenAnswer(inv -> {
                Hoidong hd = inv.getArgument(0);
                hd.setId(100L);
                return hd;
            });

        Hoidong saved = hoidongService.luuHoidong(form);

        assertNotNull(saved);
        assertEquals(100L, saved.getId());
        assertEquals(
            "Hội đồng Đánh giá KLTN 01",
            saved.getTenhoidong()
        );

        verify(hoidongRepository)
            .save(any(Hoidong.class));

        verify(thanhvienhoidongRepository, times(3))
            .save(any(Thanhvienhoidong.class));
    }

    @Test
    @DisplayName("PASS: Xoa hoi dong thanh cong")
    void testXoaHoidong_ThanhCong() {
        Long hoidongId = 100L;

        when(hoidongRepository.existsById(hoidongId))
            .thenReturn(true);

        when(assignments.findByHoidongid(hoidongId))
            .thenReturn(List.of());

        assertDoesNotThrow(() ->
            hoidongService.xoaHoidong(hoidongId));

        verify(thanhvienhoidongRepository)
            .deleteByHoidongid(hoidongId);

        verify(hoidongRepository)
            .deleteById(hoidongId);
    }

    // ===== KIEM THU BO SUNG =====

    @Test
    @DisplayName("FAIL: Giang vien khong ton tai")
    void testGiangVienKhongTonTai() {

        List<Thanhvienhoidong> members = List.of(
            createMember(1L, VaitroHoidong.CHUTICH),
            createMember(2L, VaitroHoidong.THUKY),
            createMember(3L, VaitroHoidong.THANHVIEN)
        );

        when(giangvienRepository.existsById(1L))
            .thenReturn(false);

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.kiemtraHoidongHopLe(members)
        );

        assertEquals(
            "Giảng viên không tồn tại: 1",
            ex.getMessage()
        );
    }

    @Test
    @DisplayName("FAIL: Xoa hoi dong khong ton tai")
    void testXoaHoidongKhongTonTai() {

        when(hoidongRepository.existsById(999L))
            .thenReturn(false);

        NghiepvuException ex = assertThrows(
            NghiepvuException.class,
            () -> hoidongService.xoaHoidong(999L)
        );

        assertEquals(
            "Hội đồng không tồn tại",
            ex.getMessage()
        );

        verify(hoidongRepository, never())
            .deleteById(anyLong());
    }
}
