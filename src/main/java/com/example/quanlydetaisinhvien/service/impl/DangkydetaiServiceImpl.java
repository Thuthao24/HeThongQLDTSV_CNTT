package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.DangKyDeTaiDto;
import com.example.quanlydetaisinhvien.dto.DangKyDeTaiRequest;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.DangkydetaiService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DangkydetaiServiceImpl implements DangkydetaiService {

    private final DangkydetaiRepository repository;
    private final NhomsinhvienRepository nhomsinhvienRepository;
    private final ThanhviennhomRepository thanhviennhomRepository;
    private final DetaiRepository detaiRepository;
    private final DotdangkyRepository dotdangkyRepository;
    private final SinhvienRepository sinhvienRepository;
    private final TaikhoanRepository taikhoanRepository;
    private final NopbaocaoRepository nopbaocaoRepository;
    private final KetquaRepository ketquaRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private GiangvienRepository giangvienRepository;

    public void setGiangvienRepository(GiangvienRepository giangvienRepository) {
        this.giangvienRepository = giangvienRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public DangkydetaiServiceImpl(
            DangkydetaiRepository repository,
            NhomsinhvienRepository nhomsinhvienRepository,
            ThanhviennhomRepository thanhviennhomRepository,
            DetaiRepository detaiRepository,
            DotdangkyRepository dotdangkyRepository,
            SinhvienRepository sinhvienRepository,
            TaikhoanRepository taikhoanRepository,
            NopbaocaoRepository nopbaocaoRepository,
            KetquaRepository ketquaRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) GiangvienRepository giangvienRepository) {
        this.repository = repository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
        this.thanhviennhomRepository = thanhviennhomRepository;
        this.detaiRepository = detaiRepository;
        this.dotdangkyRepository = dotdangkyRepository;
        this.sinhvienRepository = sinhvienRepository;
        this.taikhoanRepository = taikhoanRepository;
        this.nopbaocaoRepository = nopbaocaoRepository;
        this.ketquaRepository = ketquaRepository;
        this.giangvienRepository = giangvienRepository;
    }

    public DangkydetaiServiceImpl(
            DangkydetaiRepository repository,
            NhomsinhvienRepository nhomsinhvienRepository,
            ThanhviennhomRepository thanhviennhomRepository,
            DetaiRepository detaiRepository,
            DotdangkyRepository dotdangkyRepository,
            SinhvienRepository sinhvienRepository,
            TaikhoanRepository taikhoanRepository,
            NopbaocaoRepository nopbaocaoRepository,
            KetquaRepository ketquaRepository) {
        this(repository, nhomsinhvienRepository, thanhviennhomRepository, detaiRepository,
                dotdangkyRepository, sinhvienRepository, taikhoanRepository, nopbaocaoRepository, ketquaRepository, null);
    }

    public DangkydetaiServiceImpl(
            DangkydetaiRepository repository,
            NhomsinhvienRepository nhomsinhvienRepository,
            ThanhviennhomRepository thanhviennhomRepository,
            DetaiRepository detaiRepository,
            DotdangkyRepository dotdangkyRepository,
            SinhvienRepository sinhvienRepository,
            TaikhoanRepository taikhoanRepository,
            NopbaocaoRepository nopbaocaoRepository) {
        this(repository, nhomsinhvienRepository, thanhviennhomRepository, detaiRepository,
                dotdangkyRepository, sinhvienRepository, taikhoanRepository, nopbaocaoRepository, null, null);
    }

    @Override
    public List<Dangkydetai> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Dangkydetai> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public Dangkydetai save(Dangkydetai entity) {
        throw new NghiepvuException("Không ghi đăng ký trực tiếp; sử dụng nghiệp vụ đăng ký, duyệt hoặc hủy có kiểm tra quyền và thời gian.");
    }

    @Override
    public void deleteById(Long id) {
        throw new NghiepvuException("Không xóa đăng ký trực tiếp; sử dụng nghiệp vụ hủy đăng ký có kiểm tra quyền.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DangKyDeTaiDto> layDanhSachDangKyDto() {
        return layDanhSachDangKyDto(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DangKyDeTaiDto> layDanhSachDangKyDto(int page) {
        List<Dangkydetai> list = repository.findAll(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 50, org.springframework.data.domain.Sort.by("id").descending())).getContent();
        return chuyenDanhSachDangKySangDto(list);
    }

    private List<DangKyDeTaiDto> chuyenDanhSachDangKySangDto(List<Dangkydetai> list) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        // Bulk queries: avoid one query per registration while constructing the page.
        java.util.Map<Long, Nhomsinhvien> nhomMap = new java.util.HashMap<>();
        nhomsinhvienRepository.findAllById(list.stream().map(Dangkydetai::getNhomid).distinct().toList())
                .forEach(n -> nhomMap.put(n.getId(), n));
        java.util.Map<Long, Detai> detaiMap = new java.util.HashMap<>();
        detaiRepository.findAllById(list.stream().map(Dangkydetai::getDetaiid).distinct().toList())
                .forEach(d -> detaiMap.put(d.getId(), d));
        java.util.Map<Long, Dotdangky> dotMap = new java.util.HashMap<>();
        dotdangkyRepository.findAllById(nhomMap.values().stream().map(Nhomsinhvien::getDotdangkyid).distinct().toList())
                .forEach(d -> dotMap.put(d.getId(), d));
        java.util.Map<Long, Sinhvien> sinhvienMap = new java.util.HashMap<>();
        sinhvienRepository.findAllById(nhomMap.values().stream().map(Nhomsinhvien::getNhomtruongid).distinct().toList())
                .forEach(sv -> sinhvienMap.put(sv.getId(), sv));
        java.util.Map<Long, Taikhoan> accountMap = new java.util.HashMap<>();
        taikhoanRepository.findAllById(sinhvienMap.values().stream().map(Sinhvien::getTaikhoanid).distinct().toList())
                .forEach(tk -> accountMap.put(tk.getId(), tk));
        List<DangKyDeTaiDto> dtos = new ArrayList<>();
        for (Dangkydetai dk : list) {
            DangKyDeTaiDto dto = new DangKyDeTaiDto();
            dto.setId(dk.getId());
            dto.setNhomid(dk.getNhomid());
            dto.setDetaiid(dk.getDetaiid());
            dto.setTrangthai(dk.getTrangthai());
            dto.setNgaydangky(dk.getNgaydangky());
            Nhomsinhvien n = nhomMap.get(dk.getNhomid());
            if (n != null) {
                dto.setTennhom(n.getTennhom());
                dto.setDotdangkyid(n.getDotdangkyid());
                dto.setNhomtruongid(n.getNhomtruongid());
                Dotdangky dot = dotMap.get(n.getDotdangkyid());
                if (dot != null) { dto.setTendot(dot.getTendot()); dto.setHanNop(dot.getKetthucsinhvien()); }
                Sinhvien sv = sinhvienMap.get(n.getNhomtruongid());
                if (sv != null) {
                    dto.setMasinhvienNhomtruong(sv.getMasinhvien());
                    Taikhoan tk = accountMap.get(sv.getTaikhoanid());
                    if (tk != null) dto.setTenNhomtruong(tk.getHoten());
                }
            }
            Detai dt = detaiMap.get(dk.getDetaiid());
            if (dt != null) dto.setTendetai(dt.getTendetai());
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DangKyDeTaiDto> layDanhSachDangKyChoUser(String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Optional<Taikhoan> tkOpt = taikhoanRepository.findByTendangnhap(currentUsername);
        if (tkOpt.isEmpty()) {
            return new ArrayList<>();
        }
        Taikhoan tk = tkOpt.get();
        if (tk.getVaitro() == Vaitro.TRUONGKHOA) {
            return layDanhSachDangKyDto();
        }
        if (tk.getVaitro() == Vaitro.TRUONGBOMON && giangvienRepository != null) {
            Optional<Giangvien> gvOpt = giangvienRepository.findByTaikhoanid(tk.getId());
            if (gvOpt.isPresent() && gvOpt.get().getBomonid() != null) {
                List<Detai> bmDetais = detaiRepository.findByBomonid(gvOpt.get().getBomonid());
                List<Long> bmDetaiIds = bmDetais.stream().map(Detai::getId).toList();
                if (!bmDetaiIds.isEmpty()) {
                    return chuyenDanhSachDangKySangDto(repository.findByDetaiidIn(bmDetaiIds));
                }
                return new ArrayList<>();
            }
            return layDanhSachDangKyDto();
        }
        if (tk.getVaitro() == Vaitro.GIANGVIEN && giangvienRepository != null) {
            Optional<Giangvien> gvOpt = giangvienRepository.findByTaikhoanid(tk.getId());
            if (gvOpt.isPresent()) {
                List<Detai> myDetais = detaiRepository.findByGiangviendexuatid(gvOpt.get().getId());
                List<Long> myDetaiIds = myDetais.stream().map(Detai::getId).toList();
                if (!myDetaiIds.isEmpty()) {
                    return chuyenDanhSachDangKySangDto(repository.findByDetaiidIn(myDetaiIds));
                }
                return new ArrayList<>();
            }
            return new ArrayList<>();
        }
        DangKyDeTaiDto dkCuaSv = layDangKyCuaSinhVien(currentUsername);
        if (dkCuaSv != null) {
            List<DangKyDeTaiDto> list = new ArrayList<>();
            list.add(dkCuaSv);
            return list;
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public DangKyDeTaiDto layDangKyCuaSinhVien(String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            return null;
        }
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty()) {
            return null;
        }
        List<Thanhviennhom> tvList = thanhviennhomRepository.findBySinhvienid(svOpt.get().getId());
        for (Thanhviennhom tv : tvList) {
            Optional<Dangkydetai> dkOpt = repository.findByNhomid(tv.getNhomid());
            if (dkOpt.isPresent()) {
                return chuyenSangDto(dkOpt.get());
            }
        }
        return null;
    }

    private DangKyDeTaiDto chuyenSangDto(Dangkydetai dk) {
        DangKyDeTaiDto dto = new DangKyDeTaiDto();
        dto.setId(dk.getId());
        dto.setNhomid(dk.getNhomid());
        dto.setDetaiid(dk.getDetaiid());
        dto.setTrangthai(dk.getTrangthai());
        dto.setNgaydangky(dk.getNgaydangky());

        nhomsinhvienRepository.findById(dk.getNhomid()).ifPresent(nhom -> {
            dto.setTennhom(nhom.getTennhom());
            dto.setDotdangkyid(nhom.getDotdangkyid());
            dto.setNhomtruongid(nhom.getNhomtruongid());
            dotdangkyRepository.findById(nhom.getDotdangkyid()).ifPresent(d -> {
                dto.setTendot(d.getTendot());
                dto.setHanNop(d.getKetthucsinhvien());
            });
            sinhvienRepository.findById(nhom.getNhomtruongid()).ifPresent(sv -> {
                dto.setMasinhvienNhomtruong(sv.getMasinhvien());
                taikhoanRepository.findById(sv.getTaikhoanid()).ifPresent(tk -> dto.setTenNhomtruong(tk.getHoten()));
            });
        });

        detaiRepository.findById(dk.getDetaiid()).ifPresent(dt -> dto.setTendetai(dt.getTendetai()));
        return dto;
    }

    @Override
    public Dangkydetai dangkyDetai(DangKyDeTaiRequest request, String currentUsername) {
        if (request == null || request.getNhomid() == null || request.getDetaiid() == null) {
            throw new NghiepvuException("Thông tin đăng ký đề tài không hợp lệ");
        }

        Nhomsinhvien nhom = nhomsinhvienRepository.findById(request.getNhomid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm sinh viên"));

        Detai detai = detaiRepository.findById(request.getDetaiid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đề tài"));

        Dotdangky dot = dotdangkyRepository.findById(nhom.getDotdangkyid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đợt đăng ký"));

        // 1. Nhóm và đề tài phải thuộc cùng đợt đăng ký
        if (!detai.getDotdangkyid().equals(nhom.getDotdangkyid())) {
            throw new NghiepvuException("Nhóm và đề tài không cùng đợt đăng ký");
        }

        // 2. Đề tài phải ở trạng thái công bố
        if (detai.getTrangthai() != TrangthaiDetai.CONGBO) {
            throw new NghiepvuException("Chỉ được đăng ký đề tài đã được công bố");
        }

        // 3. Đề tài chưa được duyệt chính thức cho một nhóm khác
        boolean daCoNhomDuyet = repository.existsByDetaiidAndTrangthai(detai.getId(), TrangthaiDangkydetai.DADUYET);
        if (daCoNhomDuyet) {
            throw new NghiepvuException("Đề tài này đã được phê duyệt cho một nhóm khác, vui lòng chọn đề tài khác");
        }

        // 4. Đợt đăng ký phải ở trạng thái cho phép đăng ký
        if (dot.getTrangthai() != TrangthaiDotdangky.GIAIDOAN2) {
            throw new NghiepvuException("Đợt đăng ký hiện không mở để sinh viên đăng ký đề tài");
        }

        LocalDateTime now = LocalDateTime.now();
        // Cả trạng thái GĐ2 lẫn khung giờ đăng ký đều phải hợp lệ.
        if (dot.getBatdausinhvien() == null || dot.getKetthucsinhvien() == null
                || now.isBefore(dot.getBatdausinhvien()) || now.isAfter(dot.getKetthucsinhvien())) {
            throw new NghiepvuException("Ngoài thời gian sinh viên đăng ký đề tài của đợt này");
        }

        // 5. Kiểm tra nhóm đã đăng ký đề tài trước đó chưa
        Optional<Dangkydetai> dkHienTaiOpt = repository.findByNhomid(nhom.getId());
        if (dkHienTaiOpt.isPresent()) {
            Dangkydetai dkHienTai = dkHienTaiOpt.get();
            if (dkHienTai.getTrangthai() == TrangthaiDangkydetai.TUCHOI) {
                // Nhóm đã bị từ chối đề tài cũ -> Xóa lượt đăng ký bị từ chối để cho phép đăng ký đề tài mới
                repository.delete(dkHienTai);
                repository.flush();
            } else {
                throw new NghiepvuException("Nhóm đã đăng ký đề tài trong đợt này, mỗi nhóm chỉ được đăng ký một đề tài");
            }
        }

        // 6. Kiểm tra số lượng thành viên trong nhóm (3 - 5 sinh viên)
        long soThanhvien = thanhviennhomRepository.countByNhomid(nhom.getId());
        if (soThanhvien < 3 || soThanhvien > 5) {
            throw new NghiepvuException("Nhóm phải có từ 3 đến 5 thành viên mới được đăng ký đề tài");
        }

        
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new NghiepvuException( "Vui lòng đăng nhập trước khi đăng ký đề tài");
        }

        Taikhoan tk = taikhoanRepository.findByTendangnhap(currentUsername).orElseThrow(() -> new NghiepvuException(
            "Không tìm thấy tài khoản"));

        if (tk.getVaitro() != Vaitro.SINHVIEN) { throw new NghiepvuException("Chỉ sinh viên là nhóm trưởng mới được đăng ký");
        }

        Sinhvien sv = sinhvienRepository.findByTaikhoanid(tk.getId()) .orElseThrow(() -> new NghiepvuException( "Không tìm thấy thông tin sinh viên"));

        if (!sv.getId().equals(nhom.getNhomtruongid())) {  throw new NghiepvuException(  "Chỉ nhóm trưởng mới được đăng ký đề tài");
        }

        if (!thanhviennhomRepository.existsByNhomidAndSinhvienid( nhom.getId(), sv.getId())) { throw new NghiepvuException(
        "Nhóm trưởng phải thuộc danh sách thành viên nhóm");
        }


        Dangkydetai dk = new Dangkydetai();
        dk.setNhomid(nhom.getId());
        dk.setDetaiid(detai.getId());
        dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dk.setNgaydangky(now);

        try {
            return repository.save(dk);
        } catch (DataIntegrityViolationException e) {
            // Xử lý tình huống hai yêu cầu đăng ký được gửi đồng thời
            throw new NghiepvuException("Nhóm đã đăng ký đề tài rồi, không thể đăng ký lần thứ hai");
        }
    }

    
@Override
public void huyDangky(Long dangkydetaiid, String currentUsername) {
    // 1. Kiểm tra đăng nhập
    if (currentUsername == null || currentUsername.isBlank()) {
        throw new org.springframework.security.access.AccessDeniedException(
                "Vui lòng đăng nhập để hủy đăng ký đề tài");
    }

    // 2. Chỉ sinh viên mới được thực hiện
    Taikhoan tk = taikhoanRepository.findByTendangnhap(currentUsername)
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                    "Tài khoản không có quyền hủy đăng ký đề tài"));

    if (tk.getVaitro() != Vaitro.SINHVIEN) {
        throw new org.springframework.security.access.AccessDeniedException(
                "Chỉ sinh viên là nhóm trưởng mới được hủy đăng ký đề tài");
    }

    // 3. Xác định sinh viên qua tài khoản
    Sinhvien sv = sinhvienRepository.findByTaikhoanid(tk.getId())
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                    "Không tìm thấy sinh viên được phép hủy đăng ký đề tài"));

    // 4. Tìm đăng ký và nhóm tương ứng
    Dangkydetai dk = repository.findById(dangkydetaiid)
            .orElseThrow(() -> new NghiepvuException(
                    "Không tìm thấy thông tin đăng ký đề tài"));

    Nhomsinhvien nhom = nhomsinhvienRepository.findById(dk.getNhomid())
            .orElseThrow(() -> new NghiepvuException(
                    "Không tìm thấy thông tin nhóm"));

    // 5. Chỉ nhóm trưởng của đúng nhóm mới được hủy
    if (!sv.getId().equals(nhom.getNhomtruongid())
            || !thanhviennhomRepository.existsByNhomidAndSinhvienid(
                    nhom.getId(), sv.getId())) {
        throw new org.springframework.security.access.AccessDeniedException(
                "Chỉ nhóm trưởng thuộc nhóm mới có quyền hủy đăng ký đề tài");
    }

    // 6. Không cho hủy nếu đã nộp báo cáo
    if (nopbaocaoRepository.existsByDangkydetaiid(dangkydetaiid)) {
        throw new NghiepvuException(
                "Không thể hủy đăng ký đề tài vì nhóm đã có báo cáo được nộp");
    }

    // 7. Không cho hủy nếu đề tài đã được duyệt
    if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
        throw new NghiepvuException(
                "Không thể hủy đề tài đã được duyệt chính thức. Vui lòng liên hệ Trưởng bộ môn/Khoa.");
    }

    // 8. Hủy đăng ký
    repository.deleteById(dangkydetaiid);
}



    @Override
    public Dangkydetai duyetDangky(
            Long dangkydetaiid,
            boolean chapNhan,
            String currentUsername) {

        Taikhoan approvingAccount = currentUsername == null
                ? null
                : taikhoanRepository.findByTendangnhap(currentUsername)
                        .orElse(null);

        if (approvingAccount == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vui lòng đăng nhập để thực hiện duyệt đăng ký đề tài");
        }

        // Lấy ID đề tài trước khi khóa giao dịch.
        Long detaiId = repository.findDetaiIdByRegistrationId(dangkydetaiid)
                .orElseThrow(() -> new NghiepvuException(
                        "Không tìm thấy đăng ký đề tài"));

        // Khóa đề tài để các yêu cầu duyệt cùng đề tài được xử lý lần lượt.
        Detai detai = detaiRepository.findByIdForUpdate(detaiId)
                .orElseThrow(() -> new NghiepvuException(
                        "Không tìm thấy đề tài"));

        boolean coQuyen = false;
        if (approvingAccount.getVaitro() == Vaitro.TRUONGKHOA) {
            coQuyen = true;
        } else if (giangvienRepository != null && (approvingAccount.getVaitro() == Vaitro.TRUONGBOMON || approvingAccount.getVaitro() == Vaitro.GIANGVIEN)) {
            Giangvien gv = giangvienRepository.findByTaikhoanid(approvingAccount.getId()).orElse(null);
            if (gv != null) {
                if (approvingAccount.getVaitro() == Vaitro.TRUONGBOMON && gv.getBomonid() != null && gv.getBomonid().equals(detai.getBomonid())) {
                    coQuyen = true;
                } else if (gv.getId().equals(detai.getGiangviendexuatid())) {
                    coQuyen = true;
                }
            }
        }

        if (!coQuyen) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền duyệt đăng ký cho đề tài này");
        }

        Dangkydetai dk = repository.findByIdForUpdate(dangkydetaiid)
                .orElseThrow(() -> new NghiepvuException(
                        "Không tìm thấy đăng ký đề tài"));

        if (!detaiId.equals(dk.getDetaiid())) {
            throw new NghiepvuException(
                    "Thông tin đề tài đăng ký không hợp lệ");
        }

        if (dk.getTrangthai() != TrangthaiDangkydetai.CHODUYET) {
            throw new NghiepvuException(
                    "Chỉ xử lý đăng ký đang chờ duyệt");
        }

        if (chapNhan) {
            boolean daCoNhomKhac = repository.findApprovedForUpdate(
                    detaiId, TrangthaiDangkydetai.DADUYET)
                    .stream()
                    .anyMatch(x -> !x.getId().equals(dangkydetaiid));

            if (daCoNhomKhac) {
                throw new NghiepvuException(
                        "Đề tài này đã được duyệt cho nhóm khác");
            }
            // The existing topic/registration locks also serialize result initialization.
            if (ketquaRepository.findByDangkydetaiid(dangkydetaiid).isEmpty()) {
                Ketqua result = new Ketqua();
                result.setDangkydetaiid(dangkydetaiid);
                result.setDacongbo(false);
                ketquaRepository.save(result);
            }
        }

        dk.setTrangthai(chapNhan
                ? TrangthaiDangkydetai.DADUYET
                : TrangthaiDangkydetai.TUCHOI);

        return repository.save(dk);
    }
}

