package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NghiepvuDetaiService {

    private final DotdangkyRepository dotdangkyRepository;
    private final DetaiRepository detaiRepository;
    private final GiangvienhuongdanRepository giangvienhuongdanRepository;
    private final NhomsinhvienRepository nhomsinhvienRepository;
    private final ThanhviennhomRepository thanhviennhomRepository;
    private final DangkydetaiRepository dangkydetaiRepository;
    private final NopbaocaoRepository nopbaocaoRepository;
    private final DiemRepository diemRepository;
    private final KetquaRepository ketquaRepository;
    private final KetquaPublicationService publicationService;

    public NghiepvuDetaiService(
            DotdangkyRepository dotdangkyRepository,
            DetaiRepository detaiRepository,
            GiangvienhuongdanRepository giangvienhuongdanRepository,
            NhomsinhvienRepository nhomsinhvienRepository,
            ThanhviennhomRepository thanhviennhomRepository,
            DangkydetaiRepository dangkydetaiRepository,
            NopbaocaoRepository nopbaocaoRepository,
            DiemRepository diemRepository,
            KetquaRepository ketquaRepository,
            KetquaPublicationService publicationService) {
        this.dotdangkyRepository = dotdangkyRepository;
        this.detaiRepository = detaiRepository;
        this.giangvienhuongdanRepository = giangvienhuongdanRepository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
        this.thanhviennhomRepository = thanhviennhomRepository;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.nopbaocaoRepository = nopbaocaoRepository;
        this.diemRepository = diemRepository;
        this.ketquaRepository = ketquaRepository;
        this.publicationService = publicationService;
    }

    // GIAI DOAN 1: GV de xuat de tai -> duyet -> cong bo
    public Detai dexuatDetai(Detai detai) {
        Dotdangky dot = layDot(detai.getDotdangkyid());
        LocalDateTime now = LocalDateTime.now();
        if (dot.getTrangthai() != TrangthaiDotdangky.GIAIDOAN1) {
            throw new NghiepvuException("Chi duoc de xuat de tai khi dot dang o Giai doan 1");
        }
        if (now.isBefore(dot.getBatdaugiangvien()) || now.isAfter(dot.getKetthucgiangvien())) {
            throw new NghiepvuException("Ngoai thoi gian giang vien dang ky de tai");
        }
        detai.setTrangthai(TrangthaiDetai.CHODUYET);
        return detaiRepository.save(detai);
    }

    public Detai duyetDetai(Long detaiid, boolean chapnhan) {
        Detai detai = layDetai(detaiid);
        if (layDot(detai.getDotdangkyid()).getTrangthai() != TrangthaiDotdangky.GIAIDOAN1
                || detai.getTrangthai() != TrangthaiDetai.CHODUYET) {
            throw new NghiepvuException("Chi duoc duyet de tai CHODUYET trong Giai doan 1");
        }
        detai.setTrangthai(chapnhan ? TrangthaiDetai.DADUYET : TrangthaiDetai.TUCHOI);
        return detaiRepository.save(detai);
    }

    public Detai congboDetai(Long detaiid) {
        Detai detai = layDetai(detaiid);
        if (layDot(detai.getDotdangkyid()).getTrangthai() != TrangthaiDotdangky.GIAIDOAN1) {
            throw new NghiepvuException("Chi cong bo de tai trong Giai doan 1");
        }
        if (detai.getTrangthai() != TrangthaiDetai.DADUYET) {
            throw new NghiepvuException("Chi de tai da duyet moi duoc cong bo");
        }
        long soGvhd = giangvienhuongdanRepository.countByDetaiid(detaiid);
        if (soGvhd < 1 || soGvhd > 2) {
            throw new NghiepvuException("De tai phai co tu 1 den 2 giang vien huong dan");
        }
        detai.setTrangthai(TrangthaiDetai.CONGBO);
        return detaiRepository.save(detai);
    }

    // GIAI DOAN 2: nhom SV dang ky de tai da cong bo
    public Dangkydetai dangkyDetai(Long nhomid, Long detaiid) {
        Nhomsinhvien nhom = nhomsinhvienRepository.findById(nhomid)
                .orElseThrow(() -> new NghiepvuException("Khong tim thay nhom"));
        Detai detai = layDetai(detaiid);
        Dotdangky dot = layDot(nhom.getDotdangkyid());

        if (!detai.getDotdangkyid().equals(nhom.getDotdangkyid())) {
            throw new NghiepvuException("Nhom va de tai khong cung dot dang ky");
        }
        if (detai.getTrangthai() != TrangthaiDetai.CONGBO) {
            throw new NghiepvuException("Chi duoc dang ky de tai da cong bo");
        }
        if (dot.getTrangthai() != TrangthaiDotdangky.GIAIDOAN2) {
            throw new NghiepvuException("Chi duoc dang ky de tai khi dot da mo Giai doan 2");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(dot.getBatdausinhvien()) || now.isAfter(dot.getKetthucsinhvien())) {
            throw new NghiepvuException("Ngoai thoi gian sinh vien dang ky de tai");
        }
        if (dangkydetaiRepository.findByNhomid(nhomid).isPresent()) {
            throw new NghiepvuException("Moi nhom chi duoc dang ky mot de tai");
        }
        var dsThanhvien = thanhviennhomRepository.findByNhomid(nhomid);
        long soThanhvien = dsThanhvien.size();
        if (soThanhvien < 3 || soThanhvien > 5) {
            throw new NghiepvuException("Nhom phai co tu 3 den 5 sinh vien");
        }
        if (dsThanhvien.stream()
                .noneMatch(tv -> tv.getSinhvienid().equals(nhom.getNhomtruongid()))) {
            throw new NghiepvuException("Nhom truong phai la thanh vien cua nhom");
        }

        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhomid);
        dk.setDetaiid(detaiid);
        dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dk.setNgaydangky(now);
        return dangkydetaiRepository.save(dk);
    }

    // Chi nhom truong duoc nop bao cao
    public Nopbaocao nopBaocao(Long dangkydetaiid, Long sinhvienid, String duongdanfile) {
        Dangkydetai dk = dangkydetaiRepository.findById(dangkydetaiid)
                .orElseThrow(() -> new NghiepvuException("Khong tim thay dang ky de tai"));
        Nhomsinhvien nhom = nhomsinhvienRepository.findById(dk.getNhomid())
                .orElseThrow(() -> new NghiepvuException("Khong tim thay nhom"));
        if (!nhom.getNhomtruongid().equals(sinhvienid)) {
            throw new NghiepvuException("Chi nhom truong duoc nop bao cao");
        }

        Nopbaocao nop = new Nopbaocao();
        nop.setDangkydetaiid(dangkydetaiid);
        nop.setSinhvienid(sinhvienid);
        nop.setDuongdanfile(duongdanfile);
        nop.setNgaynop(LocalDateTime.now());
        return nopbaocaoRepository.save(nop);
    }

    // Diem cuoi = trung binh cong cac diem thanh phan
    public Ketqua tinhVaLuuKetqua(Long dangkydetaiid) {
        List<Diem> ds = diemRepository.findByDangkydetaiid(dangkydetaiid);
        if (ds.isEmpty()) {
            throw new NghiepvuException("Chua co diem de tinh ket qua");
        }
        BigDecimal tong = ds.stream()
                .map(Diem::getDiemso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tb = tong.divide(BigDecimal.valueOf(ds.size()), 2, RoundingMode.HALF_UP);

        // Giữ ID của kết quả hiện có, tránh vi phạm UNIQUE(Dangkydetaiid).
        // Không được tính lại kết quả đã công bố.
        Ketqua kq = ketquaRepository.findByDangkydetaiid(dangkydetaiid)
                .orElseGet(Ketqua::new);
        if (Boolean.TRUE.equals(kq.getDacongbo())) {
            throw new NghiepvuException("Kết quả đã công bố, không được tính lại.");
        }
        kq.setDangkydetaiid(dangkydetaiid);
        kq.setDiemcuoi(tb);
        kq.setDacongbo(false);
        return ketquaRepository.save(kq);
    }

    public Ketqua congboKetqua(Long ketquaid) {
        return publicationService.publish(ketquaid);
    }

    private Dotdangky layDot(Long id) {
        return dotdangkyRepository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Khong tim thay dot dang ky"));
    }

    private Detai layDetai(Long id) {
        return detaiRepository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Khong tim thay de tai"));
    }
}
