
package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.HoidongFormDto;
import com.example.quanlydetaisinhvien.dto.ThanhvienHoidongItemDto;
import com.example.quanlydetaisinhvien.entity.Hoidong;
import com.example.quanlydetaisinhvien.entity.Thanhvienhoidong;
import com.example.quanlydetaisinhvien.entity.VaitroHoidong;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.impl.HoidongServiceImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    // =========================================
    // 1. TEST HỘI ĐỒNG 3 GIẢNG VIÊN
    // =========================================

    @Test
    @DisplayName("PASS: Hội đồng 3 GV hợp lệ")
    void testKiemtraHoidongHopLe_ThanhCong_3GV() {

        mockExistingLecturers();

        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.THUKY),
                createMember(3L, VaitroHoidong.THANHVIEN)
        );

        assertDoesNotThrow(
                () -> hoidongService.kiemtraHoidongHopLe(members)
        );
    }

    // =========================================
    // 2. TEST HỘI ĐỒNG 5 GIẢNG VIÊN
    // =========================================

    @Test
    @DisplayName("PASS: Hội đồng 5 GV hợp lệ")
    void testKiemtraHoidongHopLe_ThanhCong_5GV() {

        mockExistingLecturers();

        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.THUKY),
                createMember(3L, VaitroHoidong.THANHVIEN),
                createMember(4L, VaitroHoidong.THANHVIEN),
                createMember(5L, VaitroHoidong.THANHVIEN)
        );

        assertDoesNotThrow(
                () -> hoidongService.kiemtraHoidongHopLe(members)
        );
    }

    // =========================================
    // 3. TEST DƯỚI 3 GIẢNG VIÊN
    // =========================================

    @Test
    @DisplayName("FAIL: Chặn hội đồng dưới 3 GV")
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

    // =========================================
    // 4. TEST TRÊN 5 GIẢNG VIÊN
    // =========================================

    @Test
    @DisplayName("FAIL: Chặn hội đồng vượt quá 5 GV")
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

    // =========================================
    // 5. TEST KHÔNG CÓ CHỦ TỊCH
    // =========================================

    @Test
    @DisplayName("FAIL: Chặn hội đồng không có Chủ tịch")
    void testKiemtraHoidongHopLe_ThatBai_KhongCoChuTich() {

        mockExistingLecturers();

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

    // =========================================
    // 6. TEST HAI CHỦ TỊCH
    // =========================================

    @Test
    @DisplayName("FAIL: Chặn hội đồng có 2 Chủ tịch")
    void testKiemtraHoidongHopLe_ThatBai_HaiChuTich() {

        mockExistingLecturers();

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

    // =========================================
    // 7. TEST KHÔNG CÓ THƯ KÝ
    // =========================================

    @Test
    @DisplayName("FAIL: Chặn hội đồng không có Thư ký")
    void testKiemtraHoidongHopLe_ThatBai_KhongCoThuKy() {

        mockExistingLecturers();

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

    // =========================================
    // 8. TEST TRÙNG GIẢNG VIÊN
    // =========================================

    @Test
    @DisplayName("FAIL: Chặn giảng viên trùng nhau")
    void testKiemtraHoidongHopLe_ThatBai_TrungGiangVien() {

        mockExistingLecturers();

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

    // =========================================
    // 9. TEST LƯU HỘI ĐỒNG THÀNH CÔNG
    // =========================================

    @Test
    @DisplayName("PASS: Thêm mới hội đồng thành công")
    void testLuuHoidong_ThanhCong() {

        mockExistingLecturers();

        when(dotdangkyRepository.existsById(1L))
                .thenReturn(true);

        HoidongFormDto form = new HoidongFormDto();

        form.setTenhoidong("Hội đồng Đánh giá KLTN 01");
        form.setDotdangkyid(1L);
        form.setPhong("A201");
        form.setNgaybaocao(
                LocalDateTime.now().plusDays(5)
        );

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

    // =========================================
    // 10. TEST XÓA HỘI ĐỒNG
    // =========================================

    @Test
    @DisplayName("PASS: Xóa hội đồng thành công")
    void testXoaHoidong_ThanhCong() {

        Long hoidongId = 100L;

        when(hoidongRepository.existsById(hoidongId))
                .thenReturn(true);

        assertDoesNotThrow(
                () -> hoidongService.xoaHoidong(hoidongId)
        );

        verify(thanhvienhoidongRepository)
                .deleteByHoidongid(hoidongId);

        verify(hoidongRepository)
                .deleteById(hoidongId);
    }

    // =========================================
    // 11. TEST GIẢNG VIÊN KHÔNG TỒN TẠI
    // =========================================

    @Test
    @DisplayName("FAIL: Chặn giảng viên không tồn tại")
    void testKiemtraHoidongHopLe_GiangvienKhongTonTai() {

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

    // =========================================
    // HÀM HỖ TRỢ TEST
    // =========================================

    private void mockExistingLecturers() {
        when(giangvienRepository.existsById(anyLong()))
                .thenReturn(true);
    }

    private Thanhvienhoidong createMember(
            Long giangvienId,
            VaitroHoidong role) {

        Thanhvienhoidong tv = new Thanhvienhoidong();

        tv.setGiangvienid(giangvienId);
        tv.setVaitro(role);

        return tv;
    }
}
