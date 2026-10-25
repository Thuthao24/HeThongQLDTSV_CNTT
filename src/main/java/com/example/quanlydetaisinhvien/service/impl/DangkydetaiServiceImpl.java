
package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.DangKyDeTaiDto;
import com.example.quanlydetaisinhvien.dto.DangKyDeTaiRequest;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.DangkydetaiService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

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

    public void setGiangvienRepository(
            GiangvienRepository giangvienRepository) {
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
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            GiangvienRepository giangvienRepository) {

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

        this(repository, nhomsinhvienRepository,
            thanhviennhomRepository, detaiRepository,
            dotdangkyRepository, sinhvienRepository,
            taikhoanRepository, nopbaocaoRepository,
            ketquaRepository, null);
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

        this(repository, nhomsinhvienRepository,
            thanhviennhomRepository, detaiRepository,
            dotdangkyRepository, sinhvienRepository,
            taikhoanRepository, nopbaocaoRepository,
            null, null);
    }

    // ===== CRUD CO KIEM SOAT =====

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
        throw new NghiepvuException(
            "Không lưu đăng ký trực tiếp. "
            + "Vui lòng sử dụng chức năng đăng ký đề tài.");
    }

    @Override
    public void deleteById(Long id) {
        throw new NghiepvuException(
            "Không xóa đăng ký trực tiếp. "
            + "Vui lòng sử dụng chức năng hủy đăng ký.");
    }

    // ===== DANH SACH DANG KY =====

    @Override
    @Transactional(readOnly = true)
    public List<DangKyDeTaiDto> layDanhSachDangKyDto() {
        return layDanhSachDangKyDto(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DangKyDeTaiDto> layDanhSachDangKyDto(int page) {
        List<Dangkydetai> list = repository.findAll(
            org.springframework.data.domain.PageRequest.of(
                Math.max(0, page), 50,
                org.springframework.data.domain.Sort.by("id").descending()
            )
        ).getContent();

        return chuyenDanhSachDangKySangDto(list);
    }

    private List<DangKyDeTaiDto> chuyenDanhSachDangKySangDto(
            List<Dangkydetai> list) {

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Nhomsinhvien> nhomMap = new HashMap<>();
        nhomsinhvienRepository.findAllById(
            list.stream().map(Dangkydetai::getNhomid)
                .filter(Objects::nonNull).distinct().toList()
        ).forEach(n -> nhomMap.put(n.getId(), n));

        Map<Long, Detai> detaiMap = new HashMap<>();
        detaiRepository.findAllById(
            list.stream().map(Dangkydetai::getDetaiid)
                .filter(Objects::nonNull).distinct().toList()
        ).forEach(d -> detaiMap.put(d.getId(), d));

        Map<Long, Dotdangky> dotMap = new HashMap<>();
        dotdangkyRepository.findAllById(
            nhomMap.values().stream().map(Nhomsinhvien::getDotdangkyid)
                .filter(Objects::nonNull).distinct().toList()
        ).forEach(d -> dotMap.put(d.getId(), d));

        Map<Long, Sinhvien> sinhvienMap = new HashMap<>();
        sinhvienRepository.findAllById(
            nhomMap.values().stream().map(Nhomsinhvien::getNhomtruongid)
                .filter(Objects::nonNull).distinct().toList()
        ).forEach(s -> sinhvienMap.put(s.getId(), s));

        Map<Long, Taikhoan> accountMap = new HashMap<>();
        taikhoanRepository.findAllById(
            sinhvienMap.values().stream().map(Sinhvien::getTaikhoanid)
                .filter(Objects::nonNull).distinct().toList()
        ).forEach(t -> accountMap.put(t.getId(), t));

        List<DangKyDeTaiDto> result = new ArrayList<>();

        for (Dangkydetai dk : list) {
            DangKyDeTaiDto dto = new DangKyDeTaiDto();
            dto.setId(dk.getId());
            dto.setNhomid(dk.getNhomid());
            dto.setDetaiid(dk.getDetaiid());
            dto.setTrangthai(dk.getTrangthai());
            dto.setNgaydangky(dk.getNgaydangky());

            Nhomsinhvien nhom = nhomMap.get(dk.getNhomid());

            if (nhom != null) {
                dto.setTennhom(nhom.getTennhom());
                dto.setDotdangkyid(nhom.getDotdangkyid());
                dto.setNhomtruongid(nhom.getNhomtruongid());

                Dotdangky dot = dotMap.get(nhom.getDotdangkyid());
                if (dot != null) {
                    dto.setTendot(dot.getTendot());
                    dto.setHanNop(dot.getKetthucsinhvien());
                }

                Sinhvien sv = sinhvienMap.get(nhom.getNhomtruongid());
                if (sv != null) {
                    dto.setMasinhvienNhomtruong(sv.getMasinhvien());

                    Taikhoan tk = accountMap.get(sv.getTaikhoanid());
                    if (tk != null) {
                        dto.setTenNhomtruong(tk.getHoten());
                    }
                }
            }

            Detai dt = detaiMap.get(dk.getDetaiid());
            if (dt != null) {
                dto.setTendetai(dt.getTendetai());
            }
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DangKyDeTaiDto> layDanhSachDangKyChoUser(
            String currentUsername) {

        if (currentUsername == null || currentUsername.isBlank()) {
            return new ArrayList<>();
        }

        Optional<Taikhoan> accountOpt =
            taikhoanRepository.findByTendangnhap(currentUsername);

        if (accountOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Taikhoan account = accountOpt.get();

        if (account.getVaitro() == Vaitro.TRUONGKHOA) {
            return layDanhSachDangKyDto();
        }

        if (account.getVaitro() == Vaitro.TRUONGBOMON) {
            if (giangvienRepository == null) {
                return new ArrayList<>();
            }

            Optional<Giangvien> teacher =
                giangvienRepository.findByTaikhoanid(account.getId());

            if (teacher.isEmpty() || teacher.get().getBomonid() == null) {
                return new ArrayList<>();
            }

            List<Long> ids = detaiRepository.findByBomonid(
                teacher.get().getBomonid()
            ).stream().map(Detai::getId).toList();

            return ids.isEmpty() ? new ArrayList<>()
                : chuyenDanhSachDangKySangDto(
                    repository.findByDetaiidIn(ids));
        }

        if (account.getVaitro() == Vaitro.GIANGVIEN) {
            if (giangvienRepository == null) {
                return new ArrayList<>();
            }

            Optional<Giangvien> teacher =
                giangvienRepository.findByTaikhoanid(account.getId());

            if (teacher.isEmpty()) {
                return new ArrayList<>();
            }

            List<Long> ids = detaiRepository.findByGiangviendexuatid(
                teacher.get().getId()
            ).stream().map(Detai::getId).toList();

            return ids.isEmpty() ? new ArrayList<>()
                : chuyenDanhSachDangKySangDto(
                    repository.findByDetaiidIn(ids));
        }

        if (account.getVaitro() != Vaitro.SINHVIEN) {
            return new ArrayList<>();
        }

        DangKyDeTaiDto own = layDangKyCuaSinhVien(currentUsername);
        return own == null ? new ArrayList<>() : List.of(own);
    }

    @Override
    @Transactional(readOnly = true)
    public DangKyDeTaiDto layDangKyCuaSinhVien(
            String currentUsername) {

        if (currentUsername == null || currentUsername.isBlank()) {
            return null;
        }

        Optional<Sinhvien> svOpt =
            sinhvienRepository.findByMasinhvien(currentUsername);

        if (svOpt.isEmpty()) {
            return null;
        }

        List<Thanhviennhom> members =
            thanhviennhomRepository.findBySinhvienid(svOpt.get().getId());

        for (Thanhviennhom member : members) {
            Optional<Dangkydetai> dk =
                repository.findByNhomid(member.getNhomid());

            if (dk.isPresent()) {
                return chuyenSangDto(dk.get());
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

            dotdangkyRepository.findById(nhom.getDotdangkyid())
                .ifPresent(dot -> {
                    dto.setTendot(dot.getTendot());
                    dto.setHanNop(dot.getKetthucsinhvien());
                });

            sinhvienRepository.findById(nhom.getNhomtruongid())
                .ifPresent(sv -> {
                    dto.setMasinhvienNhomtruong(sv.getMasinhvien());
                    taikhoanRepository.findById(sv.getTaikhoanid())
                        .ifPresent(tk ->
                            dto.setTenNhomtruong(tk.getHoten()));
                });
        });

        detaiRepository.findById(dk.getDetaiid())
            .ifPresent(dt -> dto.setTendetai(dt.getTendetai()));

        return dto;
    }

    // ===== KIEM TRA DANG NHAP VA QUYEN =====

    private Sinhvien currentStudent(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Vui lòng đăng nhập");
        }

        Taikhoan account = taikhoanRepository
            .findByTendangnhap(username)
            .orElseThrow(() -> new AccessDeniedException(
                "Không xác định được tài khoản"));

        if (account.getVaitro() != Vaitro.SINHVIEN) {
            throw new AccessDeniedException(
                "Chỉ sinh viên được thực hiện đăng ký");
        }

        return sinhvienRepository.findByTaikhoanid(account.getId())
            .orElseThrow(() -> new AccessDeniedException(
                "Tài khoản chưa liên kết sinh viên"));
    }

    private void checkLeader(Nhomsinhvien nhom, Sinhvien sv) {
        if (!Objects.equals(sv.getId(), nhom.getNhomtruongid())) {
            throw new AccessDeniedException(
                "Chỉ nhóm trưởng được đăng ký hoặc hủy đăng ký");
        }

        if (!thanhviennhomRepository.existsByNhomidAndSinhvienid(
                nhom.getId(), sv.getId())) {
            throw new AccessDeniedException(
                "Nhóm trưởng phải có trong danh sách thành viên");
        }
    }

    private void checkRegistrationWindow(Dotdangky dot) {
        if (dot.getTrangthai() != TrangthaiDotdangky.GIAIDOAN2) {
            throw new NghiepvuException(
                "Đợt đăng ký chưa mở hoặc đã đóng Giai đoạn 2");
        }

        LocalDateTime now = LocalDateTime.now();

        if (dot.getBatdausinhvien() == null
                || dot.getKetthucsinhvien() == null
                || now.isBefore(dot.getBatdausinhvien())
                || !now.isBefore(dot.getKetthucsinhvien())) {
            throw new NghiepvuException(
                "Ngoài thời gian sinh viên đăng ký đề tài");
        }
    }

    // ===== DANG KY DE TAI =====

    @Override
    public Dangkydetai dangkyDetai(
            DangKyDeTaiRequest request, String currentUsername) {

        if (request == null || request.getNhomid() == null
                || request.getDetaiid() == null) {
            throw new NghiepvuException(
                "Thông tin đăng ký đề tài không hợp lệ");
        }

        Nhomsinhvien nhom = nhomsinhvienRepository
            .findById(request.getNhomid())
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy nhóm sinh viên"));

        Detai detai = detaiRepository
            .findById(request.getDetaiid())
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy đề tài"));

        Dotdangky dot = dotdangkyRepository
            .findById(nhom.getDotdangkyid())
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy đợt đăng ký"));

        // Nhom va de tai phai cung dot.
        if (!Objects.equals(
                nhom.getDotdangkyid(), detai.getDotdangkyid())) {
            throw new NghiepvuException(
                "Nhóm và đề tài phải thuộc cùng đợt đăng ký");
        }

        // Chi de tai CONGBO moi duoc dang ky.
        if (detai.getTrangthai() != TrangthaiDetai.CONGBO) {
            throw new NghiepvuException(
                "Chỉ được đăng ký đề tài đã công bố");
        }

        if (repository.existsByDetaiidAndTrangthai(
                detai.getId(), TrangthaiDangkydetai.DADUYET)) {
            throw new NghiepvuException(
                "Đề tài đã được duyệt cho một nhóm khác");
        }

        checkRegistrationWindow(dot);

        // Moi nhom chi co mot ban ghi dang ky.
        Optional<Dangkydetai> oldOpt =
            repository.findByNhomid(nhom.getId());

        if (oldOpt.isPresent()
                && oldOpt.get().getTrangthai()
                != TrangthaiDangkydetai.TUCHOI) {
            throw new NghiepvuException(
                "Nhóm đã đăng ký đề tài, không thể đăng ký trùng");
        }

        // Quy tac so thanh vien dang co cua he thong.
        long memberCount =
            thanhviennhomRepository.countByNhomid(nhom.getId());

        if (memberCount < 3 || memberCount > 5) {
            throw new NghiepvuException(
                "Nhóm cần từ 3 đến 5 thành viên để đăng ký");
        }

        Sinhvien sv = currentStudent(currentUsername);
        checkLeader(nhom, sv);

        // Dang ky lai sau TUCHOI phai giu ID cu.
        if (oldOpt.isPresent()) {
            Long oldId = oldOpt.get().getId();

            if (nopbaocaoRepository.existsByDangkydetaiid(oldId)
                    || ketquaRepository.existsByDangkydetaiid(oldId)) {
                throw new NghiepvuException(
                    "Đăng ký cũ đã có báo cáo hoặc kết quả, "
                    + "không thể đăng ký lại tự động");
            }
        }

        Dangkydetai dk = oldOpt.orElseGet(Dangkydetai::new);

        dk.setNhomid(nhom.getId());
        dk.setDetaiid(detai.getId());
        dk.setTrangthai(TrangthaiDangkydetai.CHODUYET);
        dk.setNgaydangky(LocalDateTime.now());

        try {
            // UNIQUE(Nhomid) duoc kiem tra khi flush.
            return repository.saveAndFlush(dk);
        } catch (DataIntegrityViolationException ex) {
            throw new NghiepvuException(
                "Đăng ký bị trùng hoặc vi phạm ràng buộc dữ liệu");
        }
    }

    // ===== HUY DANG KY =====

    @Override
    public void huyDangky(
            Long dangkydetaiid, String currentUsername) {

        if (dangkydetaiid == null) {
            throw new NghiepvuException(
                "Mã đăng ký không hợp lệ");
        }

        Sinhvien sv = currentStudent(currentUsername);

        Dangkydetai dk = repository
            .findByIdForUpdate(dangkydetaiid)
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy đăng ký đề tài"));

        Nhomsinhvien nhom = nhomsinhvienRepository
            .findById(dk.getNhomid())
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy nhóm sinh viên"));

        checkLeader(nhom, sv);

        Dotdangky dot = dotdangkyRepository
            .findById(nhom.getDotdangkyid())
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy đợt đăng ký"));

        checkRegistrationWindow(dot);

        if (dk.getTrangthai() == TrangthaiDangkydetai.DADUYET) {
            throw new NghiepvuException(
                "Không thể hủy đăng ký đã được duyệt");
        }

        if (dk.getTrangthai() != TrangthaiDangkydetai.CHODUYET
                && dk.getTrangthai() != TrangthaiDangkydetai.TUCHOI) {
            throw new NghiepvuException(
                "Trạng thái đăng ký không cho phép hủy");
        }

        if (nopbaocaoRepository.existsByDangkydetaiid(dangkydetaiid)
                || ketquaRepository.existsByDangkydetaiid(dangkydetaiid)) {
            throw new NghiepvuException(
                "Đăng ký đã có báo cáo hoặc kết quả, không thể hủy");
        }

        repository.delete(dk);
        repository.flush();
    }

    // ===== DUYET DANG KY =====

    @Override
    public Dangkydetai duyetDangky(
            Long dangkydetaiid,
            boolean chapNhan,
            String currentUsername) {

        if (dangkydetaiid == null) {
            throw new NghiepvuException(
                "Mã đăng ký không hợp lệ");
        }

        if (currentUsername == null || currentUsername.isBlank()) {
            throw new AccessDeniedException("Vui lòng đăng nhập");
        }

        Taikhoan account = taikhoanRepository
            .findByTendangnhap(currentUsername)
            .orElseThrow(() -> new AccessDeniedException(
                "Không xác định được tài khoản"));

        Long detaiId = repository
            .findDetaiIdByRegistrationId(dangkydetaiid)
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy đăng ký đề tài"));

        // Khoa de tai tranh hai nhom duoc duyet dong thoi.
        Detai detai = detaiRepository
            .findByIdForUpdate(detaiId)
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy đề tài"));

        boolean authorized =
            account.getVaitro() == Vaitro.TRUONGKHOA;

        if (!authorized && giangvienRepository != null
                && (account.getVaitro() == Vaitro.TRUONGBOMON
                    || account.getVaitro() == Vaitro.GIANGVIEN)) {

            Optional<Giangvien> teacher =
                giangvienRepository.findByTaikhoanid(account.getId());

            if (teacher.isPresent()) {
                Giangvien g = teacher.get();

                if (account.getVaitro() == Vaitro.TRUONGBOMON
                        && Objects.equals(
                            g.getBomonid(), detai.getBomonid())) {
                    authorized = true;
                } else if (Objects.equals(
                        g.getId(), detai.getGiangviendexuatid())) {
                    authorized = true;
                }
            }
        }

        if (!authorized) {
            throw new AccessDeniedException(
                "Không có quyền duyệt đăng ký cho đề tài này");
        }

        Dangkydetai dk = repository
            .findByIdForUpdate(dangkydetaiid)
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy đăng ký đề tài"));

        if (!Objects.equals(detaiId, dk.getDetaiid())) {
            throw new NghiepvuException(
                "Dữ liệu đăng ký không nhất quán");
        }

        if (dk.getTrangthai() != TrangthaiDangkydetai.CHODUYET) {
            throw new NghiepvuException(
                "Chỉ xử lý đăng ký đang chờ duyệt");
        }

        Nhomsinhvien nhom = nhomsinhvienRepository
            .findById(dk.getNhomid())
            .orElseThrow(() -> new NghiepvuException(
                "Không tìm thấy nhóm sinh viên"));

        if (!Objects.equals(
                nhom.getDotdangkyid(), detai.getDotdangkyid())) {
            throw new NghiepvuException(
                "Đề tài và nhóm không cùng đợt đăng ký");
        }

        if (detai.getTrangthai() != TrangthaiDetai.CONGBO) {
            throw new NghiepvuException(
                "Đề tài chưa được công bố");
        }

        if (chapNhan) {
            boolean approvedElsewhere = repository.findApprovedForUpdate(
                    detaiId, TrangthaiDangkydetai.DADUYET)
                .stream()
                .anyMatch(x -> !Objects.equals(
                    x.getId(), dangkydetaiid));

            if (approvedElsewhere) {
                throw new NghiepvuException(
                    "Đề tài đã được duyệt cho nhóm khác");
            }

            // Tao ket qua trung gian neu chua ton tai.
            if (ketquaRepository.findByDangkydetaiid(
                    dangkydetaiid).isEmpty()) {
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
