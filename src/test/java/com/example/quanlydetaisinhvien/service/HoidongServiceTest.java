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

    // --- TEST PASS: Hội đồng 3 GV hợp lệ ---
    @Test
    @DisplayName("PASS: Hội đồng 3 GV hợp lệ (1 Chủ tịch, 1 Thư ký, 1 Thành viên)")
    void testKiemtraHoidongHopLe_ThanhCong_3GV() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.THUKY),
                createMember(3L, VaitroHoidong.THANHVIEN)
        );

        assertDoesNotThrow(() -> hoidongService.kiemtraHoidongHopLe(members));
    }

    // --- TEST PASS: Hội đồng 5 GV hợp lệ ---
    @Test
    @DisplayName("PASS: Hội đồng 5 GV hợp lệ (1 Chủ tịch, 1 Thư ký, 3 Thành viên)")
    void testKiemtraHoidongHopLe_ThanhCong_5GV() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.THUKY),
                createMember(3L, VaitroHoidong.THANHVIEN),
                createMember(4L, VaitroHoidong.THANHVIEN),
                createMember(5L, VaitroHoidong.THANHVIEN)
        );

        assertDoesNotThrow(() -> hoidongService.kiemtraHoidongHopLe(members));
    }

    // --- TEST FAIL: Dưới 3 GV (2 GV) ---
    @Test
    @DisplayName("FAIL: Chặn hội đồng dưới 3 GV (chỉ có 2 GV)")
    void testKiemtraHoidongHopLe_ThatBai_Duoi3GV() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.THUKY)
        );

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> hoidongService.kiemtraHoidongHopLe(members));
        assertEquals("Hội đồng phải có từ 3 đến 5 giảng viên", ex.getMessage());
    }

    // --- TEST FAIL: Quá 5 GV (6 GV) ---
    @Test
    @DisplayName("FAIL: Chặn hội đồng vượt quá 5 GV (có 6 GV)")
    void testKiemtraHoidongHopLe_ThatBai_Tren5GV() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.THUKY),
                createMember(3L, VaitroHoidong.THANHVIEN),
                createMember(4L, VaitroHoidong.THANHVIEN),
                createMember(5L, VaitroHoidong.THANHVIEN),
                createMember(6L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> hoidongService.kiemtraHoidongHopLe(members));
        assertEquals("Hội đồng phải có từ 3 đến 5 giảng viên", ex.getMessage());
    }

    // --- TEST FAIL: Không có Chủ tịch ---
    @Test
    @DisplayName("FAIL: Chặn hội đồng không có Chủ tịch")
    void testKiemtraHoidongHopLe_ThatBai_KhongCoChuTich() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.THANHVIEN),
                createMember(2L, VaitroHoidong.THUKY),
                createMember(3L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> hoidongService.kiemtraHoidongHopLe(members));
        assertEquals("Hội đồng phải có đúng 1 Chủ tịch", ex.getMessage());
    }

    // --- TEST FAIL: Có 2 Chủ tịch ---
    @Test
    @DisplayName("FAIL: Chặn hội đồng có 2 Chủ tịch")
    void testKiemtraHoidongHopLe_ThatBai_HaiChuTich() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.CHUTICH),
                createMember(3L, VaitroHoidong.THUKY)
        );

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> hoidongService.kiemtraHoidongHopLe(members));
        assertEquals("Hội đồng phải có đúng 1 Chủ tịch", ex.getMessage());
    }

    // --- TEST FAIL: Không có Thư ký ---
    @Test
    @DisplayName("FAIL: Chặn hội đồng không có Thư ký")
    void testKiemtraHoidongHopLe_ThatBai_KhongCoThuKy() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(2L, VaitroHoidong.THANHVIEN),
                createMember(3L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> hoidongService.kiemtraHoidongHopLe(members));
        assertEquals("Hội đồng phải có đúng 1 Thư ký", ex.getMessage());
    }

    // --- TEST FAIL: Trùng giảng viên trong cùng hội đồng ---
    @Test
    @DisplayName("FAIL: Chặn trùng lặp giảng viên trong cùng một hội đồng")
    void testKiemtraHoidongHopLe_ThatBai_TrungGiangVien() {
        List<Thanhvienhoidong> members = List.of(
                createMember(1L, VaitroHoidong.CHUTICH),
                createMember(1L, VaitroHoidong.THUKY), // Trùng GV 1
                createMember(2L, VaitroHoidong.THANHVIEN)
        );

        NghiepvuException ex = assertThrows(NghiepvuException.class,
                () -> hoidongService.kiemtraHoidongHopLe(members));
        assertEquals("Các giảng viên trong cùng một hội đồng không được trùng nhau", ex.getMessage());
    }

    // --- TEST CRUD: Lưu hội đồng hợp lệ thành công ---
    @Test
    @DisplayName("PASS: Thêm mới hội đồng thành công và lưu danh sách thành viên")
    void testLuuHoidong_ThanhCong() {
        HoidongFormDto form = new HoidongFormDto();
        form.setTenhoidong("Hội đồng Đánh giá KLTN 01");
        form.setDotdangkyid(1L);
        form.setPhong("A201");
        form.setNgaybaocao(LocalDateTime.now().plusDays(5));

        List<ThanhvienHoidongItemDto> tvList = new ArrayList<>();
        tvList.add(new ThanhvienHoidongItemDto(1L, VaitroHoidong.CHUTICH));
        tvList.add(new ThanhvienHoidongItemDto(2L, VaitroHoidong.THUKY));
        tvList.add(new ThanhvienHoidongItemDto(3L, VaitroHoidong.THANHVIEN));
        form.setThanhviens(tvList);

        when(hoidongRepository.save(any(Hoidong.class))).thenAnswer(inv -> {
            Hoidong hd = inv.getArgument(0);
            hd.setId(100L);
            return hd;
        });

        Hoidong saved = hoidongService.luuHoidong(form);

        assertNotNull(saved);
        assertEquals(100L, saved.getId());
        assertEquals("Hội đồng Đánh giá KLTN 01", saved.getTenhoidong());
        verify(hoidongRepository).save(any(Hoidong.class));
        verify(thanhvienhoidongRepository, times(3)).save(any(Thanhvienhoidong.class));
    }

    // --- TEST CRUD: Xóa hội đồng thành công ---
    @Test
    @DisplayName("PASS: Xóa hội đồng và các thành viên hội đồng liên quan")
    void testXoaHoidong_ThanhCong() {
        Long hoidongId = 100L;

        assertDoesNotThrow(() -> hoidongService.xoaHoidong(hoidongId));

        verify(thanhvienhoidongRepository).deleteByHoidongid(hoidongId);
        verify(hoidongRepository).deleteById(hoidongId);
    }

    private Thanhvienhoidong createMember(Long giangvienId, VaitroHoidong role) {
        Thanhvienhoidong tv = new Thanhvienhoidong();
        tv.setGiangvienid(giangvienId);
        tv.setVaitro(role);
        return tv;
    }
}
