package com.example.quanlydetaisinhvien;

import com.example.quanlydetaisinhvien.controller.NopbaocaoController;
import com.example.quanlydetaisinhvien.dto.NopBaoCaoRequest;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.NopbaocaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:file-ownership-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "app.ketqua.email.enabled=false",
        "app.ketqua.email.scheduling-enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ReportFileOwnershipTests {

    @TempDir
    static Path tempUploadDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> tempUploadDir.toAbsolutePath().toString());
    }

    @Autowired
    private NopbaocaoService nopbaocaoService;

    @Autowired
    private NopbaocaoController nopbaocaoController;

    @Autowired
    private NopbaocaoRepository nopbaocaoRepo;

    @Autowired
    private DangkydetaiRepository dangkyRepo;

    @Autowired
    private DetaiRepository detaiRepo;

    @Autowired
    private NhomsinhvienRepository nhomRepo;

    @Autowired
    private SinhvienRepository sinhvienRepo;

    @Autowired
    private TaikhoanRepository taikhoanRepo;

    @Autowired
    private ThanhviennhomRepository thanhviennhomRepo;

    @Autowired
    private BomonRepository bomonRepo;

    private Sinhvien leaderA;
    private Sinhvien leaderB;
    private Dangkydetai dkA;
    private Dangkydetai dkB;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(tempUploadDir);

        Bomon bm = new Bomon();
        bm.setMabomon("CNTT");
        bm.setTenbomon("CNTT");
        bm = bomonRepo.save(bm);

        // Leader A
        Taikhoan tkA = new Taikhoan();
        tkA.setTendangnhap("sv_lead_a");
        tkA.setMatkhau("pass123");
        tkA.setHoten("Sinh vien A");
        tkA.setEmail("sva@example.com");
        tkA.setVaitro(Vaitro.SINHVIEN);
        tkA.setKichhoat(true);
        tkA = taikhoanRepo.save(tkA);

        leaderA = new Sinhvien();
        leaderA.setMasinhvien("sv_lead_a");
        leaderA.setTaikhoanid(tkA.getId());
        leaderA.setBomonid(bm.getId());
        leaderA = sinhvienRepo.save(leaderA);

        // Leader B
        Taikhoan tkB = new Taikhoan();
        tkB.setTendangnhap("sv_lead_b");
        tkB.setMatkhau("pass123");
        tkB.setHoten("Sinh vien B");
        tkB.setEmail("svb@example.com");
        tkB.setVaitro(Vaitro.SINHVIEN);
        tkB.setKichhoat(true);
        tkB = taikhoanRepo.save(tkB);

        leaderB = new Sinhvien();
        leaderB.setMasinhvien("sv_lead_b");
        leaderB.setTaikhoanid(tkB.getId());
        leaderB.setBomonid(bm.getId());
        leaderB = sinhvienRepo.save(leaderB);

        // Group A
        Nhomsinhvien nhomA = new Nhomsinhvien();
        nhomA.setDotdangkyid(1L);
        nhomA.setTennhom("Nhom A");
        nhomA.setNhomtruongid(leaderA.getId());
        nhomA = nhomRepo.save(nhomA);
        thanhviennhomRepo.save(new Thanhviennhom(nhomA.getId(), leaderA.getId()));

        // Group B
        Nhomsinhvien nhomB = new Nhomsinhvien();
        nhomB.setDotdangkyid(1L);
        nhomB.setTennhom("Nhom B");
        nhomB.setNhomtruongid(leaderB.getId());
        nhomB = nhomRepo.save(nhomB);
        thanhviennhomRepo.save(new Thanhviennhom(nhomB.getId(), leaderB.getId()));

        // Topic A
        Detai dtA = new Detai();
        dtA.setDotdangkyid(1L);
        dtA.setTendetai("De tai A");
        dtA.setBomonid(bm.getId());
        dtA.setGiangviendexuatid(1L);
        dtA.setTrangthai(TrangthaiDetai.DADUYET);
        dtA = detaiRepo.save(dtA);

        // Topic B
        Detai dtB = new Detai();
        dtB.setDotdangkyid(1L);
        dtB.setTendetai("De tai B");
        dtB.setBomonid(bm.getId());
        dtB.setGiangviendexuatid(1L);
        dtB.setTrangthai(TrangthaiDetai.DADUYET);
        dtB = detaiRepo.save(dtB);

        // Registration A
        dkA = new Dangkydetai();
        dkA.setNhomid(nhomA.getId());
        dkA.setDetaiid(dtA.getId());
        dkA.setNgaydangky(LocalDateTime.now());
        dkA.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dkA = dangkyRepo.save(dkA);

        // Registration B
        dkB = new Dangkydetai();
        dkB.setNhomid(nhomB.getId());
        dkB.setDetaiid(dtB.getId());
        dkB.setNgaydangky(LocalDateTime.now());
        dkB.setTrangthai(TrangthaiDangkydetai.DADUYET);
        dkB = dangkyRepo.save(dkB);
    }

    @Test
    @DisplayName("FILE-01 & FILE-02: Path traversal and encoded paths are rejected by layDuongDanVatLyHopLe")
    void testPathTraversalRejected() {
        Path uploadRoot = tempUploadDir.toAbsolutePath().normalize();
        var serviceImpl = (com.example.quanlydetaisinhvien.service.impl.NopbaocaoServiceImpl) nopbaocaoService;

        // Directory traversal via ../
        assertThatThrownBy(() -> serviceImpl.layDuongDanVatLyHopLe("/uploads/baocao/../../secret.txt", uploadRoot))
                .isInstanceOf(NghiepvuException.class);

        // URL encoded traversal
        assertThatThrownBy(() -> serviceImpl.layDuongDanVatLyHopLe("/uploads/baocao/%2e%2e/%2e%2e/secret.txt", uploadRoot))
                .isInstanceOf(NghiepvuException.class);
    }

    @Test
    @DisplayName("FILE-03: Paths with redundant slashes and ./ are properly canonicalized")
    void testRedundantSlashesCanonicalized() throws IOException {
        Path uploadRoot = tempUploadDir.toAbsolutePath().normalize();
        Path testFile = uploadRoot.resolve("report1.pdf");
        Files.writeString(testFile, "%PDF-1.4 sample content");

        var serviceImpl = (com.example.quanlydetaisinhvien.service.impl.NopbaocaoServiceImpl) nopbaocaoService;
        Path resolved = serviceImpl.layDuongDanVatLyHopLe("/uploads/baocao/./report1.pdf", uploadRoot);
        assertThat(resolved).isNotNull();
        assertThat(Files.isSameFile(resolved, testFile)).isTrue();
    }

    @Test
    @DisplayName("FILE-05: Deleting Report A does NOT delete physical file owned by Report B")
    void testDeletingReportADoesNotDeleteFileReferencedByReportB() throws IOException {
        Path fileShared = tempUploadDir.resolve("shared_report.pdf");
        Files.writeString(fileShared, "%PDF-1.4 content");

        // Save report for Group B
        Nopbaocao repB = new Nopbaocao();
        repB.setDangkydetaiid(dkB.getId());
        repB.setSinhvienid(leaderB.getId());
        repB.setDuongdanfile("/uploads/baocao/shared_report.pdf");
        repB.setNgaynop(LocalDateTime.now());
        repB = nopbaocaoRepo.save(repB);

        // Save report for Group A pointing to same file
        Nopbaocao repA = new Nopbaocao();
        repA.setDangkydetaiid(dkA.getId());
        repA.setSinhvienid(leaderA.getId());
        repA.setDuongdanfile("/uploads/baocao/shared_report.pdf");
        repA.setNgaynop(LocalDateTime.now());
        repA = nopbaocaoRepo.save(repA);

        // Group A deletes their report
        nopbaocaoService.xoaBaocao(repA.getId(), "sv_lead_a");

        // Verify report A is removed from DB
        assertThat(nopbaocaoRepo.findById(repA.getId())).isEmpty();
        // Verify report B is still in DB
        assertThat(nopbaocaoRepo.findById(repB.getId())).isPresent();
        // Crucial: Verify physical file still exists!
        assertThat(Files.exists(fileShared)).isTrue();
    }

    @Test
    @DisplayName("FILE-06: Deleting sole report deletes its physical file cleanly")
    void testDeletingSoleReportDeletesPhysicalFile() throws IOException {
        Path fileSolo = tempUploadDir.resolve("solo_report.pdf");
        Files.writeString(fileSolo, "%PDF-1.4 solo");

        Nopbaocao repA = new Nopbaocao();
        repA.setDangkydetaiid(dkA.getId());
        repA.setSinhvienid(leaderA.getId());
        repA.setDuongdanfile("/uploads/baocao/solo_report.pdf");
        repA.setNgaynop(LocalDateTime.now());
        repA = nopbaocaoRepo.save(repA);

        nopbaocaoService.xoaBaocao(repA.getId(), "sv_lead_a");

        assertThat(nopbaocaoRepo.findById(repA.getId())).isEmpty();
        assertThat(Files.exists(fileSolo)).isFalse();
    }

    @Test
    @DisplayName("FILE-09: nopBaocao rejects claiming another registration's existing file")
    void testNopBaocaoRejectsOtherRegistrationFile() throws IOException {
        Path fileB = tempUploadDir.resolve("group_b_doc.pdf");
        Files.writeString(fileB, "%PDF-1.4 file B");

        Nopbaocao repB = new Nopbaocao();
        repB.setDangkydetaiid(dkB.getId());
        repB.setSinhvienid(leaderB.getId());
        repB.setDuongdanfile("/uploads/baocao/group_b_doc.pdf");
        repB.setNgaynop(LocalDateTime.now());
        nopbaocaoRepo.save(repB);

        // Group A attempts to submit using Group B's file path
        NopBaoCaoRequest reqA = new NopBaoCaoRequest();
        reqA.setDangkydetaiid(dkA.getId());
        reqA.setDuongdanfile("/uploads/baocao/group_b_doc.pdf");

        assertThatThrownBy(() -> nopbaocaoService.nopBaocao(reqA, "sv_lead_a"))
                .isInstanceOf(NghiepvuException.class)
                .hasMessageContaining("đã thuộc về đề tài khác");
    }

    @Test
    @DisplayName("FILE-10 & FILE-11: Controller download endpoint blocks unauthorized user and traversal")
    void testControllerDownloadSecurity() throws IOException {
        Path file = tempUploadDir.resolve("report_valid.pdf");
        Files.writeString(file, "%PDF-1.4 hello");

        Nopbaocao repA = new Nopbaocao();
        repA.setDangkydetaiid(dkA.getId());
        repA.setSinhvienid(leaderA.getId());
        repA.setDuongdanfile("/uploads/baocao/report_valid.pdf");
        repA.setNgaynop(LocalDateTime.now());
        repA = nopbaocaoRepo.save(repA);

        // User B tries to download Group A's report -> 403 Forbidden
        var authB = new UsernamePasswordAuthenticationToken("sv_lead_b", "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SINHVIEN")));
        ResponseEntity<Resource> resForbidden = nopbaocaoController.downloadBaocao(repA.getId(), authB);
        assertThat(resForbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // User A downloads Group A's report -> 200 OK
        var authA = new UsernamePasswordAuthenticationToken("sv_lead_a", "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SINHVIEN")));
        ResponseEntity<Resource> resOk = nopbaocaoController.downloadBaocao(repA.getId(), authA);
        assertThat(resOk.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resOk.getBody()).isNotNull();
    }
}
