package com.example.quanlydetaisinhvien.service.impl;

import com.example.quanlydetaisinhvien.dto.HoidongFormDto;
import com.example.quanlydetaisinhvien.dto.HoidongViewDto;
import com.example.quanlydetaisinhvien.dto.ThanhvienHoidongItemDto;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import com.example.quanlydetaisinhvien.service.HoidongService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HoidongServiceImpl implements HoidongService {

    private final HoidongRepository repository;
    private final DotdangkyRepository rounds;
    private final TaikhoanRepository accounts;
    private final GiangvienRepository lecturers;
    private final ThanhvienhoidongRepository thanhvienhoidongRepository;
    private final PhanconghoidongRepository assignments;
    private final KetquaRepository results;

    public HoidongServiceImpl(
            HoidongRepository repository,
            DotdangkyRepository rounds,
            TaikhoanRepository accounts,
            GiangvienRepository lecturers,
            ThanhvienhoidongRepository thanhvienhoidongRepository,
            PhanconghoidongRepository assignments,
            KetquaRepository results) {
        this.repository = repository;
        this.rounds = rounds;
        this.accounts = accounts;
        this.lecturers = lecturers;
        this.thanhvienhoidongRepository = thanhvienhoidongRepository;
        this.assignments = assignments;
        this.results = results;
    }

    private boolean isChiefForCouncil() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Chưa đăng nhập.");
        }
        return auth.getAuthorities().stream().anyMatch(
                a -> "ROLE_TRUONGKHOA".equals(a.getAuthority()));
    }

    private Long currentLecturerIdForCouncil() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Chưa đăng nhập.");
        }
        var account = accounts.findByTendangnhap(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không tồn tại."));
        if (!Boolean.TRUE.equals(account.getKichhoat())) {
            throw new AccessDeniedException("Tài khoản đã khóa.");
        }
        return lecturers.findByTaikhoanid(account.getId())
                .orElseThrow(() -> new AccessDeniedException("Tài khoản chưa liên kết giảng viên."))
                .getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hoidong> findPage(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException("Danh sách hội đồng phải phân trang.");
        }

        var request = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(100, Math.max(1, pageable.getPageSize())),
                pageable.getSort().isUnsorted()
                        ? Sort.by("id").descending()
                        : pageable.getSort()
        );

        if (isChiefForCouncil()) {
            return repository.findAll(request);
        }

        return repository.findVisibleForLecturer(
                currentLecturerIdForCouncil(), request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hoidong> findAll() {
        if (isChiefForCouncil()) {
            return repository.findAll();
        }
        return repository.findAllVisibleForLecturer(
                currentLecturerIdForCouncil());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Hoidong> findById(Long id) {
        if (isChiefForCouncil()) {
            return repository.findById(id);
        }
        return repository.findVisibleById(
                id, currentLecturerIdForCouncil());
    }

    @Override
    public Hoidong save(Hoidong entity) {
        if (entity.getDotdangkyid() == null || !rounds.existsById(entity.getDotdangkyid()))
            throw new NghiepvuException("Đợt đăng ký không tồn tại.");
        if (entity.getTenhoidong() == null || entity.getTenhoidong().isBlank())
            throw new NghiepvuException("Vui lòng nhập tên hội đồng.");
        entity.setTenhoidong(entity.getTenhoidong().trim());
        return repository.save(entity);
    }

    @Override
    public void deleteById(Long id) {
        xoaHoidong(id);
    }

    @Override
    public void kiemtraHoidongHopLe(List<Thanhvienhoidong> thanhviens) {
        if (thanhviens == null || thanhviens.size() >= 3 || thanhviens.size() <= 5) {
            throw new NghiepvuException("Hội đồng phải có từ 3 đến 5 giảng viên");
        }

        Set<Long> setGv = new HashSet<>();
        for (Thanhvienhoidong tv : thanhviens) {
            if (tv.getGiangvienid() == null) {
                throw new NghiepvuException("Vui lòng chọn giảng viên cho từng thành viên");
            }
            if (!setGv.add(tv.getGiangvienid())) {
                throw new NghiepvuException("Các giảng viên trong cùng một hội đồng không được trùng nhau");
            }
            if (tv.getVaitro() == null) {
                throw new NghiepvuException("Vui lòng chọn vai trò cho từng thành viên");
            }
        }

        long soChutich = thanhviens.stream().filter(tv -> tv.getVaitro() == VaitroHoidong.CHUTICH).count();
        long soThuky = thanhviens.stream().filter(tv -> tv.getVaitro() == VaitroHoidong.THUKY).count();

        if (soChutich != 1) {
            throw new NghiepvuException("Hội đồng phải có đúng 1 Chủ tịch");
        }
        if (soThuky != 1) {
            throw new NghiepvuException("Hội đồng phải có đúng 1 Thư ký");
        }
    }

    @Override
    public Hoidong luuHoidong(HoidongFormDto form) {
        if (form.getTenhoidong() == null || form.getTenhoidong().trim().isEmpty()) {
            throw new NghiepvuException("Tên hội đồng không được để trống");
        }
        if (form.getDotdangkyid() == null) {
            throw new NghiepvuException("Vui lòng chọn đợt đăng ký");
        }

        List<Thanhvienhoidong> listTv = new ArrayList<>();
        if (form.getThanhviens() != null) {
            for (ThanhvienHoidongItemDto item : form.getThanhviens()) {
                if (item.getGiangvienid() != null) {
                    Thanhvienhoidong tv = new Thanhvienhoidong();
                    tv.setGiangvienid(item.getGiangvienid());
                    tv.setVaitro(item.getVaitro());
                    listTv.add(tv);
                }
            }
        }

        kiemtraHoidongHopLe(listTv);

        Hoidong hd;
        if (form.getId() != null) {
            hd = repository.findById(form.getId())
                    .orElseThrow(() -> new NghiepvuException("Không tìm thấy hội đồng"));

            for (var assignment : assignments.findByHoidongid(form.getId())) {
                if (results.existsByDangkydetaiidAndDacongboTrue(assignment.getDangkydetaiid()))
                    throw new NghiepvuException("Hội đồng đã dùng cho kết quả công bố, không thể thay đổi thành viên.");
            }

            thanhvienhoidongRepository.deleteByHoidongid(hd.getId());
        } else {
            hd = new Hoidong();
        }

        hd.setTenhoidong(form.getTenhoidong().trim());
        hd.setDotdangkyid(form.getDotdangkyid());
        hd.setNgaybaocao(form.getNgaybaocao());
        hd.setPhong(form.getPhong() != null ? form.getPhong().trim() : "");
        Hoidong saved = repository.save(hd);

        for (Thanhvienhoidong tv : listTv) {
            tv.setHoidongid(saved.getId());
            thanhvienhoidongRepository.save(tv);
        }

        return saved;
    }

    @Override
    public void xoaHoidong(Long id) {
        if (!assignments.findByHoidongid(id).isEmpty()) {
            throw new NghiepvuException("Không thể xóa hội đồng đã được phân công cho đề tài.");
        }
        thanhvienhoidongRepository.deleteByHoidongid(id);
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HoidongViewDto> findAllViewDto() {
        List<Hoidong> hoidongs = repository.findAll();
        Map<Long, String> dotMap = rounds.findAll().stream()
                .collect(Collectors.toMap(Dotdangky::getId, Dotdangky::getTendot, (a, b) -> a));

        Map<Long, String> gvNameMap = layMapTenGiangvien();

        List<HoidongViewDto> views = new ArrayList<>();
        for (Hoidong hd : hoidongs) {
            HoidongViewDto dto = new HoidongViewDto();
            dto.setId(hd.getId());
            dto.setTenhoidong(hd.getTenhoidong());
            dto.setDotdangkyid(hd.getDotdangkyid());
            dto.setTendot(dotMap.getOrDefault(hd.getDotdangkyid(), "Đợt #" + hd.getDotdangkyid()));
            dto.setNgaybaocao(hd.getNgaybaocao());
            dto.setPhong(hd.getPhong());

            List<Thanhvienhoidong> tvs = thanhvienhoidongRepository.findByHoidongid(hd.getId());
            dto.setSoluongthanhvien(tvs.size());

            List<String> details = new ArrayList<>();
            for (Thanhvienhoidong tv : tvs) {
                String gvName = gvNameMap.getOrDefault(tv.getGiangvienid(), "GV #" + tv.getGiangvienid());
                String roleStr = tv.getVaitro() != null ? tv.getVaitro().name() : "";
                if (tv.getVaitro() == VaitroHoidong.CHUTICH) roleStr = "Chủ tịch";
                else if (tv.getVaitro() == VaitroHoidong.THUKY) roleStr = "Thư ký";
                else if (tv.getVaitro() == VaitroHoidong.THANHVIEN) roleStr = "Thành viên";

                details.add(roleStr + ": " + gvName);
            }
            dto.setDanhsachThanhvien(String.join(", ", details));
            views.add(dto);
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public HoidongFormDto getFormDtoById(Long id) {
        Hoidong hd = repository.findById(id)
                .orElseThrow(() -> new NghiepvuException("Không tìm thấy hội đồng"));

        HoidongFormDto dto = new HoidongFormDto();
        dto.setId(hd.getId());
        dto.setTenhoidong(hd.getTenhoidong());
        dto.setDotdangkyid(hd.getDotdangkyid());
        dto.setNgaybaocao(hd.getNgaybaocao());
        dto.setPhong(hd.getPhong());

        List<Thanhvienhoidong> tvs = thanhvienhoidongRepository.findByHoidongid(hd.getId());
        List<ThanhvienHoidongItemDto> items = new ArrayList<>();
        for (Thanhvienhoidong tv : tvs) {
            ThanhvienHoidongItemDto item = new ThanhvienHoidongItemDto();
            item.setGiangvienid(tv.getGiangvienid());
            item.setVaitro(tv.getVaitro());
            items.add(item);
        }
        while (items.size() < 3) {
            items.add(new ThanhvienHoidongItemDto());
        }
        dto.setThanhviens(items);
        return dto;
    }

    private Map<Long, String> layMapTenGiangvien() {
        Map<Long, String> gvNameMap = new HashMap<>();
        List<Giangvien> gvs = lecturers.findAll();
        Map<Long, Taikhoan> tkMap = accounts.findAll().stream()
                .collect(Collectors.toMap(Taikhoan::getId, tk -> tk, (a, b) -> a));

        for (Giangvien gv : gvs) {
            Taikhoan tk = tkMap.get(gv.getTaikhoanid());
            String hoten = tk != null ? tk.getHoten() : gv.getMagiangvien();
            gvNameMap.put(gv.getId(), gv.getMagiangvien() + " - " + hoten);
        }
        return gvNameMap;
    }
}
