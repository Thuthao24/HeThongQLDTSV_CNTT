package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.NopBaoCaoDto;
import com.example.quanlydetaisinhvien.dto.NopBaoCaoRequest;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.NopbaocaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class NopbaocaoServiceImpl implements NopbaocaoService {

    private final NopbaocaoRepository repository;
    private final DangkydetaiRepository dangkydetaiRepository;
    private final NhomsinhvienRepository nhomsinhvienRepository;
    private final SinhvienRepository sinhvienRepository;
    private final TaikhoanRepository taikhoanRepository;
    private final DetaiRepository detaiRepository;

    @Autowired(required = false)
    private DiemRepository diemRepository;

    @Autowired(required = false)
    private KetquaRepository ketquaRepository;

    @Autowired(required = false)
    private ThanhviennhomRepository thanhviennhomRepository;

    @Autowired(required = false)
    private DotdangkyRepository dotdangkyRepository;

    public NopbaocaoServiceImpl(
            NopbaocaoRepository repository,
            DangkydetaiRepository dangkydetaiRepository,
            NhomsinhvienRepository nhomsinhvienRepository,
            SinhvienRepository sinhvienRepository,
            TaikhoanRepository taikhoanRepository,
            DetaiRepository detaiRepository) {
        this.repository = repository;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.nhomsinhvienRepository = nhomsinhvienRepository;
        this.sinhvienRepository = sinhvienRepository;
        this.taikhoanRepository = taikhoanRepository;
        this.detaiRepository = detaiRepository;
    }

    @Override
    public List<Nopbaocao> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<Nopbaocao> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * Không cho phép ghi trực tiếp qua giao diện CRUD chung: cách này bỏ qua
     * kiểm tra danh tính, vai trò và trạng thái nghiệp vụ.
     * Gọi các phương thức nghiệp vụ có tham số currentUsername thay thế.
     */
    @Override
    public Nopbaocao save(Nopbaocao entity) {
        throw new NghiepvuException("Không hỗ trợ lưu báo cáo trực tiếp; vui lòng sử dụng luồng nghiệp vụ có kiểm tra quyền");
    }

    @Override
    public void deleteById(Long id) {
        throw new NghiepvuException("Không hỗ trợ xóa báo cáo trực tiếp; vui lòng sử dụng luồng nghiệp vụ có kiểm tra quyền");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NopBaoCaoDto> layDanhSachBaoCaoDto() {
        return layDanhSachBaoCaoDto(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NopBaoCaoDto> layDanhSachBaoCaoDto(int page) {
        List<Nopbaocao> list = repository.findAll(org.springframework.data.domain.PageRequest.of(Math.max(0, page), 50, org.springframework.data.domain.Sort.by("id").descending())).getContent();
        return chuyenDanhSachBaoCaoSangDto(list);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NopBaoCaoDto> layDanhSachBaoCaoChoUser(String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            return new ArrayList<>(); // Không công khai toàn bộ báo cáo nếu thiếu danh tính.
        }
        Optional<Taikhoan> tkOpt = taikhoanRepository.findByTendangnhap(currentUsername);
        if (tkOpt.isPresent() && tkOpt.get().getVaitro() == Vaitro.TRUONGKHOA) {
            return layDanhSachBaoCaoDto();
        }
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty() || thanhviennhomRepository == null) {
            return new ArrayList<>();
        }

        List<Thanhviennhom> tvList = thanhviennhomRepository.findBySinhvienid(svOpt.get().getId());
        Set<Long> nhomIds = tvList.stream().map(Thanhviennhom::getNhomid).collect(Collectors.toSet());
        if (nhomIds.isEmpty()) return new ArrayList<>();
        Set<Long> dangkyIds = dangkydetaiRepository.findByNhomidIn(nhomIds).stream()
                .map(Dangkydetai::getId).collect(Collectors.toSet());
        if (dangkyIds.isEmpty()) return new ArrayList<>();
        return chuyenDanhSachBaoCaoSangDto(repository.findByDangkydetaiidIn(dangkyIds));
    }

    @Override
    @Transactional(readOnly = true)
    public NopBaoCaoDto layBaoCaoMoiNhatCuaDangKy(Long dangkydetaiid) {
        if (dangkydetaiid == null) {
            return null;
        }
        List<Nopbaocao> list = repository.findByDangkydetaiid(dangkydetaiid);
        if (list.isEmpty()) {
            return null;
        }
        return chuyenSangDto(list.get(list.size() - 1));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean kiemTraQuyenTruyCapBaoCao(Long baocaoid, String currentUsername) {
        if (baocaoid == null || currentUsername == null) {
            return false;
        }
        Optional<Taikhoan> tkOpt = taikhoanRepository.findByTendangnhap(currentUsername);
        if (tkOpt.isEmpty()) {
            return false;
        }
        if (tkOpt.get().getVaitro() == Vaitro.TRUONGKHOA) {
            return true;
        }
        if (tkOpt.get().getVaitro() != Vaitro.SINHVIEN) {
            return false;
        }
        Optional<Sinhvien> svOpt = sinhvienRepository.findByMasinhvien(currentUsername);
        if (svOpt.isEmpty() || thanhviennhomRepository == null) {
            return false;
        }
        Optional<Nopbaocao> nbOpt = repository.findById(baocaoid);
        if (nbOpt.isEmpty()) {
            return false;
        }
        Optional<Dangkydetai> dkOpt = dangkydetaiRepository.findById(nbOpt.get().getDangkydetaiid());
        if (dkOpt.isEmpty()) {
            return false;
        }
        return thanhviennhomRepository.existsByNhomidAndSinhvienid(dkOpt.get().getNhomid(), svOpt.get().getId());
    }

    /** Chuyển DTO với dữ liệu liên quan được nạp theo lô, tránh N+1. */
    private List<NopBaoCaoDto> chuyenDanhSachBaoCaoSangDto(List<Nopbaocao> reports) {
        if (reports.isEmpty()) return new ArrayList<>();
        Set<Long> svIds = reports.stream().map(Nopbaocao::getSinhvienid).collect(Collectors.toSet());
        Set<Long> dkIds = reports.stream().map(Nopbaocao::getDangkydetaiid).collect(Collectors.toSet());
        Map<Long, Sinhvien> svs = new HashMap<>();
        sinhvienRepository.findAllById(svIds).forEach(e -> svs.put(e.getId(), e));
        Set<Long> tkIds = svs.values().stream().map(Sinhvien::getTaikhoanid)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Taikhoan> tks = new HashMap<>();
        taikhoanRepository.findAllById(tkIds).forEach(e -> tks.put(e.getId(), e));
        Map<Long, Dangkydetai> dks = new HashMap<>();
        dangkydetaiRepository.findAllById(dkIds).forEach(e -> dks.put(e.getId(), e));
        Set<Long> nhomIds = dks.values().stream().map(Dangkydetai::getNhomid).collect(Collectors.toSet());
        Set<Long> detaiIds = dks.values().stream().map(Dangkydetai::getDetaiid).collect(Collectors.toSet());
        Map<Long, Nhomsinhvien> nhoms = new HashMap<>();
        nhomsinhvienRepository.findAllById(nhomIds).forEach(e -> nhoms.put(e.getId(), e));
        Map<Long, Detai> detais = new HashMap<>();
        detaiRepository.findAllById(detaiIds).forEach(e -> detais.put(e.getId(), e));
        Set<Long> dotIds = nhoms.values().stream().map(Nhomsinhvien::getDotdangkyid).collect(Collectors.toSet());
        Map<Long, Dotdangky> dots = new HashMap<>();
        if (dotdangkyRepository != null) dotdangkyRepository.findAllById(dotIds).forEach(e -> dots.put(e.getId(), e));
        List<NopBaoCaoDto> result = new ArrayList<>();
        for (Nopbaocao nb : reports) {
            NopBaoCaoDto dto = new NopBaoCaoDto();
            dto.setId(nb.getId()); dto.setDangkydetaiid(nb.getDangkydetaiid());
            dto.setSinhvienid(nb.getSinhvienid()); dto.setDuongdanfile(nb.getDuongdanfile());
            dto.setNgaynop(nb.getNgaynop());
            Sinhvien sv = svs.get(nb.getSinhvienid());
            if (sv != null) {
                dto.setMasinhvien(sv.getMasinhvien());
                Taikhoan tk = tks.get(sv.getTaikhoanid());
                if (tk != null) dto.setHoten(tk.getHoten());
            }
            Dangkydetai dk = dks.get(nb.getDangkydetaiid());
            if (dk != null) {
                dto.setNhomid(dk.getNhomid()); dto.setDetaiid(dk.getDetaiid());
                Nhomsinhvien nhom = nhoms.get(dk.getNhomid());
                if (nhom != null) {
                    dto.setTennhom(nhom.getTennhom());
                    Dotdangky dot = dots.get(nhom.getDotdangkyid());
                    if (dot != null) dto.setHanNop(dot.getNgaybaocaohoidong() != null ? dot.getNgaybaocaohoidong() : dot.getHanchamphanbien());
                }
                Detai detai = detais.get(dk.getDetaiid());
                if (detai != null) dto.setTendetai(detai.getTendetai());
            }
            result.add(dto);
        }
        return result;
    }

    private NopBaoCaoDto chuyenSangDto(Nopbaocao nb) {
        NopBaoCaoDto dto = new NopBaoCaoDto();
        dto.setId(nb.getId());
        dto.setDangkydetaiid(nb.getDangkydetaiid());
        dto.setSinhvienid(nb.getSinhvienid());
        dto.setDuongdanfile(nb.getDuongdanfile());
        dto.setNgaynop(nb.getNgaynop());

        sinhvienRepository.findById(nb.getSinhvienid()).ifPresent(sv -> {
            dto.setMasinhvien(sv.getMasinhvien());
            taikhoanRepository.findById(sv.getTaikhoanid()).ifPresent(tk -> dto.setHoten(tk.getHoten()));
        });

        dangkydetaiRepository.findById(nb.getDangkydetaiid()).ifPresent(dk -> {
            dto.setNhomid(dk.getNhomid());
            dto.setDetaiid(dk.getDetaiid());
            nhomsinhvienRepository.findById(dk.getNhomid()).ifPresent(n -> {
                dto.setTennhom(n.getTennhom());
                if (dotdangkyRepository != null) {
                    dotdangkyRepository.findById(n.getDotdangkyid()).ifPresent(d -> dto.setHanNop(d.getNgaybaocaohoidong() != null ? d.getNgaybaocaohoidong() : d.getHanchamphanbien()));
                }
            });
            detaiRepository.findById(dk.getDetaiid()).ifPresent(dt -> dto.setTendetai(dt.getTendetai()));
        });

        return dto;
    }

    @Override
    public Nopbaocao nopBaocao(NopBaoCaoRequest request, String currentUsername) {
        if (request == null || request.getDangkydetaiid() == null) {
            throw new NghiepvuException("Vui lòng chọn đề tài cần nộp báo cáo");
        }

        if (request.getDuongdanfile() == null || request.getDuongdanfile().trim().isEmpty()) {
            throw new NghiepvuException("Đường dẫn file báo cáo không được để trống");
        }

        kiemTraKhoaNopBaoCao(request.getDangkydetaiid());

        Sinhvien sinhvienNop = xacThucNhomTruongNopBaoCao(request.getDangkydetaiid(), currentUsername);

        // Kiểm tra xem đề tài này đã có bản ghi báo cáo chưa
        // Nếu đã có: Cập nhật bản ghi hiện tại (nộp lại / thay thế), không tạo thêm bản ghi trùng lặp
        Nopbaocao nb = repository.findFirstByDangkydetaiidOrderByIdDesc(request.getDangkydetaiid())
                .orElse(new Nopbaocao());

        nb.setDangkydetaiid(request.getDangkydetaiid());
        nb.setSinhvienid(sinhvienNop.getId());
        nb.setDuongdanfile(request.getDuongdanfile().trim());
        nb.setNgaynop(LocalDateTime.now());

        return repository.save(nb);
    }

    @Override
    public Nopbaocao nopBaocaoVoiUpload(Long dangkydetaiid, MultipartFile file, String currentUsername) {
        if (dangkydetaiid == null) {
            throw new NghiepvuException("Vui lòng chọn đề tài cần nộp báo cáo");
        }
        if (file == null || file.isEmpty()) {
            throw new NghiepvuException("Vui lòng chọn file báo cáo để tải lên");
        }

        if (file.getSize() > 20 * 1024 * 1024) {
            throw new NghiepvuException("Dung lượng tệp vượt quá giới hạn 20MB theo quy định");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new NghiepvuException("Tên tệp không hợp lệ");
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!List.of("pdf", "doc", "docx", "zip", "rar").contains(ext)) {
            throw new NghiepvuException("Định dạng tệp không được hỗ trợ. Chỉ chấp nhận các định dạng: .pdf, .doc, .docx, .zip, .rar");
        }

        kiemTraKhoaNopBaoCao(dangkydetaiid);
        kiemTraDinhDangFileVoiMagicBytes(file, ext);

        Sinhvien sinhvienNop = xacThucNhomTruongNopBaoCao(dangkydetaiid, currentUsername);

        String safeName = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;

        Path targetPath = null;
        try {
            Path uploadDir = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            targetPath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String duongdanfile = "/uploads/baocao/" + fileName;

            // Kiểm tra xem đề tài này đã có bản ghi báo cáo chưa
            // Nếu đã có: Cập nhật bản ghi hiện tại (nộp lại / thay thế), không tạo thêm bản ghi trùng lặp
            Nopbaocao nb = repository.findFirstByDangkydetaiidOrderByIdDesc(dangkydetaiid)
                    .orElse(new Nopbaocao());
            String oldFileDuongDan = nb.getId() != null ? nb.getDuongdanfile() : null;

            nb.setDangkydetaiid(dangkydetaiid);
            nb.setSinhvienid(sinhvienNop.getId());
            nb.setDuongdanfile(duongdanfile);
            nb.setNgaynop(LocalDateTime.now());

            Nopbaocao saved = repository.save(nb);

            // Dọn dẹp tệp cũ sau khi lưu CSDL thành công
            if (oldFileDuongDan != null && !oldFileDuongDan.equals(duongdanfile)) {
                xoaFileVatLy(oldFileDuongDan);
            }

            return saved;
        } catch (Exception e) {
            // Dọn dẹp tệp mồ côi nếu lưu CSDL hoặc upload thất bại
            if (targetPath != null) {
                try {
                    Files.deleteIfExists(targetPath);
                } catch (Exception ignored) {
                }
            }
            if (e instanceof NghiepvuException) {
                throw (NghiepvuException) e;
            }
            throw new NghiepvuException("Không thể lưu file tải lên: " + e.getMessage());
        }
    }

    @Override
    public void xoaBaocao(Long id, String currentUsername) {
        Nopbaocao nb = repository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy báo cáo"));

        kiemTraKhoaNopBaoCao(nb.getDangkydetaiid());
        xacThucNhomTruongNopBaoCao(nb.getDangkydetaiid(), currentUsername);

        String oldFileDuongDan = nb.getDuongdanfile();
        repository.deleteById(id);
        xoaFileVatLy(oldFileDuongDan);
    }

    /**
     * Quy tắc nghiệp vụ bắt buộc:
     * - Người chưa đăng nhập không được phép nộp báo cáo.
     * - Phải xác định sinh viên từ tài khoản đang đăng nhập bằng cơ chế xác thực hiện có trong dự án.
     * - Không tin mã sinh viên, mã nhóm hoặc thông tin quyền hạn do client tự gửi lên.
     * - Kiểm tra đề tài của nhóm đã được duyệt chính thức.
     * - Kiểm tra sinh viên thực sự là nhóm trưởng của nhóm tương ứng.
     */
    
    @Override
    @Transactional(readOnly = true)
    public boolean kiemTraDaKhoaNopBaoCao(Long dangkydetaiid) {
        if (dangkydetaiid == null) return false;
        long soLuongDiem = (diemRepository != null) ? diemRepository.demSoLuongDiemTheoDangky(dangkydetaiid) : 0;
        boolean daCongBo = (ketquaRepository != null) && ketquaRepository.existsByDangkydetaiidAndDacongboTrue(dangkydetaiid);
        return (soLuongDiem > 0 || daCongBo);
    }

    private void kiemTraKhoaNopBaoCao(Long dangkydetaiid) {
        if (dangkydetaiid == null) return;
        if (kiemTraDaKhoaNopBaoCao(dangkydetaiid)) {
            throw new NghiepvuException("Đề tài đã có điểm đánh giá hoặc đã công bố kết quả. Chức năng nộp, cập nhật hoặc xóa báo cáo đã bị khóa để bảo toàn dữ liệu!");
        }
    }

    private void kiemTraDinhDangFileVoiMagicBytes(MultipartFile file, String ext) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read < 4) {
                throw new NghiepvuException("Tệp tin rỗng hoặc không đúng định dạng tiêu chuẩn");
            }
            boolean hopLe = false;
            if ("pdf".equals(ext)) {
                hopLe = (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46); // %PDF
            } else if ("docx".equals(ext) || "zip".equals(ext)) {
                hopLe = (header[0] == 0x50 && header[1] == 0x4B && (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07) && (header[3] == 0x04 || header[3] == 0x06 || header[3] == 0x08));
            } else if ("rar".equals(ext)) {
                hopLe = (header[0] == 0x52 && header[1] == 0x61 && header[2] == 0x72 && header[3] == 0x21); // Rar!
            } else if ("doc".equals(ext)) {
                hopLe = (header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF && header[2] == 0x11 && header[3] == (byte) 0xE0)
                        || (header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04);
            }
            if (!hopLe) {
                throw new NghiepvuException("Nội dung tệp không khớp với phần mở rộng ." + ext + ". Vui lòng kiểm tra lại tệp thực tế.");
            }
        } catch (IOException e) {
            throw new NghiepvuException("Không thể đọc và kiểm tra tệp tin: " + e.getMessage());
        }
    }

    private void xoaFileVatLy(String duongdanfile) {
        if (duongdanfile == null || duongdanfile.trim().isEmpty()) return;
        try {
            String prefix = "/uploads/baocao/";
            if (duongdanfile.startsWith(prefix)) {
                String filename = duongdanfile.substring(prefix.length());
                Path uploadDir = Paths.get(uploadDirectory).toAbsolutePath().normalize();
                Path filePath = uploadDir.resolve(filename).normalize();
                if (filePath.startsWith(uploadDir) && Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private Sinhvien xacThucNhomTruongNopBaoCao(Long dangkydetaiid, String currentUsername) {
        if (currentUsername == null || currentUsername.trim().isEmpty()) {
            throw new NghiepvuException("Người chưa đăng nhập không được phép nộp báo cáo");
        }

        Taikhoan tk = taikhoanRepository.findByTendangnhap(currentUsername)
                .orElseThrow(() -> new NghiepvuException("Tài khoản người dùng không tồn tại"));

        Dangkydetai dk = dangkydetaiRepository.findById(dangkydetaiid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin đăng ký đề tài"));

        if (dk.getTrangthai() != null && dk.getTrangthai() != TrangthaiDangkydetai.DADUYET) {
            throw new NghiepvuException("Chỉ được nộp báo cáo khi đề tài của nhóm đã được duyệt chính thức (Trạng thái hiện tại: " + dk.getTrangthai() + ")");
        }

        Nhomsinhvien nhom = nhomsinhvienRepository.findById(dk.getNhomid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin nhóm sinh viên"));

        // Nếu là Trưởng khoa: hỗ trợ thao tác quản trị
        if (tk.getVaitro() == Vaitro.TRUONGKHOA) {
            return sinhvienRepository.findById(nhom.getNhomtruongid())
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy thông tin nhóm trưởng"));
        }

        // Nếu không phải SINHVIEN: từ chối
        if (tk.getVaitro() != Vaitro.SINHVIEN) {
            throw new NghiepvuException("Chỉ sinh viên là nhóm trưởng mới được phép nộp báo cáo");
        }

        // Lấy thông tin sinh viên từ tài khoản đăng nhập (Bảo mật: không tin mã client gửi)
        Sinhvien sv = sinhvienRepository.findByMasinhvien(currentUsername)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy hồ sơ sinh viên tương ứng với tài khoản đăng nhập"));

        // Kiểm tra sinh viên có đúng là nhóm trưởng của nhóm sở hữu đề tài này không
        if (!nhom.getNhomtruongid().equals(sv.getId())) {
            throw new NghiepvuException("Chỉ nhóm trưởng mới được phép nộp báo cáo. Thành viên thường hoặc người ngoài nhóm không được phép.");
        }

        return sv;
    }
    @Value("${app.upload.dir:uploads/baocao}")
        private String uploadDirectory;
}
