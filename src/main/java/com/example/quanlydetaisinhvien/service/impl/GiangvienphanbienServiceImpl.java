package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.GiangvienphanbienService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class GiangvienphanbienServiceImpl implements GiangvienphanbienService {

    private final GiangvienphanbienRepository repository;
    private final DangkydetaiRepository dangkydetaiRepository;
    private final DetaiRepository detaiRepository;
    private final DotdangkyRepository dotdangkyRepository;
    private final GiangvienhuongdanRepository giangvienhuongdanRepository;
    private final DiemRepository diemRepository;
    private final GiangvienRepository giangvienRepository;
    private final TaikhoanRepository taikhoanRepository;

    public GiangvienphanbienServiceImpl(
            GiangvienphanbienRepository repository,
            DangkydetaiRepository dangkydetaiRepository,
            DetaiRepository detaiRepository,
            DotdangkyRepository dotdangkyRepository,
            GiangvienhuongdanRepository giangvienhuongdanRepository,
            DiemRepository diemRepository,
            GiangvienRepository giangvienRepository,
            TaikhoanRepository taikhoanRepository) {
        this.repository = repository;
        this.dangkydetaiRepository = dangkydetaiRepository;
        this.detaiRepository = detaiRepository;
        this.dotdangkyRepository = dotdangkyRepository;
        this.giangvienhuongdanRepository = giangvienhuongdanRepository;
        this.diemRepository = diemRepository;
        this.giangvienRepository = giangvienRepository;
        this.taikhoanRepository = taikhoanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Giangvienphanbien> findAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Giangvienphanbien> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Giangvienphanbien> findByDangkydetaiid(Long dangkydetaiid) {
        return repository.findByDangkydetaiid(dangkydetaiid);
    }

    @Override
    public Giangvienphanbien save(Giangvienphanbien entity) {
        if (entity == null || entity.getDangkydetaiid() == null || entity.getGiangvienid() == null) {
            throw new NghiepvuException("Thiếu thông tin đăng ký đề tài hoặc giảng viên phản biện.");
        }
        return phanCong(entity.getDangkydetaiid(), entity.getGiangvienid(), null);
    }

    @Override
    public void deleteById(Long id) {
        Giangvienphanbien pb = repository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy phân công phản biện cần xóa."));
        checkPermission(null);
        // Quy tắc: Không được đổi/xóa GVPB sau khi giảng viên này đã chấm điểm
        if (diemRepository.existsByDangkydetaiidAndGiangvienid(pb.getDangkydetaiid(), pb.getGiangvienid())) {
            throw new NghiepvuException("Không thể xóa hoặc thay đổi giảng viên phản biện đã hoàn thành chấm điểm cho đề tài.");
        }
        repository.deleteById(id);
    }

    @Override
    public Giangvienphanbien phanCong(Long dangkydetaiid, Long giangvienid, String currentUsername) {
        checkPermission(currentUsername);

        if (dangkydetaiid == null || giangvienid == null) {
            throw new NghiepvuException("Thiếu thông tin đăng ký đề tài hoặc giảng viên phản biện.");
        }

        // 1. Đúng đăng ký đề tài: phải tồn tại và đã được duyệt chính thức (DADUYET)
        Dangkydetai dk = dangkydetaiRepository.findById(dangkydetaiid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đăng ký đề tài."));
        if (dk.getTrangthai() != TrangthaiDangkydetai.DADUYET) {
            throw new NghiepvuException("Chỉ được phân công phản biện cho đề tài đã được duyệt chính thức.");
        }

        // 2. Đúng đợt đăng ký: đợt phải hợp lệ, không ở trạng thái hủy
        Detai dt = detaiRepository.findById(dk.getDetaiid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đề tài."));
        Dotdangky dot = dotdangkyRepository.findById(dt.getDotdangkyid())
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy đợt đăng ký."));
        if (dot.getTrangthai() == TrangthaiDotdangky.NHAP || dot.getTrangthai() == TrangthaiDotdangky.DONG) {
            throw new NghiepvuException("Đợt đăng ký chưa mở hoặc đã đóng, không thể phân công phản biện.");
        }

        // 3. Giảng viên phản biện hợp lệ
        Giangvien gv = giangvienRepository.findById(giangvienid)
                .orElseThrow(() -> new NghiepvuException("Giảng viên phản biện không tồn tại."));
        Taikhoan tk = taikhoanRepository.findById(gv.getTaikhoanid())
                .orElseThrow(() -> new NghiepvuException("Tài khoản giảng viên không tồn tại."));
        if (!Boolean.TRUE.equals(tk.getKichhoat())) {
            throw new NghiepvuException("Tài khoản giảng viên đã bị vô hiệu hóa.");
        }

        // 4. Loại GVHD: Giảng viên hướng dẫn không được làm GVPB cho chính đề tài mình hướng dẫn
        if (giangvienhuongdanRepository.existsByDetaiidAndGiangvienid(dt.getId(), giangvienid)) {
            throw new NghiepvuException("Giảng viên hướng dẫn không được làm giảng viên phản biện cho đề tài này.");
        }

        // 5. Chặn trùng: Không phân công trùng 1 giảng viên 2 lần cho cùng 1 đề tài
        if (repository.existsByDangkydetaiidAndGiangvienid(dangkydetaiid, giangvienid)) {
            throw new NghiepvuException("Giảng viên này đã được phân công phản biện cho đề tài này rồi!");
        }

        Giangvienphanbien pb = new Giangvienphanbien();
        pb.setDangkydetaiid(dangkydetaiid);
        pb.setGiangvienid(giangvienid);
        return repository.save(pb);
    }

    @Override
    public void xoaPhanCong(Long dangkydetaiid, Long giangvienid, String currentUsername) {
        checkPermission(currentUsername);

        Giangvienphanbien pb = repository.findByDangkydetaiidAndGiangvienid(dangkydetaiid, giangvienid)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy phân công phản biện của giảng viên này."));

        // Quy tắc: Không được đổi/xóa GVPB sau khi giảng viên này đã chấm điểm
        if (diemRepository.existsByDangkydetaiidAndGiangvienid(dangkydetaiid, giangvienid)) {
            throw new NghiepvuException("Không thể xóa hoặc thay đổi giảng viên phản biện đã hoàn thành chấm điểm cho đề tài.");
        }

        repository.delete(pb);
    }

    private void checkPermission(String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            if (username != null) {
                Taikhoan tk = taikhoanRepository.findByTendangnhap(username).orElse(null);
                if (tk != null && (tk.getVaitro() == Vaitro.TRUONGKHOA || tk.getVaitro() == Vaitro.TRUONGBOMON)) {
                    return;
                }
            }
            throw new AccessDeniedException("Chưa đăng nhập hoặc không đủ thẩm quyền.");
        }
        boolean hasPermission = auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_TRUONGKHOA".equals(a.getAuthority()) || "ROLE_TRUONGBOMON".equals(a.getAuthority())
        );
        if (!hasPermission) {
            throw new AccessDeniedException("Chỉ Trưởng khoa hoặc Trưởng bộ môn mới có quyền phân công giảng viên phản biện.");
        }
    }
}
