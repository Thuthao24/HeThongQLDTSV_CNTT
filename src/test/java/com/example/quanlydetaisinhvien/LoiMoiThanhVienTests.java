package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.LoiMoiThanhVienDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.NhomsinhvienService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = QuanlydetaisinhvienApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:loimoi-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@Transactional
class LoiMoiThanhVienTests {

    @Autowired private EntityManager em;
    @Autowired private NhomsinhvienService service;
    @Autowired private ThanhviennhomRepository thanhviennhomRepository;
    @Autowired private LoimoithanhvienRepository loimoithanhvienRepository;

    private Bomon bm;
    private Dotdangky dot;
    private Sinhvien svLeader;
    private Sinhvien svCandidate;
    private Nhomsinhvien nhom;

    @BeforeEach
    void setUp() {
        bm = new Bomon();
        bm.setMabomon("CNPM_TEST");
        bm.setTenbomon("Cong nghe phan mem");
        em.persist(bm);

        dot = new Dotdangky();
        dot.setTendot("Dot KLTN 2026");
        dot.setLoaidot(Loaidot.KLTN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
        dot.setKetthucgiangvien(LocalDateTime.now().plusDays(10));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(5));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        em.persist(dot);

        // Leader
        Taikhoan tkLeader = new Taikhoan();
        tkLeader.setTendangnhap("sv_lead");
        tkLeader.setMatkhau("123456");
        tkLeader.setHoten("Sinh Vien Truong Nhom");
        tkLeader.setVaitro(Vaitro.SINHVIEN);
        tkLeader.setKichhoat(true);
        em.persist(tkLeader);

        svLeader = new Sinhvien();
        svLeader.setTaikhoanid(tkLeader.getId());
        svLeader.setMasinhvien("sv_lead");
        svLeader.setBomonid(bm.getId());
        em.persist(svLeader);

        // Candidate
        Taikhoan tkCandidate = new Taikhoan();
        tkCandidate.setTendangnhap("sv_candidate");
        tkCandidate.setMatkhau("123456");
        tkCandidate.setHoten("Sinh Vien Ung Vien");
        tkCandidate.setVaitro(Vaitro.SINHVIEN);
        tkCandidate.setKichhoat(true);
        em.persist(tkCandidate);

        svCandidate = new Sinhvien();
        svCandidate.setTaikhoanid(tkCandidate.getId());
        svCandidate.setMasinhvien("sv_candidate");
        svCandidate.setBomonid(bm.getId());
        em.persist(svCandidate);

        // Nhóm
        nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhom Test Loi Moi");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(svLeader.getId());
        em.persist(nhom);

        Thanhviennhom tvLeader = new Thanhviennhom(nhom.getId(), svLeader.getId());
        em.persist(tvLeader);

        em.flush();
    }

    @Test
    void testLeaderInviteStudentRequiresConfirmation() {
        // Nhóm trưởng thêm sinh viên sv_candidate vào nhóm
        service.themThanhVien(nhom.getId(), "sv_candidate", "sv_lead");

        // Sinh viên CHƯA được thêm vào thanhviennhom
        boolean daVaoNhom = thanhviennhomRepository.existsByNhomidAndSinhvienid(nhom.getId(), svCandidate.getId());
        assertFalse(daVaoNhom, "Sinh viên không được thêm trực tiếp vào nhóm khi chưa đồng ý!");

        // Lời mời được tạo ở trạng thái CHO_XAC_NHAN
        List<LoiMoiThanhVienDto> dsLoiMoi = service.layDanhSachLoiMoiCuaSinhVien("sv_candidate");
        assertEquals(1, dsLoiMoi.size());
        assertEquals(TrangthaiLoimoi.CHO_XAC_NHAN, dsLoiMoi.get(0).getTrangthai());
        assertEquals("Nhom Test Loi Moi", dsLoiMoi.get(0).getTennhom());

        // Sinh viên chấp nhận lời mời (Đồng ý)
        service.dongYLoiMoi(dsLoiMoi.get(0).getId(), "sv_candidate");

        // Sau khi đồng ý, sinh viên chính thức có trong thanhviennhom
        daVaoNhom = thanhviennhomRepository.existsByNhomidAndSinhvienid(nhom.getId(), svCandidate.getId());
        assertTrue(daVaoNhom, "Sinh viên phải được thêm vào nhóm sau khi đồng ý!");

        // Danh sách lời mời đang chờ bây giờ rỗng
        dsLoiMoi = service.layDanhSachLoiMoiCuaSinhVien("sv_candidate");
        assertTrue(dsLoiMoi.isEmpty());
    }

    @Test
    void testStudentCanRejectInvitation() {
        // Nhóm trưởng mời sinh viên
        service.themThanhVien(nhom.getId(), "sv_candidate", "sv_lead");

        List<LoiMoiThanhVienDto> dsLoiMoi = service.layDanhSachLoiMoiCuaSinhVien("sv_candidate");
        assertEquals(1, dsLoiMoi.size());

        // Sinh viên bấm Từ chối
        service.tuChoiLoiMoi(dsLoiMoi.get(0).getId(), "sv_candidate");

        // Sinh viên không vào nhóm
        boolean daVaoNhom = thanhviennhomRepository.existsByNhomidAndSinhvienid(nhom.getId(), svCandidate.getId());
        assertFalse(daVaoNhom);

        // Lời mời không còn trong danh sách chờ
        dsLoiMoi = service.layDanhSachLoiMoiCuaSinhVien("sv_candidate");
        assertTrue(dsLoiMoi.isEmpty());
    }

    @Test
    void testLeaderCanCancelInvitation() {
        // Nhóm trưởng mời sinh viên
        service.themThanhVien(nhom.getId(), "sv_candidate", "sv_lead");

        List<Loimoithanhvien> lmList = loimoithanhvienRepository.findByNhomidAndTrangthai(nhom.getId(), TrangthaiLoimoi.CHO_XAC_NHAN);
        assertEquals(1, lmList.size());

        // Nhóm trưởng hủy lời mời
        service.huyLoiMoi(nhom.getId(), lmList.get(0).getId(), "sv_lead");

        // Lời mời bị hủy
        List<LoiMoiThanhVienDto> dsLoiMoi = service.layDanhSachLoiMoiCuaSinhVien("sv_candidate");
        assertTrue(dsLoiMoi.isEmpty());
    }
}
