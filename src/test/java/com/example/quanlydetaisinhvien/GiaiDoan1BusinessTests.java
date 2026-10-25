package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.GiaiDoan1.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.GiaiDoan1Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiaiDoan1BusinessTests {
    @Mock DotdangkyRepository dots;
    @Mock DetaiRepository detais;
    @Mock GiangvienRepository gvs;
    @Mock GiangvienhuongdanRepository guides;
    @Mock BomonRepository bomons;
    @Mock TaikhoanRepository users;
    @InjectMocks GiaiDoan1Service service;

    private Taikhoan khoa(){
        Taikhoan u=new Taikhoan();u.setId(1L);u.setVaitro(Vaitro.TRUONGKHOA);u.setTendangnhap("dean");return u;
    }
    private Dotdangky round(TrangthaiDotdangky state){
        Dotdangky d=new Dotdangky();d.setId(100L);d.setTrangthai(state);
        d.setBatdaugiangvien(LocalDateTime.now().minusDays(1));
        d.setKetthucgiangvien(LocalDateTime.now().plusDays(1));
        d.setBatdausinhvien(LocalDateTime.now().plusDays(2));
        d.setKetthucsinhvien(LocalDateTime.now().plusDays(5));return d;
    }
    @Test void invalidRoundDatesRejected(){
        when(users.findByTendangnhap("dean")).thenReturn(Optional.of(khoa()));
        DotForm form=new DotForm();form.setTendot("NCKH");form.setLoaidot(Loaidot.NCKH);
        form.setBatdaugiangvien(LocalDateTime.now().plusDays(2));
        form.setKetthucgiangvien(LocalDateTime.now().plusDays(1));
        form.setBatdausinhvien(LocalDateTime.now().plusDays(3));
        form.setKetthucsinhvien(LocalDateTime.now().plusDays(4));
        assertThrows(NghiepvuException.class,()->service.saveDot(null,form,"dean"));
        verify(dots,never()).save(any());
    }
    @Test void giaiDoan2BeforeStudentStartRejected(){
        when(users.findByTendangnhap("dean")).thenReturn(Optional.of(khoa()));
        when(dots.findByIdForUpdate(100L)).thenReturn(Optional.of(round(TrangthaiDotdangky.CONGBO)));
        assertThrows(NghiepvuException.class,()->service.chuyenGiaiDoan(100L,TrangthaiDotdangky.GIAIDOAN2,"dean"));
    }
    @Test void cannotEndFirstPhaseWithoutPublishedTopic(){
        when(users.findByTendangnhap("dean")).thenReturn(Optional.of(khoa()));
        when(dots.findByIdForUpdate(100L)).thenReturn(Optional.of(round(TrangthaiDotdangky.GIAIDOAN1)));
        
        assertThrows(NghiepvuException.class,()->service.chuyenGiaiDoan(100L,TrangthaiDotdangky.CONGBO,"dean"));
    }
    @Test void cannotProposeWhenNotInStageOne(){
        DeTaiForm f=new DeTaiForm();f.setDotdangkyid(100L);f.setTendetai("Đề tài mới");
        when(dots.findByIdForUpdate(100L)).thenReturn(Optional.of(round(TrangthaiDotdangky.GIAIDOAN2)));
        assertThrows(NghiepvuException.class,()->service.saveDetai(null,f,"gv001"));
        verify(detais,never()).save(any());
    }
    @Test void cannotPublishWithoutGuide(){
        Detai e=new Detai();e.setId(200L);e.setDotdangkyid(100L);e.setTrangthai(TrangthaiDetai.DADUYET);
        when(detais.findById(200L)).thenReturn(Optional.of(e));
        when(users.findByTendangnhap("dean")).thenReturn(Optional.of(khoa()));
        when(dots.findByIdForUpdate(100L)).thenReturn(Optional.of(round(TrangthaiDotdangky.GIAIDOAN1)));
        when(guides.countByDetaiid(200L)).thenReturn(0L);
        assertThrows(NghiepvuException.class,()->service.congBo(200L,"dean"));
    }
}
