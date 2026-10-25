package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NhomsinhvienServiceImplTest {

    @Autowired
    private NhomsinhvienServiceImpl service;

    @Autowired
    private BomonRepository bomonRepo;

    @Autowired
    private LoimoithanhvienRepository loiRepo;

    @Autowired
    private NhomsinhvienRepository nhomRepo;

    @Autowired
    private ThanhviennhomRepository tvRepo;

    @Autowired
    private DangkydetaiRepository dkRepo;

    @Autowired
    private SinhvienRepository svRepo;

    @Autowired
    private TaikhoanRepository tkRepo;

    private Sinhvien leader;
    private Nhomsinhvien group;
    private Loimoithanhvien invitation;

    @BeforeEach
    void setUp() {
        // create a leader account
        Taikhoan tk = new Taikhoan();
        tk.setTendangnhap("leader");
        tk.setHoten("Leader User");
        tk.setVaitro(Vaitro.TRUONGKHOA);
        tk.setKichhoat(true);
        tk.setMatkhau("password");
        tkRepo.save(tk);
        leader = new Sinhvien();
        leader.setMasinhvien("SV001");
        leader.setTaikhoanid(tk.getId());
        // assign a valid Bomon to satisfy NOT NULL constraint
        Bomon bomon = new Bomon();
        bomon.setMabomon("BM001");
        bomon.setTenbomon("Bomon 1");
        bomonRepo.save(bomon);
        leader.setBomonid(bomon.getId());
        svRepo.save(leader);
        // create a group
        group = new Nhomsinhvien();
        group.setTennhom("GroupA");
        group.setNhomtruongid(leader.getId());
        group.setDotdangkyid(1L); // assume existing registration period
        nhomRepo.save(group);
        // add leader as member
        tvRepo.save(new Thanhviennhom(group.getId(), leader.getId()));
        // create a normal student
        Taikhoan tk2 = new Taikhoan();
        tk2.setTendangnhap("student");
        tk2.setHoten("Student User");
        tk2.setVaitro(Vaitro.SINHVIEN);
        tk2.setKichhoat(true);
        tk2.setMatkhau("password");
        tkRepo.save(tk2);
        Sinhvien sv = new Sinhvien();
        sv.setMasinhvien("SV002");
        sv.setTaikhoanid(tk2.getId());
        // assign a valid Bomon (reuse the one created above)
        sv.setBomonid(bomon.getId());
        svRepo.save(sv);
        // create invitation for that student
        invitation = new Loimoithanhvien();
        // set the inviter (leader) to satisfy NGUOIMOIID NOT NULL constraint
        invitation.setNguoimoiid(leader.getId());
        invitation.setNhomid(group.getId());
        invitation.setSinhvienid(sv.getId());
        invitation.setTrangthai(TrangthaiLoimoi.CHO_XAC_NHAN);
        loiRepo.save(invitation);
        // create a registration record to lock the group (simulate locked state)
        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(group.getId());
        dk.setTrangthai(TrangthaiDangkydetai.DADUYET); // locked
        dkRepo.save(dk);
    }

    @Test
    @Transactional
    void testDongYLoiMoiRollbackPrevention() {
        // Expect exception because group is locked
        NghiepvuException ex = Assertions.assertThrows(NghiepvuException.class, () -> {
            service.dongYLoiMoi(invitation.getId(), "student");
        });
        Assertions.assertTrue(ex.getMessage().contains("không thể tham gia nhóm"));
        // After exception, the invitation status must be DA_HUY (persisted)
        Optional<Loimoithanhvien> after = loiRepo.findById(invitation.getId());
        Assertions.assertTrue(after.isPresent());
        Assertions.assertEquals(TrangthaiLoimoi.DA_HUY, after.get().getTrangthai());
    }
}
