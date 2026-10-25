package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.NhomSinhVienDto;
import com.example.quanlydetaisinhvien.dto.TaoNhomRequest;
import com.example.quanlydetaisinhvien.dto.ThanhVienDto;
import com.example.quanlydetaisinhvien.entity.*;
import java.time.LocalDateTime;
import com.example.quanlydetaisinhvien.dto.LoiMoiThanhVienDto;
import com.example.quanlydetaisinhvien.repository.LoimoithanhvienRepository;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.NhomsinhvienService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.Objects;

@Service
@Transactional
public class NhomsinhvienServiceImpl implements NhomsinhvienService {
    @Override
    @Transactional(readOnly = true)
    public List<Nhomsinhvien> layNhomChoQuanTri(int limit) {
        int size = Math.min(100, Math.max(1, limit));
        return repository.findAll(org.springframework.data.domain.PageRequest.of(0, size)).getContent();
    }


    private final NhomsinhvienRepository repository;
    private final ThanhviennhomRepository thanhviennhomRepository;
    private final SinhvienRepository sinhvienRepository;
    private final TaikhoanRepository taikhoanRepository;
    private final DotdangkyRepository dotdangkyRepository;
    private final DangkydetaiRepository dangkydetaiRepository;
    private final DetaiRepository detaiRepository;
    private final NopbaocaoRepository nopbaocaoRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private LoimoithanhvienRepository loimoithanhvienRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private void huyLoiMoiTrongGiaoDichMoi(Long loiMoiId) {
        if (loimoithanhvienRepository == null || loiMoiId == null) return;
        loimoithanhvienRepository.findById(loiMoiId).ifPresent(lm -> {
            lm.setTrangthai(TrangthaiLoimoi.DA_HUY);
            lm.setNgayphanhoi(LocalDateTime.now());
            loimoithanhvienRepository.saveAndFlush(lm);
        });
    }

    public void setLoimoithanhvienRepository(LoimoithanhvienRepository loimoithanhvienRepository) {
        this.loimoithanhvienRepository = loimoithanhvienRepository;
    }

    public void setTransactionManager(org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public NhomsinhvienServiceImpl(
            NhomsinhvienRepository repository,
            ThanhviennhomRepository thanhviennhomRepository,
            SinhvienRepository sinhvienRepository,
            TaikhoanRepository taikhoanRepository,
            DotdangkyRepository dotdangkyRepository,
            DangkydetaiRepository dangkydetaiRepository,
            DetaiRepository detaiRepository,
            NopbaocaoRepository nopbaocaoRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) LoimoithanhvienRepository loimoithanhvienRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.thanhviennhomRepository = thanhviennhomRepository;
        this.sinhvienRepository = sinhvienRepository;
        this.taikhoanRepository = taikhoanRepository;
        this.dotdangkyRepository = dotdangkyRepository;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.detaiRepository = detaiRepository;
        this.nopbaocaoRepository = nopbaocaoRepository;
        this.loimoithanhvienRepository = loimoithanhvienRepository;
        this.transactionManager = transactionManager;
    }

    public NhomsinhvienServiceImpl(
            NhomsinhvienRepository repository,
            ThanhviennhomRepository thanhviennhomRepository,
            SinhvienRepository sinhvienRepository,
            TaikhoanRepository taikhoanRepository,
            DotdangkyRepository dotdangkyRepository,
            DangkydetaiRepository dangkydetaiRepository,
            DetaiRepository detaiRepository,
            NopbaocaoRepository nopbaocaoRepository) {
        this(repository, thanhviennhomRepository, sinhvienRepository, taikhoanRepository,
             dotdangkyRepository, dangkydetaiRepository, detaiRepository, nopbaocaoRepository, null, null);
    }

    public NhomsinhvienServiceImpl(
            NhomsinhvienRepository repository,
            ThanhviennhomRepository thanhviennhomRepository,
            SinhvienRepository sinhvienRepository,
            TaikhoanRepository taikhoanRepository,
            DotdangkyRepository dotdangkyRepository,
            DangkydetaiRepository dangkydetaiRepository,
            DetaiRepository detaiRepository,
            NopbaocaoRepository nopbaocaoRepository,
            LoimoithanhvienRepository loimoithanhvienRepository) {
        this(repository, thanhviennhomRepository, sinhvienRepository, taikhoanRepository,
             dotdangkyRepository, dangkydetaiRepository, detaiRepository, nopbaocaoRepository, loimoithanhvienRepository, null);
    }

    @Override
    public List<Nhomsinhvien> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Nhomsinhvien> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Không cho phép ghi trực tiếp qua giao diện CRUD chung: cách này bỏ qua
     * kiểm tra danh tính, vai trò và trạng thái nghiệp vụ.
     * Gọi các phương thức nghiệp vụ có tham số currentUsername thay thế.
     */
    @Override
    public Nhomsinhvien save(Nhomsinhvien entity) {
        throw new NghiepvuException("Không hỗ trợ lưu nhóm sinh viên trực tiếp; vui lòng sử dụng luồng nghiệp vụ có kiểm tra quyền");
    }

    @Override
    public void deleteById(Long id) {
        throw new NghiepvuException("Không hỗ trợ xóa nhóm sinh viên trực tiếp; vui lòng sử dụng luồng nghiệp vụ có kiểm tra quyền");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NhomSinhVienDto> layDanhSachNhomDto() {
        return layDanhSachNhomDto(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NhomSinhVienDto> layDanhSachNhomDto(int page) {
        List<Nhomsinhvien> list = repository.findAll(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 50, org.springframework.data.domain.Sort.by("id").descending())).getContent();
        return chuyenDanhSachNhomSangDto(list, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NhomSinhVienDto> layDanhSachNhomChoUser(String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Optional<Taikhoan> tkOpt = taikhoanRepository.findByTendangnhap(currentUsername);
        if (tkOpt.isPresent() && tkOpt.get().getVaitro() == Vaitro.TRUONGKHOA) {
            return layDanhSachNhomDto();
        }
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty()) {
            return new ArrayList<>();
        }
        List<Thanhviennhom> tvList = thanhviennhomRepository.findBySinhvienid(svOpt.get().getId());
        Set<Long> ids = tvList.stream().map(Thanhviennhom::getNhomid).collect(Collectors.toSet());
        return chuyenDanhSachNhomSangDto(repository.findAllById(ids), true);
    }

    @Override
    @Transactional(readOnly = true)
    public NhomSinhVienDto layNhomCuaSinhVien(String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            return null;
        }
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty()) {
            return null;
        }
        List<Thanhviennhom> tvList = thanhviennhomRepository.findBySinhvienid(svOpt.get().getId());
        if (tvList.isEmpty()) {
            return null;
        }
        // Lấy nhóm mới nhất của sinh viên
        Long nhomid = tvList.get(tvList.size() - 1).getNhomid();
        return repository.findById(nhomid).map(nhom -> chuyenSangDto(nhom, true)).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean kiemTraSinhVienThuocNhom(Long nhomid, String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty() || nhomid == null) {
            return false;
        }
        Optional<Taikhoan> tkOpt = taikhoanRepository.findByTendangnhap(currentUsername);
        if (tkOpt.isPresent() && tkOpt.get().getVaitro() == Vaitro.TRUONGKHOA) {
            return true;
        }
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty()) {
            return false;
        }
        return thanhviennhomRepository.existsByNhomidAndSinhvienid(nhomid, svOpt.get().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public NhomSinhVienDto layChiTietNhomDto(Long id) {
        Nhomsinhvien nhom = repository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm với ID: " + id));
        return chuyenSangDto(nhom, true);
    }

    @Override
    public Nhomsinhvien taoNhom(TaoNhomRequest request, String currentUsername) {
        if (request.getTennhom() == null || request.getTennhom().trim().isEmpty()) {
            throw new NghiepvuException("Tên nhóm không được để trống");
        }
        if (request.getDotdangkyid() == null) {
            throw new NghiepvuException("Vui lòng chọn đợt đăng ký");
        }

        Dotdangky dot = dotdangkyRepository.findById(request.getDotdangkyid())
                .orElseThrow(() -> new NghiepvuException("Đợt đăng ký không tồn tại"));

        if (dot.getTrangthai() == TrangthaiDotdangky.DONG) {
            throw new NghiepvuException("Đợt đăng ký này đã đóng, không thể tạo nhóm mới");
        }

        if (repository.existsByDotdangkyidAndTennhom(dot.getId(), request.getTennhom().trim())) {
            throw new NghiepvuException("Tên nhóm đã tồn tại trong đợt đăng ký này");
        }

        Sinhvien nhomtruong;
        Taikhoan tk = currentUsername != null ? taikhoanRepository.findByTendangnhap(currentUsername).orElse(null) : null;
        if (tk != null && tk.getVaitro() == Vaitro.SINHVIEN) {
            nhomtruong = sinhvienRepository.findByMasinhvien(currentUsername)
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin sinh viên cho tài khoản đăng nhập"));
        } else if (request.getMasinhvienNhomtruong() != null && !request.getMasinhvienNhomtruong().trim().isEmpty()) {
            nhomtruong = sinhvienRepository.findByMasinhvien(request.getMasinhvienNhomtruong().trim())
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy sinh viên nhóm trưởng với MSSV: " + request.getMasinhvienNhomtruong()));
        } else {
            throw new NghiepvuException("Vui lòng chỉ định sinh viên làm nhóm trưởng");
        }

        // Quy tắc: Một sinh viên không được xuất hiện trong hai nhóm khác nhau thuộc cùng một đợt đăng ký
        if (thanhviennhomRepository.existsSinhvienInActiveDot(nhomtruong.getId())) {
            throw new NghiepvuException("Sinh viên " + nhomtruong.getMasinhvien() + " hiện đang tham gia một nhóm trong đợt đăng ký chưa kết thúc");
        }

        Nhomsinhvien nhom = new Nhomsinhvien();
        nhom.setDotdangkyid(dot.getId());
        nhom.setTennhom(request.getTennhom().trim());
        nhom.setNhomtruongid(nhomtruong.getId());
        Nhomsinhvien savedNhom = repository.save(nhom);

        // Mỗi nhóm phải có nhóm trưởng và nhóm trưởng phải là thành viên của chính nhóm đó
        Thanhviennhom tv = new Thanhviennhom();
        tv.setNhomid(savedNhom.getId());
        tv.setSinhvienid(nhomtruong.getId());
        thanhviennhomRepository.save(tv);

        return savedNhom;
    }

    @Override
    public Nhomsinhvien capNhatThongTinNhom(Long nhomid, String tenNhomMoi, String currentUsername) {
        Nhomsinhvien nhom = repository.findById(nhomid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm với ID: " + nhomid));

        kiemTraQuyenQuanLyNhom(nhom, currentUsername);

        if (tenNhomMoi == null || tenNhomMoi.trim().isEmpty()) {
            throw new NghiepvuException("Tên nhóm không được để trống");
        }

        String tenTrimmed = tenNhomMoi.trim();
        if (!tenTrimmed.equalsIgnoreCase(nhom.getTennhom())) {
            if (repository.existsByDotdangkyidAndTennhom(nhom.getDotdangkyid(), tenTrimmed)) {
                throw new NghiepvuException("Tên nhóm đã tồn tại trong đợt đăng ký này");
            }
            nhom.setTennhom(tenTrimmed);
        }

        return repository.save(nhom);
    }

    @Override
    public void xoaNhom(Long nhomid, String currentUsername) {
        Nhomsinhvien nhom = repository.findById(nhomid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm với ID: " + nhomid));

        kiemTraQuyenQuanLyNhom(nhom, currentUsername);

        // Kiểm tra quan hệ dữ liệu: Đăng ký đề tài
        Optional<Dangkydetai> dkOpt = dangkydetaiRepository.findByNhomid(nhomid);
        if (dkOpt.isPresent()) {
            Dangkydetai dk = dkOpt.get();
            if (nopbaocaoRepository.existsByDangkydetaiid(dk.getId())) {
                throw new NghiepvuException("Không thể xóa nhóm đã nộp báo cáo đề tài");
            }
            if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                throw new NghiepvuException("Không thể xóa nhóm khi đề tài đã được duyệt chính thức");
            }
            dangkydetaiRepository.deleteById(dk.getId());
        }

        // Xóa thành viên nhóm trước khi xóa nhóm
        thanhviennhomRepository.deleteByNhomid(nhomid);
        repository.deleteById(nhomid);
    }

    @Override
    public void themThanhVien(Long nhomid, String masinhvien, String currentUsername) {
        // Khóa bi quan nhóm để đảm bảo đồng bộ tuyệt đối với luồng nhận lời mời
        Nhomsinhvien nhom = repository.findByIdForUpdate(nhomid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm với ID: " + nhomid));

        kiemTraQuyenQuanLyNhom(nhom, currentUsername);

        if (masinhvien == null || masinhvien.trim().isEmpty()) {
            throw new NghiepvuException("Mã số sinh viên không được để trống");
        }

        // 1. Nhóm tối đa 5 sinh viên - Sử dụng Current Read có khóa để ngăn chặn race condition trên MySQL REPEATABLE READ
        List<Thanhviennhom> currentMembers = thanhviennhomRepository.findByNhomidForUpdate(nhomid);
        if (currentMembers.size() >= 5) {
            throw new NghiepvuException("Nhóm đã đạt số lượng tối đa 05 sinh viên theo quy định");
        }

        // 2. Kiểm tra trạng thái đề tài và báo cáo của nhóm
        Optional<Dangkydetai> dkOpt = dangkydetaiRepository.findByNhomid(nhomid);
        if (dkOpt.isPresent()) {
            Dangkydetai dk = dkOpt.get();
            if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                throw new NghiepvuException("Không thể thêm thành viên khi đề tài của nhóm đã được duyệt chính thức");
            }
            if (nopbaocaoRepository.existsByDangkydetaiid(dk.getId())) {
                throw new NghiepvuException("Không thể thêm thành viên khi nhóm đã nộp báo cáo đề tài");
            }
        }

        // 3. Kiểm tra trạng thái đợt đăng ký
        Dotdangky dot = dotdangkyRepository.findById(nhom.getDotdangkyid()).orElse(null);
        if (dot != null && dot.getTrangthai() == TrangthaiDotdangky.DONG) {
            throw new NghiepvuException("Đợt đăng ký này đã đóng, không thể thay đổi danh sách thành viên");
        }

        Sinhvien sv = sinhvienRepository.findByMasinhvien(masinhvien.trim())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy sinh viên với MSSV: " + masinhvien.trim()));

        // 4. Không cho phép thêm trùng một sinh viên vào cùng nhóm
        if (thanhviennhomRepository.existsByNhomidAndSinhvienid(nhomid, sv.getId())) {
            throw new NghiepvuException("Sinh viên này đã có trong nhóm");
        }

        // 5. Không cho phép sinh viên đang là Nhóm trưởng của nhóm khác tham gia nhóm này
        if (!repository.findByNhomtruongid(sv.getId()).isEmpty()) {
            throw new NghiepvuException("Sinh viên " + sv.getMasinhvien() + " đang là nhóm trưởng của nhóm khác, không thể thêm vào nhóm này");
        }

        // 6. Một sinh viên không được xuất hiện trong hai nhóm khác nhau thuộc cùng một đợt đăng ký
        if (thanhviennhomRepository.existsSinhvienInActiveDot(sv.getId())) {
            throw new NghiepvuException("Sinh viên " + sv.getMasinhvien() + " hiện đang tham gia nhóm khác trong đợt đăng ký chưa kết thúc");
        }

        // 7. Chặn nếu sinh viên đã tham gia nhóm khác đang có đề tài được đăng ký hoặc duyệt chính thức
        List<Thanhviennhom> tvCuaSv = thanhviennhomRepository.findBySinhvienid(sv.getId());
        for (Thanhviennhom tvOther : tvCuaSv) {
            if (!tvOther.getNhomid().equals(nhomid)) {
                Optional<Dangkydetai> dkOtherOpt = dangkydetaiRepository.findByNhomid(tvOther.getNhomid());
                if (dkOtherOpt.isPresent()) {
                    TrangthaiDangkydetai tt = dkOtherOpt.get().getTrangthai();
                    if (tt == TrangthaiDangkydetai.DADUYET || tt == TrangthaiDangkydetai.CHODUYET) {
                        throw new NghiepvuException("Sinh viên " + sv.getMasinhvien() + " đã tham gia nhóm khác đang đăng ký hoặc đã được duyệt đề tài");
                    }
                }
            }
        }

        Taikhoan tkNguoiThaoTac = (currentUsername != null) ? taikhoanRepository.findByTendangnhap(currentUsername).orElse(null) : null;
        boolean laTruongKhoa = (tkNguoiThaoTac != null && tkNguoiThaoTac.getVaitro() == Vaitro.TRUONGKHOA);

        if (laTruongKhoa) {
            Thanhviennhom tv = new Thanhviennhom();
            tv.setNhomid(nhomid);
            tv.setSinhvienid(sv.getId());
            thanhviennhomRepository.save(tv);
        } else {
            if (loimoithanhvienRepository != null && loimoithanhvienRepository.existsByNhomidAndSinhvienidAndTrangthai(nhomid, sv.getId(), TrangthaiLoimoi.CHO_XAC_NHAN)) {
                throw new NghiepvuException("Đã gửi lời mời đến sinh viên " + sv.getMasinhvien() + " rồi, đang chờ sinh viên xác nhận");
            }
            Sinhvien svNhomTruong = sinhvienRepository.findById(nhom.getNhomtruongid())
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin nhóm trưởng"));
            Loimoithanhvien loiMoi = new Loimoithanhvien(nhomid, sv.getId(), svNhomTruong.getId());
            if (loimoithanhvienRepository != null) {
                loimoithanhvienRepository.save(loiMoi);
            }
        }
    }

    @Override
    public void xoaThanhVien(Long nhomid, Long sinhvienid, String currentUsername) {
        Nhomsinhvien nhom = repository.findById(nhomid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm với ID: " + nhomid));

        kiemTraQuyenQuanLyNhom(nhom, currentUsername);

        if (!thanhviennhomRepository.existsByNhomidAndSinhvienid(nhomid, sinhvienid)) {
            throw new NghiepvuException("Sinh viên không phải là thành viên của nhóm");
        }

        // Không được để thao tác xóa nhóm trưởng tạo ra trạng thái dữ liệu không hợp lệ
        if (nhom.getNhomtruongid().equals(sinhvienid)) {
            throw new NghiepvuException("Không thể xóa nhóm trưởng khỏi nhóm. Vui lòng chuyển vai trò nhóm trưởng cho thành viên khác trước khi xóa.");
        }

        // 1. Kiểm tra trạng thái đề tài và báo cáo của nhóm
        Optional<Dangkydetai> dkOpt = dangkydetaiRepository.findByNhomid(nhomid);
        if (dkOpt.isPresent()) {
            Dangkydetai dk = dkOpt.get();
            if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                throw new NghiepvuException("Không thể xóa thành viên khi đề tài của nhóm đã được duyệt chính thức");
            }
            if (dk.getTrangthai() == TrangthaiDangkydetai.CHODUYET) {
                long soLuongHienTai = thanhviennhomRepository.countByNhomid(nhomid);
                if (soLuongHienTai <= 3) {
                    throw new NghiepvuException("Không thể xóa thành viên vì nhóm đang chờ duyệt đề tài (yêu cầu tối thiểu 3 thành viên). Vui lòng hủy đăng ký đề tài nếu muốn thay đổi sĩ số.");
                }
            }
            if (nopbaocaoRepository.existsByDangkydetaiid(dk.getId())) {
                throw new NghiepvuException("Không thể xóa thành viên khi nhóm đã nộp báo cáo đề tài");
            }
        }

        // 2. Kiểm tra trạng thái đợt đăng ký
        Dotdangky dot = dotdangkyRepository.findById(nhom.getDotdangkyid()).orElse(null);
        if (dot != null && dot.getTrangthai() == TrangthaiDotdangky.DONG) {
            throw new NghiepvuException("Đợt đăng ký này đã đóng, không thể thay đổi danh sách thành viên");
        }

        thanhviennhomRepository.deleteByNhomidAndSinhvienid(nhomid, sinhvienid);
    }

    @Override
    public void doiNhomTruong(Long nhomid, Long sinhvienidMoi, String currentUsername) {
        Nhomsinhvien nhom = repository.findById(nhomid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm với ID: " + nhomid));

        kiemTraQuyenQuanLyNhom(nhom, currentUsername);

        // Kiểm tra trạng thái đợt đăng ký
        Dotdangky dot = dotdangkyRepository.findById(nhom.getDotdangkyid()).orElse(null);
        if (dot != null && dot.getTrangthai() == TrangthaiDotdangky.DONG) {
            throw new NghiepvuException("Đợt đăng ký này đã đóng, không thể thay đổi thông tin nhóm trưởng");
        }

        // Nhóm trưởng phải là thành viên của chính nhóm đó
        if (!thanhviennhomRepository.existsByNhomidAndSinhvienid(nhomid, sinhvienidMoi)) {
            throw new NghiepvuException("Nhóm trưởng mới bắt buộc phải là một thành viên của nhóm");
        }

        nhom.setNhomtruongid(sinhvienidMoi);
        repository.save(nhom);
    }

    @Override
    public void roiNhom(Long nhomid, String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            throw new NghiepvuException("Vui lòng đăng nhập để thực hiện thao tác này");
        }
        Nhomsinhvien nhom = repository.findById(nhomid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm với ID: " + nhomid));

        Sinhvien sv = sinhvienRepository.findByMasinhvien(currentUsername.trim())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin sinh viên"));

        if (!thanhviennhomRepository.existsByNhomidAndSinhvienid(nhomid, sv.getId())) {
            throw new NghiepvuException("Bạn không phải thành viên của nhóm này");
        }

        if (nhom.getNhomtruongid().equals(sv.getId())) {
            throw new NghiepvuException("Nhóm trưởng không thể tự rời nhóm. Vui lòng chuyển vai trò nhóm trưởng cho thành viên khác hoặc xóa nhóm.");
        }

        Optional<Dangkydetai> dkOpt = dangkydetaiRepository.findByNhomid(nhomid);
        if (dkOpt.isPresent()) {
            Dangkydetai dk = dkOpt.get();
            if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                throw new NghiepvuException("Không thể rời nhóm khi đề tài đã được duyệt chính thức");
            }
            if (dk.getTrangthai() == TrangthaiDangkydetai.CHODUYET) {
                long soLuongHienTai = thanhviennhomRepository.countByNhomid(nhomid);
                if (soLuongHienTai <= 3) {
                    throw new NghiepvuException("Không thể rời nhóm vì nhóm đang chờ duyệt đề tài (yêu cầu tối thiểu 3 thành viên). Vui lòng hủy đăng ký đề tài nếu muốn thay đổi sĩ số.");
                }
            }
            if (nopbaocaoRepository.existsByDangkydetaiid(dk.getId())) {
                throw new NghiepvuException("Không thể rời nhóm khi nhóm đã nộp báo cáo đề tài");
            }
        }

        thanhviennhomRepository.deleteByNhomidAndSinhvienid(nhomid, sv.getId());
    }

    private void kiemTraQuyenQuanLyNhom(Nhomsinhvien nhom, String currentUsername) {
        if (currentUsername == null) return;
        Taikhoan tk = taikhoanRepository.findByTendangnhap(currentUsername).orElse(null);
        if (tk == null) return;

        // Trưởng khoa có toàn quyền quản trị
        if (tk.getVaitro() == Vaitro.TRUONGKHOA) {
            return;
        }

        // Nếu là sinh viên, chỉ nhóm trưởng mới có quyền thao tác quản lý nhóm
        if (tk.getVaitro() == Vaitro.SINHVIEN) {
            Sinhvien sv = sinhvienRepository.findByMasinhvien(currentUsername).orElse(null);
            if (sv == null || !nhom.getNhomtruongid().equals(sv.getId())) {
                throw new NghiepvuException("Chỉ nhóm trưởng mới có quyền thực hiện thao tác này");
            }
        }
    }

    /** Nạp quan hệ theo lô cho danh sách nhóm, không truy vấn lại từng hàng. */
    private List<NhomSinhVienDto> chuyenDanhSachNhomSangDto(Iterable<Nhomsinhvien> source, boolean includeMembers) {
        List<Nhomsinhvien> nhoms = new ArrayList<>();
        source.forEach(nhoms::add);
        if (nhoms.isEmpty()) return new ArrayList<>();
        Set<Long> nhomIds = nhoms.stream().map(Nhomsinhvien::getId).collect(Collectors.toSet());
        Set<Long> dotIds = nhoms.stream().map(Nhomsinhvien::getDotdangkyid).collect(Collectors.toSet());
        Map<Long, Dotdangky> dots = new HashMap<>();
        dotdangkyRepository.findAllById(dotIds).forEach(d -> dots.put(d.getId(), d));
        List<Thanhviennhom> members = thanhviennhomRepository.findByNhomidIn(nhomIds);
        Map<Long, List<Thanhviennhom>> membersByNhom = members.stream().collect(Collectors.groupingBy(Thanhviennhom::getNhomid));
        Map<Long, Dangkydetai> dks = dangkydetaiRepository.findByNhomidIn(nhomIds).stream()
                .collect(Collectors.toMap(Dangkydetai::getNhomid, d -> d, (a,b) -> a));
        Set<Long> svIds = new HashSet<>();
        nhoms.forEach(n -> svIds.add(n.getNhomtruongid()));
        if (includeMembers) members.forEach(m -> svIds.add(m.getSinhvienid()));
        Map<Long, Sinhvien> svs = new HashMap<>();
        sinhvienRepository.findAllById(svIds).forEach(v -> svs.put(v.getId(), v));
        Set<Long> tkIds = svs.values().stream().map(Sinhvien::getTaikhoanid)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Taikhoan> tks = new HashMap<>();
        taikhoanRepository.findAllById(tkIds).forEach(t -> tks.put(t.getId(), t));
        Set<Long> detaiIds = dks.values().stream().map(Dangkydetai::getDetaiid).collect(Collectors.toSet());
        Map<Long, Detai> detais = new HashMap<>();
        detaiRepository.findAllById(detaiIds).forEach(d -> detais.put(d.getId(), d));
        List<NhomSinhVienDto> result = new ArrayList<>();
        for (Nhomsinhvien n : nhoms) {
            NhomSinhVienDto dto = new NhomSinhVienDto();
            dto.setId(n.getId()); dto.setDotdangkyid(n.getDotdangkyid());
            dto.setTennhom(n.getTennhom()); dto.setNhomtruongid(n.getNhomtruongid());
            Dotdangky dot = dots.get(n.getDotdangkyid());
            if (dot != null) dto.setTendot(dot.getTendot());
            Sinhvien leader = svs.get(n.getNhomtruongid());
            if (leader != null) {
                dto.setMasinhvienNhomtruong(leader.getMasinhvien());
                Taikhoan tk = tks.get(leader.getTaikhoanid());
                if (tk != null) dto.setTenNhomtruong(tk.getHoten());
            }
            List<Thanhviennhom> list = membersByNhom.getOrDefault(n.getId(), List.of());
            dto.setSoThanhvien(list.size());
            Dangkydetai dk = dks.get(n.getId());
            if (dk != null) {
                dto.setDangkydetaiid(dk.getId()); dto.setDetaiid(dk.getDetaiid());
                if (dk.getTrangthai() != null) dto.setTrangthaiDangky(dk.getTrangthai().name());
                Detai dt = detais.get(dk.getDetaiid());
                if (dt != null) dto.setTendetai(dt.getTendetai());
            }
            if (includeMembers) {
                List<ThanhVienDto> tvDtos = new ArrayList<>();
                for (Thanhviennhom tv : list) {
                    Sinhvien sv = svs.get(tv.getSinhvienid());
                    if (sv == null) continue;
                    Taikhoan tk = tks.get(sv.getTaikhoanid());
                    tvDtos.add(new ThanhVienDto(sv.getId(), sv.getMasinhvien(),
                            tk == null ? "" : tk.getHoten(), tk == null ? "" : tk.getEmail(),
                            sv.getLop(), n.getNhomtruongid().equals(sv.getId())));
                }
                dto.setDanhSachThanhVien(tvDtos);
            }
            result.add(dto);
        }
        return result;
    }

    private NhomSinhVienDto chuyenSangDto(Nhomsinhvien nhom, boolean includeMembers) {
        NhomSinhVienDto dto = new NhomSinhVienDto();
        dto.setId(nhom.getId());
        dto.setDotdangkyid(nhom.getDotdangkyid());
        dto.setTennhom(nhom.getTennhom());
        dto.setNhomtruongid(nhom.getNhomtruongid());

        // Đợt đăng ký
        dotdangkyRepository.findById(nhom.getDotdangkyid()).ifPresent(d -> dto.setTendot(d.getTendot()));

        // Nhóm trưởng
        sinhvienRepository.findById(nhom.getNhomtruongid()).ifPresent(sv -> {
            dto.setMasinhvienNhomtruong(sv.getMasinhvien());
            taikhoanRepository.findById(sv.getTaikhoanid()).ifPresent(tk -> dto.setTenNhomtruong(tk.getHoten()));
        });

        // Số thành viên
        List<Thanhviennhom> tvList = thanhviennhomRepository.findByNhomid(nhom.getId());
        dto.setSoThanhvien(tvList.size());

        // Đề tài đăng ký nếu có
        dangkydetaiRepository.findByNhomid(nhom.getId()).ifPresent(dk -> {
            dto.setDangkydetaiid(dk.getId());
            dto.setDetaiid(dk.getDetaiid());
            dto.setTrangthaiDangky(dk.getTrangthai().name());
            detaiRepository.findById(dk.getDetaiid()).ifPresent(dt -> dto.setTendetai(dt.getTendetai()));
        });

        if (includeMembers) {
            List<ThanhVienDto> memberDtos = new ArrayList<>();
            for (Thanhviennhom tv : tvList) {
                sinhvienRepository.findById(tv.getSinhvienid()).ifPresent(sv -> {
                    String hoten = "";
                    String email = "";
                    Optional<Taikhoan> tkOpt = taikhoanRepository.findById(sv.getTaikhoanid());
                    if (tkOpt.isPresent()) {
                        hoten = tkOpt.get().getHoten();
                        email = tkOpt.get().getEmail();
                    }
                    boolean laNhomTruong = nhom.getNhomtruongid().equals(sv.getId());
                    memberDtos.add(new ThanhVienDto(sv.getId(), sv.getMasinhvien(), hoten, email, sv.getLop(), laNhomTruong));
                });
            }
            dto.setDanhSachThanhVien(memberDtos);

            if (loimoithanhvienRepository != null) {
                List<Loimoithanhvien> pendingList = loimoithanhvienRepository.findByNhomidAndTrangthai(nhom.getId(), TrangthaiLoimoi.CHO_XAC_NHAN);
                List<LoiMoiThanhVienDto> lmDtos = new ArrayList<>();
                if (!pendingList.isEmpty()) {
                    Set<Long> pendingSvIds = pendingList.stream().map(Loimoithanhvien::getSinhvienid).collect(Collectors.toSet());
                    Map<Long, Sinhvien> pendingSvMap = sinhvienRepository.findAllById(pendingSvIds).stream()
                            .collect(Collectors.toMap(Sinhvien::getId, Function.identity()));
                    Set<Long> pendingTkIds = pendingSvMap.values().stream().map(Sinhvien::getTaikhoanid).filter(Objects::nonNull).collect(Collectors.toSet());
                    Map<Long, Taikhoan> pendingTkMap = taikhoanRepository.findAllById(pendingTkIds).stream()
                            .collect(Collectors.toMap(Taikhoan::getId, Function.identity()));

                    for (Loimoithanhvien lm : pendingList) {
                        LoiMoiThanhVienDto lmDto = new LoiMoiThanhVienDto();
                        lmDto.setId(lm.getId());
                        lmDto.setNhomid(lm.getNhomid());
                        lmDto.setSinhvienid(lm.getSinhvienid());
                        lmDto.setNgaytao(lm.getNgaytao());
                        lmDto.setTrangthai(lm.getTrangthai());
                        Sinhvien pendingSv = pendingSvMap.get(lm.getSinhvienid());
                        if (pendingSv != null) {
                            lmDto.setMasinhvien(pendingSv.getMasinhvien());
                            Taikhoan pendingTk = pendingTkMap.get(pendingSv.getTaikhoanid());
                            if (pendingTk != null) lmDto.setHotenSinhvien(pendingTk.getHoten());
                        }
                        lmDtos.add(lmDto);
                    }
                }
                dto.setDsLoiMoiDangCho(lmDtos);
            }
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoiMoiThanhVienDto> layDanhSachLoiMoiCuaSinhVien(String currentUsername) {
        if (currentUsername == null || loimoithanhvienRepository == null) {
            return new ArrayList<>();
        }
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty()) {
            return new ArrayList<>();
        }
        Sinhvien sv = svOpt.get();
        List<Loimoithanhvien> list = loimoithanhvienRepository.findBySinhvienidAndTrangthaiOrderByIdDesc(
                sv.getId(), TrangthaiLoimoi.CHO_XAC_NHAN, org.springframework.data.domain.PageRequest.of(0, 50));
        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        // Tối ưu batch loading để triệt tiêu nguy cơ N+1 Query
        Set<Long> nhomIds = list.stream().map(Loimoithanhvien::getNhomid).collect(Collectors.toSet());
        Map<Long, Nhomsinhvien> nhomMap = repository.findAllById(nhomIds).stream()
                .collect(Collectors.toMap(Nhomsinhvien::getId, Function.identity()));

        Set<Long> dotIds = nhomMap.values().stream().map(Nhomsinhvien::getDotdangkyid).collect(Collectors.toSet());
        Map<Long, Dotdangky> dotMap = dotdangkyRepository.findAllById(dotIds).stream()
                .collect(Collectors.toMap(Dotdangky::getId, Function.identity()));

        Set<Long> leaderIds = nhomMap.values().stream().map(Nhomsinhvien::getNhomtruongid).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Sinhvien> leaderMap = sinhvienRepository.findAllById(leaderIds).stream()
                .collect(Collectors.toMap(Sinhvien::getId, Function.identity()));

        Set<Long> tkIds = new HashSet<>();
        if (sv.getTaikhoanid() != null) tkIds.add(sv.getTaikhoanid());
        leaderMap.values().forEach(l -> { if (l.getTaikhoanid() != null) tkIds.add(l.getTaikhoanid()); });
        Map<Long, Taikhoan> tkMap = taikhoanRepository.findAllById(tkIds).stream()
                .collect(Collectors.toMap(Taikhoan::getId, Function.identity()));

        List<Dangkydetai> dkList = dangkydetaiRepository.findByNhomidIn(new ArrayList<>(nhomIds));
        Map<Long, Dangkydetai> dkByNhomMap = dkList.stream()
                .collect(Collectors.toMap(Dangkydetai::getNhomid, Function.identity(), (a, b) -> a));

        Set<Long> detaiIds = dkList.stream().map(Dangkydetai::getDetaiid).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Detai> detaiMap = detaiRepository.findAllById(detaiIds).stream()
                .collect(Collectors.toMap(Detai::getId, Function.identity()));

        String svHoten = (sv.getTaikhoanid() != null && tkMap.containsKey(sv.getTaikhoanid())) ? tkMap.get(sv.getTaikhoanid()).getHoten() : null;

        List<LoiMoiThanhVienDto> dtos = new ArrayList<>();
        for (Loimoithanhvien lm : list) {
            Nhomsinhvien nhom = nhomMap.get(lm.getNhomid());
            if (nhom == null) continue;

            LoiMoiThanhVienDto dto = new LoiMoiThanhVienDto();
            dto.setId(lm.getId());
            dto.setNhomid(nhom.getId());
            dto.setTennhom(nhom.getTennhom());
            dto.setDotdangkyid(nhom.getDotdangkyid());
            dto.setTrangthai(lm.getTrangthai());
            dto.setNgaytao(lm.getNgaytao());
            dto.setNgayphanhoi(lm.getNgayphanhoi());

            dto.setSinhvienid(sv.getId());
            dto.setMasinhvien(sv.getMasinhvien());
            dto.setHotenSinhvien(svHoten);

            Dotdangky dot = dotMap.get(nhom.getDotdangkyid());
            if (dot != null) dto.setTendot(dot.getTendot());

            Sinhvien leader = leaderMap.get(nhom.getNhomtruongid());
            if (leader != null) {
                dto.setNguoimoiid(leader.getId());
                dto.setMasinhvienNhomtruong(leader.getMasinhvien());
                Taikhoan tkLeader = tkMap.get(leader.getTaikhoanid());
                if (tkLeader != null) dto.setTenNhomtruong(tkLeader.getHoten());
            }

            Dangkydetai dk = dkByNhomMap.get(nhom.getId());
            if (dk != null) {
                Detai dt = detaiMap.get(dk.getDetaiid());
                if (dt != null) dto.setTendetai(dt.getTendetai());
            }

            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(noRollbackFor = com.example.quanlydetaisinhvien.exception.NghiepvuException.class)
    public void dongYLoiMoi(Long loiMoiId, String currentUsername) {
        if (loimoithanhvienRepository == null) {
            throw new NghiepvuException("Hệ thống lời mời chưa sẵn sàng");
        }
        Loimoithanhvien lm = loimoithanhvienRepository.findById(loiMoiId)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy lời mời"));

        if (lm.getTrangthai() != TrangthaiLoimoi.CHO_XAC_NHAN) {
            throw new NghiepvuException("Lời mời này không còn khả dụng hoặc đã được xử lý");
        }

        Sinhvien sv = sinhvienRepository.findByMasinhvien(currentUsername)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin sinh viên"));

        if (!lm.getSinhvienid().equals(sv.getId())) {
            throw new NghiepvuException("Bạn không có quyền xử lý lời mời của người khác");
        }

        // Khóa bi quan nhóm để đảm bảo xử lý đồng thời không vượt quá 5 thành viên
        Nhomsinhvien nhom = repository.findByIdForUpdate(lm.getNhomid())
                .orElseThrow(() -> new NghiepvuException("Nhóm sinh viên không còn tồn tại"));

        // Chặn chấp nhận lời mời sau khi nhóm đã khóa thành viên:
        // 1. Kiểm tra trạng thái đề tài và báo cáo của nhóm
        Optional<Dangkydetai> dkOpt = dangkydetaiRepository.findByNhomid(nhom.getId());
        if (dkOpt.isPresent()) {
            Dangkydetai dk = dkOpt.get();
            if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
                huyLoiMoiTrongGiaoDichMoi(lm.getId());
                throw new NghiepvuException("Không thể tham gia nhóm vì đề tài của nhóm đã được duyệt chính thức (Nhóm đã khóa thành viên)");
            }
            if (nopbaocaoRepository.existsByDangkydetaiid(dk.getId())) {
                huyLoiMoiTrongGiaoDichMoi(lm.getId());
                throw new NghiepvuException("Không thể tham gia nhóm vì nhóm đã nộp báo cáo đề tài (Nhóm đã khóa thành viên)");
            }
        }

        // 2. Kiểm tra trạng thái đợt đăng ký
        Dotdangky dot = dotdangkyRepository.findById(nhom.getDotdangkyid()).orElse(null);
        if (dot != null && dot.getTrangthai() == TrangthaiDotdangky.DONG) {
            huyLoiMoiTrongGiaoDichMoi(lm.getId());
            throw new NghiepvuException("Đợt đăng ký này đã đóng, không thể tham gia nhóm");
        }

        // 3. Kiểm tra số lượng thành viên hiện tại với khóa bi quan (Current Read trên MySQL InnoDB)
        List<Thanhviennhom> currentMembers = thanhviennhomRepository.findByNhomidForUpdate(nhom.getId());
        if (currentMembers.size() >= 5) {
            huyLoiMoiTrongGiaoDichMoi(lm.getId());
            throw new NghiepvuException("Nhóm đã đủ 05 thành viên, không thể gia nhập");
        }

        // 4. Kiểm tra sinh viên đã có trong nhóm chưa
        if (currentMembers.stream().anyMatch(tv -> tv.getSinhvienid().equals(sv.getId()))) {
            lm.setTrangthai(TrangthaiLoimoi.DONG_Y);
            lm.setNgayphanhoi(LocalDateTime.now());
            loimoithanhvienRepository.save(lm);
            throw new NghiepvuException("Bạn đã là thành viên của nhóm này rồi");
        }

        // 5. Kiểm tra sinh viên đã thuộc nhóm khác trong cùng đợt chưa
        if (thanhviennhomRepository.existsSinhvienInActiveDot(sv.getId())) {
            throw new NghiepvuException("Bạn hiện đang tham gia một nhóm trong đợt đăng ký chưa kết thúc");
        }

        // Thêm vào nhóm chính thức
        Thanhviennhom tv = new Thanhviennhom(nhom.getId(), sv.getId());
        thanhviennhomRepository.save(tv);

        // Cập nhật trạng thái lời mời
        lm.setTrangthai(TrangthaiLoimoi.DONG_Y);
        lm.setNgayphanhoi(LocalDateTime.now());
        loimoithanhvienRepository.save(lm);

        // Hủy các lời mời khác của sinh viên trong cùng đợt
        List<Loimoithanhvien> otherLoiMoi = loimoithanhvienRepository.findBySinhvienidAndTrangthai(sv.getId(), TrangthaiLoimoi.CHO_XAC_NHAN);
        for (Loimoithanhvien other : otherLoiMoi) {
            if (!other.getId().equals(lm.getId())) {
                other.setTrangthai(TrangthaiLoimoi.DA_HUY);
                other.setNgayphanhoi(LocalDateTime.now());
                loimoithanhvienRepository.save(other);
            }
        }
    }

    @Override
    public void tuChoiLoiMoi(Long loiMoiId, String currentUsername) {
        if (loimoithanhvienRepository == null) {
            throw new NghiepvuException("Hệ thống lời mời chưa sẵn sàng");
        }
        Loimoithanhvien lm = loimoithanhvienRepository.findById(loiMoiId)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy lời mời"));

        Sinhvien sv = sinhvienRepository.findByMasinhvien(currentUsername)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin sinh viên"));

        if (!lm.getSinhvienid().equals(sv.getId())) {
            throw new NghiepvuException("Bạn không có quyền xử lý lời mời của người khác");
        }

        lm.setTrangthai(TrangthaiLoimoi.TU_CHOI);
        lm.setNgayphanhoi(LocalDateTime.now());
        loimoithanhvienRepository.save(lm);
    }

    @Override
    public void huyLoiMoi(Long nhomid, Long loiMoiId, String currentUsername) {
        if (loimoithanhvienRepository == null) return;
        Nhomsinhvien nhom = repository.findById(nhomid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy nhóm"));
        kiemTraQuyenQuanLyNhom(nhom, currentUsername);

        Loimoithanhvien lm = loimoithanhvienRepository.findById(loiMoiId)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy lời mời"));
        if (!lm.getNhomid().equals(nhomid)) {
            throw new NghiepvuException("Lời mời không thuộc về nhóm này");
        }

        lm.setTrangthai(TrangthaiLoimoi.DA_HUY);
        lm.setNgayphanhoi(LocalDateTime.now());
        loimoithanhvienRepository.save(lm);
    }

    @Override
    @Transactional(readOnly = true)
    public long demSoLoiMoiDangCho(String currentUsername) {
        if (currentUsername == null || loimoithanhvienRepository == null) return 0;
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty()) return 0;
        return loimoithanhvienRepository.countBySinhvienidAndTrangthai(svOpt.get().getId(), TrangthaiLoimoi.CHO_XAC_NHAN);
    }
}
