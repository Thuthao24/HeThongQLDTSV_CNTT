package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.dto.TaoNhomRequest;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.DangkydetaiService;
import com.example.quanlydetaisinhvien.service.NhomsinhvienService;
import com.example.quanlydetaisinhvien.service.NopbaocaoService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = QuanlydetaisinhvienApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:thuyvan-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never", "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false", "spring.mail.test-connection=false"
})
@Transactional
class ThuyVanFixesTests {

    @Autowired private EntityManager em;
    @Autowired private NhomsinhvienService nhomService;
    @Autowired private DangkydetaiService dangkydetaiService;
    @Autowired private NopbaocaoService nopbaocaoService;

    @Autowired private ThanhviennhomRepository thanhviennhomRepository;
    @Autowired private DangkydetaiRepository dangkydetaiRepository;
    @Autowired private DiemRepository diemRepository;
    @Autowired private NopbaocaoRepository nopbaocaoRepository;

    private Bomon bm;
    private Dotdangky dot;
    private Dotdangky dot2;
    private Taikhoan tkTruongKhoa;
    private Taikhoan tkGv;
    private Giangvien gv;
    private Detai dt;

    @BeforeEach
    void setUp() {
        bm = new Bomon();
        bm.setMabomon("CNPM_TV");
        bm.setTenbomon("Cong nghe phan mem");
        em.persist(bm);

        dot = new Dotdangky();
        dot.setTendot("Đợt 1 - 2026");
        dot.setLoaidot(Loaidot.KLTN);
        dot.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
        dot.setKetthucgiangvien(LocalDateTime.now().minusDays(5));
        dot.setBatdausinhvien(LocalDateTime.now().minusDays(4));
        dot.setKetthucsinhvien(LocalDateTime.now().plusDays(5));
        dot.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        em.persist(dot);

        dot2 = new Dotdangky();
        dot2.setTendot("Đợt 2 - 2026");
        dot2.setLoaidot(Loaidot.NCKH);
        dot2.setBatdaugiangvien(LocalDateTime.now().minusDays(10));
        dot2.setKetthucgiangvien(LocalDateTime.now().minusDays(5));
        dot2.setBatdausinhvien(LocalDateTime.now().minusDays(4));
        dot2.setKetthucsinhvien(LocalDateTime.now().plusDays(10));
        dot2.setTrangthai(TrangthaiDotdangky.GIAIDOAN2);
        em.persist(dot2);

        tkTruongKhoa = new Taikhoan();
        tkTruongKhoa.setTendangnhap("truongkhoa_tv");
        tkTruongKhoa.setMatkhau("123");
        tkTruongKhoa.setHoten("Trưởng Khoa TV");
        tkTruongKhoa.setEmail("tk_tv@edu.vn");
        tkTruongKhoa.setVaitro(Vaitro.TRUONGKHOA);
        tkTruongKhoa.setKichhoat(true);
        em.persist(tkTruongKhoa);

        tkGv = new Taikhoan();
        tkGv.setTendangnhap("gv_tv");
        tkGv.setMatkhau("123");
        tkGv.setHoten("Giảng Viên TV");
        tkGv.setEmail("gv_tv@edu.vn");
        tkGv.setVaitro(Vaitro.GIANGVIEN);
        tkGv.setKichhoat(true);
        em.persist(tkGv);

        gv = new Giangvien();
        gv.setMagiangvien("GV_TV_01");
        gv.setTaikhoanid(tkGv.getId());
        gv.setBomonid(bm.getId());
        em.persist(gv);

        dt = new Detai();
        dt.setTendetai("Đề tài Test Quy Tắc Sĩ Số và Khóa Báo Cáo");
        dt.setDotdangkyid(dot.getId());
        dt.setBomonid(bm.getId());
        dt.setGiangviendexuatid(gv.getId());
        dt.setTrangthai(TrangthaiDetai.CONGBO);
        em.persist(dt);

        em.flush();
    }

    private Sinhvien taoSinhVien(String mssv) {
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap(mssv);
        tk.setMatkhau("123");
        tk.setHoten("Sinh Viên " + mssv);
        tk.setEmail(mssv + "@student.edu.vn");
        tk.setVaitro(Vaitro.SINHVIEN);
        tk.setKichhoat(true);
        em.persist(tk);

        Sinhvien sv = new Sinhvien();
        sv.setMasinhvien(mssv);
        sv.setBomonid(bm.getId());
        sv.setTaikhoanid(tk.getId());
        em.persist(sv);
        em.flush();
        return sv;
    }

    @Test
    @DisplayName("P0 Item 04: Duyệt đề tài thất bại nếu nhóm có dưới 3 thành viên")
    void testApprovalFailsWhenGroupHasLessThan3Members() {
        Sinhvien sv1 = taoSinhVien("SV_TV_01");
        Sinhvien sv2 = taoSinhVien("SV_TV_02");

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhóm 2 Thành Viên");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv1.getId());
        em.persist(nhom);

        em.persist(new Thanhviennhom(nhom.getId(), sv1.getId()));
        em.persist(new Thanhviennhom(nhom.getId(), sv2.getId()));

        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(dt.getId());
        dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dk.setNgaydangky(LocalDateTime.now());
        em.persist(dk);
        em.flush();

        NghiepvuException ex = assertThrows(NghiepvuException.class, () ->
                dangkydetaiService.duyetDangky(dk.getId(), true, "truongkhoa_tv")
        );
        assertTrue(ex.getMessage().contains("từ 3 đến 5 thành viên"));
    }

    @Test
    @DisplayName("P0 Item 04: Duyệt đề tài thành công khi nhóm có đủ 3 thành viên")
    void testApprovalSucceedsWhenGroupHas3Members() {
        Sinhvien sv1 = taoSinhVien("SV_TV_03");
        Sinhvien sv2 = taoSinhVien("SV_TV_04");
        Sinhvien sv3 = taoSinhVien("SV_TV_05");

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhóm 3 Thành Viên Hợp Lệ");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv1.getId());
        em.persist(nhom);

        em.persist(new Thanhviennhom(nhom.getId(), sv1.getId()));
        em.persist(new Thanhviennhom(nhom.getId(), sv2.getId()));
        em.persist(new Thanhviennhom(nhom.getId(), sv3.getId()));

        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(dt.getId());
        dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dk.setNgaydangky(LocalDateTime.now());
        em.persist(dk);
        em.flush();

        Dangkydetai approved = dangkydetaiService.duyetDangky(dk.getId(), true, "truongkhoa_tv");
        assertEquals(TrangthaiDangkydetai.DADUYET, approved.getTrangthai());
    }

    @Test
    @DisplayName("P0 Item 04: Chặn giảm sĩ số xuống dưới 3 thành viên khi đề tài đang chờ duyệt (CHODUYET)")
    void testCannotDropBelow3MembersWhenChoDuyet() {
        Sinhvien sv1 = taoSinhVien("SV_TV_06");
        Sinhvien sv2 = taoSinhVien("SV_TV_07");
        Sinhvien sv3 = taoSinhVien("SV_TV_08");

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhóm Chờ Duyệt 3 SV");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv1.getId());
        em.persist(nhom);

        em.persist(new Thanhviennhom(nhom.getId(), sv1.getId()));
        em.persist(new Thanhviennhom(nhom.getId(), sv2.getId()));
        em.persist(new Thanhviennhom(nhom.getId(), sv3.getId()));

        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(dt.getId());
        dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dk.setNgaydangky(LocalDateTime.now());
        em.persist(dk);
        em.flush();

        // 1. Nhóm trưởng xóa thành viên
        NghiepvuException ex1 = assertThrows(NghiepvuException.class, () ->
                nhomService.xoaThanhVien(nhom.getId(), sv2.getId(), sv1.getMasinhvien())
        );
        assertTrue(ex1.getMessage().contains("tối thiểu 3 thành viên"));

        // 2. Thành viên tự rời nhóm
        NghiepvuException ex2 = assertThrows(NghiepvuException.class, () ->
                nhomService.roiNhom(nhom.getId(), sv3.getMasinhvien())
        );
        assertTrue(ex2.getMessage().contains("tối thiểu 3 thành viên"));
    }

    @Test
    @DisplayName("P0 Item 05: Chặn 1 sinh viên tham gia nhóm khác ở các đợt đang mở")
    void testStudentCannotJoinMultipleGroupsInActiveRounds() {
        Sinhvien sv = taoSinhVien("SV_TV_09");

        // Tham gia nhóm 1 ở đợt 1 (đang mở)
        Nhomsinhvien nhom1 = new Nhomsinhvien();
        nhom1.setTennhom("Nhóm Đợt 1");
        nhom1.setDotdangkyid(dot.getId());
        nhom1.setNhomtruongid(sv.getId());
        em.persist(nhom1);
        em.persist(new Thanhviennhom(nhom1.getId(), sv.getId()));
        em.flush();

        // Tạo nhóm ở đợt 2 với chính sinh viên này
        TaoNhomRequest req = new TaoNhomRequest();
        req.setDotdangkyid(dot2.getId());
        req.setTennhom("Nhóm Đợt 2 Trùng SV");
        req.setMasinhvienNhomtruong(sv.getMasinhvien());

        NghiepvuException ex = assertThrows(NghiepvuException.class, () ->
                nhomService.taoNhom(req, sv.getMasinhvien())
        );
        assertTrue(ex.getMessage().contains("chưa kết thúc"));
    }

    @Test
    @DisplayName("P1 Item 21: Kiểm tra binary magic bytes, từ chối file giả mạo phần mở rộng")
    void testMagicBytesValidationRejectsFakeExtension() {
        byte[] fakePdfBytes = "Đây là văn bản giả mạo không có header %PDF".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "baocao_fake.pdf", "application/pdf", fakePdfBytes);

        Sinhvien sv = taoSinhVien("SV_TV_10");
        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhóm Nộp Test");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv.getId());
        em.persist(nhom);
        em.persist(new Thanhviennhom(nhom.getId(), sv.getId()));

        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(dt.getId());
        dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dk.setNgaydangky(LocalDateTime.now());
        em.persist(dk);
        em.flush();

        NghiepvuException ex = assertThrows(NghiepvuException.class, () ->
                nopbaocaoService.nopBaocaoVoiUpload(dk.getId(), file, sv.getMasinhvien())
        );
        assertTrue(ex.getMessage().contains("Nội dung tệp không khớp"));
    }

    @Test
    @DisplayName("Special Requirement: Khóa chức năng nộp, cập nhật hoặc xóa báo cáo khi nhóm đã có điểm")
    void testReportSubmissionLockedWhenGraded() {
        Sinhvien sv = taoSinhVien("SV_TV_11");
        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhóm Đã Có Điểm");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(sv.getId());
        em.persist(nhom);
        em.persist(new Thanhviennhom(nhom.getId(), sv.getId()));

        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(dt.getId());
        dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dk.setNgaydangky(LocalDateTime.now());
        em.persist(dk);

        // Tạo điểm cho nhóm
        Diem d = new Diem();
        d.setDangkydetaiid(dk.getId());
        d.setGiangvienid(gv.getId());
        d.setDiemso(new BigDecimal("8.5"));
        d.setNhanxet("Đạt yêu cầu");
        em.persist(d);

        // Tạo 1 báo cáo trước đó
        Nopbaocao nb = new Nopbaocao();
        nb.setDangkydetaiid(dk.getId());
        nb.setSinhvienid(sv.getId());
        nb.setDuongdanfile("/uploads/baocao/test.pdf");
        nb.setNgaynop(LocalDateTime.now().minusDays(1));
        em.persist(nb);
        em.flush();

        // 1. Kiểm tra kiemTraDaKhoaNopBaoCao
        assertTrue(nopbaocaoService.kiemTraDaKhoaNopBaoCao(dk.getId()));

        // 2. Nộp file mới -> Phải bị chặn
        byte[] validPdf = "%PDF-1.4 sample content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "baocao_moi.pdf", "application/pdf", validPdf);
        NghiepvuException exUpload = assertThrows(NghiepvuException.class, () ->
                nopbaocaoService.nopBaocaoVoiUpload(dk.getId(), file, sv.getMasinhvien())
        );
        assertTrue(exUpload.getMessage().contains("đã có điểm đánh giá"));

        // 3. Xóa báo cáo cũ -> Phải bị chặn
        NghiepvuException exDelete = assertThrows(NghiepvuException.class, () ->
                nopbaocaoService.xoaBaocao(nb.getId(), sv.getMasinhvien())
        );
        assertTrue(exDelete.getMessage().contains("đã có điểm đánh giá"));
    }

    @Test
    @DisplayName("Khóa nộp báo cáo riêng biệt cho từng đăng ký đề tài (không khóa nhầm đăng ký khác)")
    void testReportLockIsPerRegistration() {
        Sinhvien svA = taoSinhVien("SV_TV_20");
        Nhomsinhvien nhomA = new Nhomsinhvien();
        nhomA.setTennhom("Nhóm A - Có Điểm");
        nhomA.setDotdangkyid(dot.getId());
        nhomA.setNhomtruongid(svA.getId());
        em.persist(nhomA);
        em.persist(new Thanhviennhom(nhomA.getId(), svA.getId()));

        Dangkydetai dkA = new Dangkydetai();
        dkA.setNhomid(nhomA.getId());
        dkA.setDetaiid(dt.getId());
        dkA.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dkA.setNgaydangky(LocalDateTime.now());
        em.persist(dkA);

        // Nhóm A có điểm
        Diem dA = new Diem();
        dA.setDangkydetaiid(dkA.getId());
        dA.setGiangvienid(gv.getId());
        dA.setDiemso(new BigDecimal("9.0"));
        em.persist(dA);

        // Nhóm B chưa có điểm
        Sinhvien svB = taoSinhVien("SV_TV_21");
        Nhomsinhvien nhomB = new Nhomsinhvien();
        nhomB.setTennhom("Nhóm B - Chưa Có Điểm");
        nhomB.setDotdangkyid(dot.getId());
        nhomB.setNhomtruongid(svB.getId());
        em.persist(nhomB);
        em.persist(new Thanhviennhom(nhomB.getId(), svB.getId()));

        Dangkydetai dkB = new Dangkydetai();
        dkB.setNhomid(nhomB.getId());
        dkB.setDetaiid(dt.getId());
        dkB.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dkB.setNgaydangky(LocalDateTime.now());
        em.persist(dkB);
        em.flush();

        // Nhóm A bị khóa, Nhóm B KHÔNG bị khóa
        assertTrue(nopbaocaoService.kiemTraDaKhoaNopBaoCao(dkA.getId()));
        assertFalse(nopbaocaoService.kiemTraDaKhoaNopBaoCao(dkB.getId()));

        // Nhóm B nộp thành công
        byte[] validPdf = "%PDF-1.4 sample content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "baocao_b.pdf", "application/pdf", validPdf);
        assertDoesNotThrow(() -> nopbaocaoService.nopBaocaoVoiUpload(dkB.getId(), file, svB.getMasinhvien()));
    }

    @Test
    @DisplayName("Xác minh chỉ nhóm trưởng đúng nhóm mới được nộp hoặc xóa báo cáo")
    void testOnlyGroupLeaderCanSubmitOrDeleteReport() {
        Sinhvien svLeader = taoSinhVien("SV_TV_30");
        Sinhvien svMember = taoSinhVien("SV_TV_31");
        Sinhvien svOutsider = taoSinhVien("SV_TV_32");

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setTennhom("Nhóm Quyền Nộp");
        nhom.setDotdangkyid(dot.getId());
        nhom.setNhomtruongid(svLeader.getId());
        em.persist(nhom);
        em.persist(new Thanhviennhom(nhom.getId(), svLeader.getId()));
        em.persist(new Thanhviennhom(nhom.getId(), svMember.getId()));

        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(dt.getId());
        dk.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dk.setNgaydangky(LocalDateTime.now());
        em.persist(dk);
        em.flush();

        byte[] validPdf = "%PDF-1.4 sample content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "baocao.pdf", "application/pdf", validPdf);

        // Thành viên thường cố nộp -> Từ chối
        assertThrows(NghiepvuException.class, () ->
                nopbaocaoService.nopBaocaoVoiUpload(dk.getId(), file, svMember.getMasinhvien())
        );

        // Người ngoài nhóm cố nộp -> Từ chối
        assertThrows(NghiepvuException.class, () ->
                nopbaocaoService.nopBaocaoVoiUpload(dk.getId(), file, svOutsider.getMasinhvien())
        );

        // Nhóm trưởng nộp -> Thành công
        Nopbaocao nb = assertDoesNotThrow(() ->
                nopbaocaoService.nopBaocaoVoiUpload(dk.getId(), file, svLeader.getMasinhvien())
        );

        // Thành viên thường cố xóa -> Từ chối
        assertThrows(NghiepvuException.class, () ->
                nopbaocaoService.xoaBaocao(nb.getId(), svMember.getMasinhvien())
        );

        // Nhóm trưởng xóa -> Thành công
        assertDoesNotThrow(() ->
                nopbaocaoService.xoaBaocao(nb.getId(), svLeader.getMasinhvien())
        );
    }
}
