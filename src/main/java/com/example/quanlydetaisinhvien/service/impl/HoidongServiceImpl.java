
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

    // =========================================
    // 1. REPOSITORIES
    // =========================================

    private final HoidongRepository repository;

    private final DotdangkyRepository rounds;

    private final TaikhoanRepository accounts;

    private final GiangvienRepository lecturers;

    private final ThanhvienhoidongRepository
            thanhvienhoidongRepository;

    private final PhanconghoidongRepository assignments;

    private final KetquaRepository results;

    // =========================================
    // 2. CONSTRUCTOR
    // =========================================

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
        this.thanhvienhoidongRepository =
                thanhvienhoidongRepository;
        this.assignments = assignments;
        this.results = results;
    }

    // =========================================
    // 3. KIỂM TRA QUYỀN TRƯỞNG KHOA
    // =========================================

    private boolean isChiefForCouncil() {

        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Chưa đăng nhập."
            );
        }

        return auth.getAuthorities()
                .stream()
                .anyMatch(a ->
                        "ROLE_TRUONGKHOA"
                                .equals(a.getAuthority())
                );
    }

    // =========================================
    // 4. LẤY ID GIẢNG VIÊN ĐANG ĐĂNG NHẬP
    // =========================================

    private Long currentLecturerIdForCouncil() {

        var auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Chưa đăng nhập."
            );
        }

        var account = accounts
                .findByTendangnhap(auth.getName())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Tài khoản không tồn tại."
                        )
                );

        if (!Boolean.TRUE.equals(
                account.getKichhoat())) {

            throw new AccessDeniedException(
                    "Tài khoản đã khóa."
            );
        }

        return lecturers
                .findByTaikhoanid(account.getId())
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Tài khoản chưa liên kết giảng viên."
                        )
                )
                .getId();
    }

    // =========================================
    // 5. DANH SÁCH HỘI ĐỒNG CÓ PHÂN TRANG
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public Page<Hoidong> findPage(Pageable pageable) {

        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "Danh sách hội đồng phải phân trang."
            );
        }

        var request = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(
                        100,
                        Math.max(1, pageable.getPageSize())
                ),
                pageable.getSort().isUnsorted()
                        ? Sort.by("id").descending()
                        : pageable.getSort()
        );

        // Trưởng khoa xem tất cả hội đồng
        if (isChiefForCouncil()) {
            return repository.findAll(request);
        }

        // Giảng viên xem hội đồng được tham gia
        return repository.findVisibleForLecturer(
                currentLecturerIdForCouncil(),
                request
        );
    }

    // =========================================
    // 6. LẤY TẤT CẢ HỘI ĐỒNG THEO QUYỀN
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public List<Hoidong> findAll() {

        if (isChiefForCouncil()) {
            return repository.findAll();
        }

        return repository.findAllVisibleForLecturer(
                currentLecturerIdForCouncil()
        );
    }

    // =========================================
    // 7. TÌM HỘI ĐỒNG THEO ID
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public Optional<Hoidong> findById(Long id) {

        if (isChiefForCouncil()) {
            return repository.findById(id);
        }

        return repository.findVisibleById(
                id,
                currentLecturerIdForCouncil()
        );
    }

    // =========================================
    // 8. LƯU THÔNG TIN CƠ BẢN HỘI ĐỒNG
    // =========================================

    @Override
    public Hoidong save(Hoidong entity) {

        if (entity == null) {
            throw new NghiepvuException(
                    "Dữ liệu hội đồng không hợp lệ."
            );
        }

        if (entity.getDotdangkyid() == null
                || !rounds.existsById(
                        entity.getDotdangkyid())) {

            throw new NghiepvuException(
                    "Đợt đăng ký không tồn tại."
            );
        }

        if (entity.getTenhoidong() == null
                || entity.getTenhoidong().isBlank()) {

            throw new NghiepvuException(
                    "Vui lòng nhập tên hội đồng."
            );
        }

        entity.setTenhoidong(
                entity.getTenhoidong().trim()
        );

        return repository.save(entity);
    }

    // =========================================
    // 9. XÓA HỘI ĐỒNG
    // =========================================

    @Override
    public void deleteById(Long id) {
        xoaHoidong(id);
    }

    // =========================================
    // 10. KIỂM TRA DANH SÁCH THÀNH VIÊN
    // =========================================

    @Override
    public void kiemtraHoidongHopLe(
            List<Thanhvienhoidong> thanhviens) {

        // Chỉ chấp nhận từ 3 đến 5 thành viên
        if (thanhviens == null
                || thanhviens.size() < 3
                || thanhviens.size() > 5) {

            throw new NghiepvuException(
                    "Hội đồng phải có từ 3 đến 5 giảng viên"
            );
        }

        Set<Long> danhSachId = new HashSet<>();

        int soChutich = 0;
        int soThuky = 0;

        for (Thanhvienhoidong tv : thanhviens) {

            // Không được có dòng thành viên null
            if (tv == null) {
                throw new NghiepvuException(
                        "Dữ liệu thành viên không hợp lệ"
                );
            }

            // Phải chọn giảng viên
            if (tv.getGiangvienid() == null) {
                throw new NghiepvuException(
                        "Vui lòng chọn giảng viên cho từng thành viên"
                );
            }

            Long giangvienId = tv.getGiangvienid();

            // Không được chọn cùng giảng viên hai lần
            if (!danhSachId.add(giangvienId)) {
                throw new NghiepvuException(
                        "Các giảng viên trong hội đồng không được trùng nhau"
                );
            }

            // Kiểm tra giảng viên tồn tại
            if (!lecturers.existsById(giangvienId)) {
                throw new NghiepvuException(
                        "Giảng viên không tồn tại: "
                                + giangvienId
                );
            }

            // Phải có vai trò
            if (tv.getVaitro() == null) {
                throw new NghiepvuException(
                        "Vui lòng chọn vai trò cho từng thành viên"
                );
            }

            // Đếm Chủ tịch và Thư ký
            if (tv.getVaitro() == VaitroHoidong.CHUTICH) {
                soChutich++;
            }

            if (tv.getVaitro() == VaitroHoidong.THUKY) {
                soThuky++;
            }
        }

        // Hội đồng chỉ được có 1 Chủ tịch
        if (soChutich != 1) {
            throw new NghiepvuException(
                    "Hội đồng phải có đúng 1 Chủ tịch"
            );
        }

        // Hội đồng chỉ được có 1 Thư ký
        if (soThuky != 1) {
            throw new NghiepvuException(
                    "Hội đồng phải có đúng 1 Thư ký"
            );
        }
    }

    // =========================================
    // 11. THÊM MỚI / CHỈNH SỬA HỘI ĐỒNG
    // =========================================

    @Override
    public Hoidong luuHoidong(HoidongFormDto form) {

        // Kiểm tra form
        if (form == null) {
            throw new NghiepvuException(
                    "Dữ liệu hội đồng không hợp lệ"
            );
        }

        // Kiểm tra tên hội đồng
        if (form.getTenhoidong() == null
                || form.getTenhoidong().isBlank()) {

            throw new NghiepvuException(
                    "Tên hội đồng không được để trống"
            );
        }

        // Kiểm tra đợt đăng ký
        if (form.getDotdangkyid() == null) {
            throw new NghiepvuException(
                    "Vui lòng chọn đợt đăng ký"
            );
        }

        if (!rounds.existsById(form.getDotdangkyid())) {
            throw new NghiepvuException(
                    "Đợt đăng ký không tồn tại"
            );
        }

        // =====================================
        // CHUYỂN DTO THÀNH DANH SÁCH THÀNH VIÊN
        // =====================================

        if (form.getThanhviens() == null) {
            throw new NghiepvuException(
                    "Vui lòng nhập danh sách thành viên hội đồng"
            );
        }

        List<Thanhvienhoidong> listTv = new ArrayList<>();

        for (ThanhvienHoidongItemDto item
                : form.getThanhviens()) {

            // Không được bỏ trống giảng viên
            if (item == null
                    || item.getGiangvienid() == null) {

                throw new NghiepvuException(
                        "Vui lòng chọn đầy đủ giảng viên"
                );
            }

            // Không được bỏ trống vai trò
            if (item.getVaitro() == null) {

                throw new NghiepvuException(
                        "Vui lòng chọn vai trò cho từng giảng viên"
                );
            }

            Thanhvienhoidong tv =
                    new Thanhvienhoidong();

            tv.setGiangvienid(
                    item.getGiangvienid()
            );

            tv.setVaitro(
                    item.getVaitro()
            );

            listTv.add(tv);
        }

        // Kiểm tra 3-5 người, không trùng,
        // đúng 1 Chủ tịch và 1 Thư ký
        kiemtraHoidongHopLe(listTv);

        // =====================================
        // XÁC ĐỊNH TẠO MỚI HAY CHỈNH SỬA
        // =====================================

        Hoidong hd;

        if (form.getId() != null) {

            hd = repository.findById(
                    form.getId()
            ).orElseThrow(() ->
                    new NghiepvuException(
                            "Không tìm thấy hội đồng"
                    )
            );

            // Chặn thay đổi hội đồng đã có
            // kết quả được công bố
            for (var assignment
                    : assignments.findByHoidongid(
                            form.getId())) {

                boolean daCongBo =
                        results
                                .existsByDangkydetaiidAndDacongboTrue(
                                        assignment.getDangkydetaiid()
                                );

                if (daCongBo) {
                    throw new NghiepvuException(
                            "Hội đồng đã dùng cho kết quả công bố, không thể thay đổi thành viên."
                    );
                }
            }

            // Xóa thành viên cũ để lưu lại
            // danh sách mới trong cùng giao dịch
            thanhvienhoidongRepository
                    .deleteByHoidongid(hd.getId());

        } else {

            // Tạo hội đồng mới
            hd = new Hoidong();
        }

        // =====================================
        // CẬP NHẬT THÔNG TIN HỘI ĐỒNG
        // =====================================

        hd.setTenhoidong(
                form.getTenhoidong().trim()
        );

        hd.setDotdangkyid(
                form.getDotdangkyid()
        );

        hd.setNgaybaocao(
                form.getNgaybaocao()
        );

        hd.setPhong(
                form.getPhong() != null
                        ? form.getPhong().trim()
                        : ""
        );

        // Lưu hội đồng trước để lấy ID
        Hoidong saved = repository.save(hd);

        // =====================================
        // LƯU DANH SÁCH THÀNH VIÊN
        // =====================================

        for (Thanhvienhoidong tv : listTv) {

            tv.setHoidongid(
                    saved.getId()
            );

            thanhvienhoidongRepository.save(tv);
        }

        return saved;
    }

    // =========================================
    // 12. XÓA HỘI ĐỒNG CÓ RÀNG BUỘC
    // =========================================

    @Override
    public void xoaHoidong(Long id) {

        if (id == null || !repository.existsById(id)) {
            throw new NghiepvuException(
                    "Hội đồng không tồn tại"
            );
        }

        // Không cho xóa hội đồng đã phân công
        if (!assignments.findByHoidongid(id).isEmpty()) {

            throw new NghiepvuException(
                    "Không thể xóa hội đồng đã được phân công cho đề tài."
            );
        }

        // Xóa thành viên trước, hội đồng sau
        thanhvienhoidongRepository
                .deleteByHoidongid(id);

        repository.deleteById(id);
    }

    // =========================================
    // 13. DANH SÁCH HỘI ĐỒNG KÈM THÀNH VIÊN
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public List<HoidongViewDto> findAllViewDto() {

        List<Hoidong> hoidongs =
                findAll();

        Map<Long, String> dotMap =
                rounds.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                Dotdangky::getId,
                                Dotdangky::getTendot,
                                (a, b) -> a
                        ));

        Map<Long, String> gvNameMap =
                layMapTenGiangvien();

        List<HoidongViewDto> views =
                new ArrayList<>();

        for (Hoidong hd : hoidongs) {

            HoidongViewDto dto =
                    new HoidongViewDto();

            dto.setId(hd.getId());

            dto.setTenhoidong(
                    hd.getTenhoidong()
            );

            dto.setDotdangkyid(
                    hd.getDotdangkyid()
            );

            dto.setTendot(
                    dotMap.getOrDefault(
                            hd.getDotdangkyid(),
                            "Đợt #" + hd.getDotdangkyid()
                    )
            );

            dto.setNgaybaocao(
                    hd.getNgaybaocao()
            );

            dto.setPhong(
                    hd.getPhong()
            );

            // Lấy thành viên hội đồng
            List<Thanhvienhoidong> tvs =
                    thanhvienhoidongRepository
                            .findByHoidongid(hd.getId());

            dto.setSoluongthanhvien(
                    tvs.size()
            );

            List<String> details =
                    new ArrayList<>();

            for (Thanhvienhoidong tv : tvs) {

                String gvName =
                        gvNameMap.getOrDefault(
                                tv.getGiangvienid(),
                                "GV #" + tv.getGiangvienid()
                        );

                String roleStr = "Chưa xác định";

                if (tv.getVaitro()
                        == VaitroHoidong.CHUTICH) {

                    roleStr = "Chủ tịch";

                } else if (tv.getVaitro()
                        == VaitroHoidong.THUKY) {

                    roleStr = "Thư ký";

                } else if (tv.getVaitro()
                        == VaitroHoidong.THANHVIEN) {

                    roleStr = "Thành viên";
                }

                details.add(
                        roleStr + ": " + gvName
                );
            }

            dto.setDanhsachThanhvien(
                    String.join(", ", details)
            );

            views.add(dto);
        }

        return views;
    }

    // =========================================
    // 14. LẤY DỮ LIỆU HỘI ĐỒNG ĐỂ CHỈNH SỬA
    // =========================================

    @Override
    @Transactional(readOnly = true)
    public HoidongFormDto getFormDtoById(Long id) {

        Hoidong hd = repository.findById(id)
                .orElseThrow(() ->
                        new NghiepvuException(
                                "Không tìm thấy hội đồng"
                        )
                );

        HoidongFormDto dto =
                new HoidongFormDto();

        dto.setId(hd.getId());

        dto.setTenhoidong(
                hd.getTenhoidong()
        );

        dto.setDotdangkyid(
                hd.getDotdangkyid()
        );

        dto.setNgaybaocao(
                hd.getNgaybaocao()
        );

        dto.setPhong(
                hd.getPhong()
        );

        // Lấy danh sách thành viên hiện tại
        List<Thanhvienhoidong> tvs =
                thanhvienhoidongRepository
                        .findByHoidongid(hd.getId());

        List<ThanhvienHoidongItemDto> items =
                new ArrayList<>();

        for (Thanhvienhoidong tv : tvs) {

            ThanhvienHoidongItemDto item =
                    new ThanhvienHoidongItemDto();

            item.setGiangvienid(
                    tv.getGiangvienid()
            );

            item.setVaitro(
                    tv.getVaitro()
            );

            items.add(item);
        }

        // Nếu ít hơn 3 dòng, bổ sung cho giao diện
        while (items.size() < 3) {

            ThanhvienHoidongItemDto item =
                    new ThanhvienHoidongItemDto();

            // Gán vai trò mặc định cho dòng trống
            int index = items.size();

            if (index == 0) {
                item.setVaitro(
                        VaitroHoidong.CHUTICH
                );

            } else if (index == 1) {
                item.setVaitro(
                        VaitroHoidong.THUKY
                );

            } else {
                item.setVaitro(
                        VaitroHoidong.THANHVIEN
                );
            }

            items.add(item);
        }

        dto.setThanhviens(items);

        return dto;
    }

    // =========================================
    // 15. MAP MÃ GV THÀNH HỌ TÊN GIẢNG VIÊN
    // =========================================

    private Map<Long, String> layMapTenGiangvien() {

        Map<Long, String> gvNameMap =
                new HashMap<>();

        List<Giangvien> gvs =
                lecturers.findAll();

        Map<Long, Taikhoan> tkMap =
                accounts.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                Taikhoan::getId,
                                tk -> tk,
                                (a, b) -> a
                        ));

        for (Giangvien gv : gvs) {

            Taikhoan tk =
                    tkMap.get(
                            gv.getTaikhoanid()
                    );

            String hoten =
                    tk != null
                            ? tk.getHoten()
                            : gv.getMagiangvien();

            gvNameMap.put(
                    gv.getId(),
                    gv.getMagiangvien()
                            + " - "
                            + hoten
            );
        }

        return gvNameMap;
    }
}
