
package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.GiaiDoan1.DeTaiForm;
import com.example.quanlydetaisinhvien.dto.GiaiDoan1.DotForm;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class GiaiDoan1Service {

    private final DotdangkyRepository dots;
    private final DetaiRepository detais;
    private final GiangvienRepository gv;
    private final GiangvienhuongdanRepository huongdans;
    private final BomonRepository bomons;
    private final TaikhoanRepository taikhoans;

    public GiaiDoan1Service(
            DotdangkyRepository dots,
            DetaiRepository detais,
            GiangvienRepository gv,
            GiangvienhuongdanRepository huongdans,
            BomonRepository bomons,
            TaikhoanRepository taikhoans) {
        this.dots = dots;
        this.detais = detais;
        this.gv = gv;
        this.huongdans = huongdans;
        this.bomons = bomons;
        this.taikhoans = taikhoans;
    }

    // ===== TRA CUU =====

    public Dotdangky dot(Long id) {
        return dots.findById(id).orElseThrow(
            () -> new NghiepvuException("Không tìm thấy đợt đăng ký"));
    }

    private Dotdangky lockDot(Long id) {
        return dots.findByIdForUpdate(id).orElseThrow(
            () -> new NghiepvuException("Không tìm thấy đợt đăng ký"));
    }

    public Detai detai(Long id) {
        return detais.findById(id).orElseThrow(
            () -> new NghiepvuException("Không tìm thấy đề tài"));
    }

    public List<Dotdangky> dsDot() {
        return dots.findAll();
    }

    public List<Dotdangky> dsDotGiaiDoan1() {
        return dots.findByTrangthaiIn(
            List.of(TrangthaiDotdangky.GIAIDOAN1));
    }

    public List<Detai> dsDetai() {
        return detais.findAll();
    }

    public List<Giangvienhuongdan> dsGvhd(Long id) {
        return huongdans.findByDetaiid(id);
    }

    public List<Giangvien> dsGiangVien() {
        return gv.findAll();
    }

    @Transactional(readOnly = true)
    public List<Giangvien> dsGiangVienChoices() {
        return gv.findAll(
            org.springframework.data.domain.PageRequest.of(
                0, 200,
                org.springframework.data.domain.Sort.by("magiangvien")
            )
        ).getContent();
    }

    @Transactional(readOnly = true)
    public List<Dotdangky> dsDotChoices() {
        return dots.findAll(
            org.springframework.data.domain.PageRequest.of(
                0, 100,
                org.springframework.data.domain.Sort.by("id").descending()
            )
        ).getContent();
    }

    public List<Bomon> dsBoMon() {
        return bomons.findAll();
    }

    // ===== PHAN QUYEN =====

    private Taikhoan user(String username) {
        if (username == null || username.isBlank()) {
            throw new AccessDeniedException("Vui lòng đăng nhập");
        }
        return taikhoans.findByTendangnhap(username)
            .orElseThrow(() -> new AccessDeniedException(
                "Không xác định được tài khoản"));
    }

    private boolean isTeacher(Vaitro role) {
        return role == Vaitro.GIANGVIEN
            || role == Vaitro.TRUONGBOMON
            || role == Vaitro.TRUONGKHOA;
    }

    private void khoa(String username) {
        if (user(username).getVaitro() != Vaitro.TRUONGKHOA) {
            throw new AccessDeniedException(
                "Chỉ trưởng khoa được thực hiện");
        }
    }

    private Giangvien currentTeacher(String username) {
        Taikhoan account = user(username);
        if (!isTeacher(account.getVaitro())) {
            throw new AccessDeniedException(
                "Chỉ giảng viên được đề xuất đề tài");
        }

        return gv.findByTaikhoanid(account.getId())
            .orElseThrow(() -> new AccessDeniedException(
                "Tài khoản chưa liên kết giảng viên"));
    }

    public boolean canPropose(String username) {
        if (username == null || username.isBlank()) return false;
        Taikhoan account = user(username);
        return isTeacher(account.getVaitro())
            && gv.findByTaikhoanid(account.getId()).isPresent();
    }

    public boolean canEditTopic(String username, Detai topic) {
        if (topic == null || topic.getTrangthai() == null) {
            return false;
        }

        if (topic.getTrangthai() != TrangthaiDetai.CHODUYET
                && topic.getTrangthai() != TrangthaiDetai.TUCHOI) {
            return false;
        }

        Taikhoan account = user(username);
        if (account.getVaitro() == Vaitro.TRUONGKHOA) {
            return true;
        }

        if (!isTeacher(account.getVaitro())) return false;

        return gv.findByTaikhoanid(account.getId())
            .map(teacher -> Objects.equals(
                teacher.getId(), topic.getGiangviendexuatid()))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public Set<Long> editableTopicIds(
            String username, List<Detai> topics) {

        Set<Long> allowed = new HashSet<>();
        if (topics == null || topics.isEmpty()) return allowed;

        Taikhoan account = user(username);
        boolean isDean = account.getVaitro() == Vaitro.TRUONGKHOA;

        Long teacherId = null;
        if (!isDean && isTeacher(account.getVaitro())) {
            teacherId = gv.findByTaikhoanid(account.getId())
                .map(Giangvien::getId).orElse(null);
        }

        for (Detai topic : topics) {
            if (topic == null) continue;

            if (topic.getTrangthai() != TrangthaiDetai.CHODUYET
                    && topic.getTrangthai() != TrangthaiDetai.TUCHOI) {
                continue;
            }

            if (isDean || (teacherId != null
                    && Objects.equals(
                        teacherId, topic.getGiangviendexuatid()))) {
                allowed.add(topic.getId());
            }
        }
        return allowed;
    }

    // ===== KIEM TRA DOT DANG KY =====

    private void validTimes(DotForm f) {
        if (f == null || f.getTendot() == null
                || f.getTendot().trim().isEmpty()
                || f.getTendot().trim().length() > 200
                || f.getLoaidot() == null) {
            throw new NghiepvuException(
                "Tên đợt hoặc loại đợt không hợp lệ");
        }

        LocalDateTime g1 = f.getBatdaugiangvien();
        LocalDateTime g2 = f.getKetthucgiangvien();
        LocalDateTime s1 = f.getBatdausinhvien();
        LocalDateTime s2 = f.getKetthucsinhvien();

        if (g1 == null || g2 == null || s1 == null || s2 == null) {
            throw new NghiepvuException(
                "Vui lòng nhập đầy đủ bốn mốc thời gian");
        }

        if (!g1.isBefore(g2) || !s1.isBefore(s2)
                || s1.isBefore(g2)) {
            throw new NghiepvuException(
                "Thứ tự đúng: GV bắt đầu < GV kết thúc "
                + "<= SV bắt đầu < SV kết thúc");
        }

        Loaidot loai = f.getLoaidot();
        LocalDateTime hanPB = f.getHanchamphanbien();
        LocalDateTime ngayHD = f.getNgaybaocaohoidong();

        if ((loai == Loaidot.TLCN || loai == Loaidot.KLTN)
                && hanPB == null) {
            throw new NghiepvuException(
                "TLCN và KLTN phải nhập hạn chấm phản biện");
        }

        if (loai == Loaidot.KLTN && ngayHD == null) {
            throw new NghiepvuException(
                "KLTN phải nhập ngày báo cáo hội đồng");
        }

        if ((loai == Loaidot.MONHOC || loai == Loaidot.NCKH)
                && hanPB != null) {
            throw new NghiepvuException(
                "MONHOC và NCKH không có hạn chấm phản biện");
        }

        if (loai != Loaidot.KLTN && ngayHD != null) {
            throw new NghiepvuException(
                "Chỉ KLTN có ngày báo cáo hội đồng");
        }

        if (hanPB != null && !hanPB.isAfter(s2)) {
            throw new NghiepvuException(
                "Hạn chấm phản biện phải sau hạn đăng ký sinh viên");
        }

        if (ngayHD != null && hanPB != null
                && ngayHD.isBefore(hanPB)) {
            throw new NghiepvuException(
                "Ngày báo cáo hội đồng không được trước hạn phản biện");
        }
    }

    // ===== CRUD DOT =====

    public Dotdangky saveDot(Long id, DotForm f, String username) {
        khoa(username);
        validTimes(f);

        Dotdangky d = id == null
            ? new Dotdangky()
            : lockDot(id);

        if (id != null && d.getTrangthai() != TrangthaiDotdangky.NHAP) {
            throw new NghiepvuException(
                "Chỉ sửa đợt ở trạng thái NHÁP");
        }

        d.setTendot(f.getTendot().trim());
        d.setLoaidot(f.getLoaidot());
        d.setBatdaugiangvien(f.getBatdaugiangvien());
        d.setKetthucgiangvien(f.getKetthucgiangvien());
        d.setBatdausinhvien(f.getBatdausinhvien());
        d.setKetthucsinhvien(f.getKetthucsinhvien());
        d.setHanchamphanbien(f.getHanchamphanbien());
        d.setNgaybaocaohoidong(f.getNgaybaocaohoidong());

        if (id == null) {
            d.setTrangthai(TrangthaiDotdangky.NHAP);
        }

        return dots.save(d);
    }

    public void deleteDot(Long id, String username) {
        khoa(username);
        Dotdangky d = lockDot(id);

        if (d.getTrangthai() != TrangthaiDotdangky.NHAP
                || detais.existsByDotdangkyid(id)) {
            throw new NghiepvuException(
                "Chỉ xóa đợt NHÁP chưa có đề tài");
        }

        dots.delete(d);
        dots.flush();
    }

    // ===== CHUYEN GIAI DOAN =====

    public Dotdangky chuyenGiaiDoan(
            Long id, TrangthaiDotdangky next, String username) {

        khoa(username);
        if (next == null) {
            throw new NghiepvuException(
                "Chưa chọn trạng thái tiếp theo");
        }

        Dotdangky d = lockDot(id);
        TrangthaiDotdangky old = d.getTrangthai();
        LocalDateTime now = LocalDateTime.now();

        if (old == TrangthaiDotdangky.NHAP
                && next == TrangthaiDotdangky.GIAIDOAN1) {

            if (d.getKetthucgiangvien() == null
                    || !now.isBefore(d.getKetthucgiangvien())) {
                throw new NghiepvuException(
                    "Đã hết hạn đăng ký giảng viên");
            }

        } else if (old == TrangthaiDotdangky.GIAIDOAN1
                && next == TrangthaiDotdangky.CONGBO) {

            if (!detais.existsByDotdangkyidAndTrangthai(
                    id, TrangthaiDetai.CONGBO)) {
                throw new NghiepvuException(
                    "Cần ít nhất một đề tài đã công bố");
            }

            if (detais.existsByDotdangkyidAndTrangthai(
                    id, TrangthaiDetai.CHODUYET)) {
                throw new NghiepvuException(
                    "Còn đề tài đang chờ duyệt");
            }

        } else if (old == TrangthaiDotdangky.CONGBO
                && next == TrangthaiDotdangky.GIAIDOAN2) {

            if (d.getBatdausinhvien() == null
                    || d.getKetthucsinhvien() == null
                    || now.isBefore(d.getBatdausinhvien())
                    || !now.isBefore(d.getKetthucsinhvien())) {
                throw new NghiepvuException(
                    "Chỉ mở GĐ2 trong thời gian đăng ký sinh viên");
            }

        } else if (old == TrangthaiDotdangky.GIAIDOAN2
                && next == TrangthaiDotdangky.DONG) {
            // Truong khoa chu dong dong dot.
        } else {
            throw new NghiepvuException(
                "Chuyển trạng thái không hợp lệ: "
                + old + " -> " + next);
        }

        d.setTrangthai(next);
        return dots.save(d);
    }

    // ===== DE XUAT DE TAI =====

    private void activeProposal(Dotdangky d) {
        if (d.getTrangthai() != TrangthaiDotdangky.GIAIDOAN1) {
            throw new NghiepvuException(
                "Đợt đăng ký chưa ở Giai đoạn 1");
        }

        LocalDateTime now = LocalDateTime.now();
        if (d.getBatdaugiangvien() == null
                || d.getKetthucgiangvien() == null
                || now.isBefore(d.getBatdaugiangvien())
                || !now.isBefore(d.getKetthucgiangvien())) {
            throw new NghiepvuException(
                "Ngoài thời gian giảng viên đăng ký đề tài");
        }
    }

    private List<Long> validGuides(DeTaiForm f) {
        if (f == null || f.getGvhd1() == null) {
            throw new NghiepvuException(
                "Đề tài cần ít nhất một GVHD");
        }

        List<Long> guides = new ArrayList<>();
        guides.add(f.getGvhd1());

        if (f.getGvhd2() != null) {
            if (f.getGvhd1().equals(f.getGvhd2())) {
                throw new NghiepvuException(
                    "Hai GVHD phải khác nhau");
            }
            guides.add(f.getGvhd2());
        }

        for (Long guideId : guides) {
            gv.findById(guideId).orElseThrow(
                () -> new NghiepvuException(
                    "GVHD không tồn tại: " + guideId));
        }

        return guides;
    }

    public Detai saveDetai(Long id, DeTaiForm f, String username) {
        if (f == null) {
            throw new NghiepvuException(
                "Dữ liệu đề tài không được để trống");
        }

        if (f.getDotdangkyid() == null
                || f.getTendetai() == null
                || f.getTendetai().trim().isEmpty()
                || f.getTendetai().trim().length() > 255) {
            throw new NghiepvuException(
                "Cần chọn đợt và nhập tên đề tài tối đa 255 ký tự");
        }

        activeProposal(lockDot(f.getDotdangkyid()));

        Detai e;
        if (id == null) {
            e = new Detai();
            e.setGiangviendexuatid(
                currentTeacher(username).getId());
        } else {
            e = detai(id);

            if (!canEditTopic(username, e)) {
                throw new AccessDeniedException(
                    "Không có quyền sửa đề tài này");
            }

            if (!Objects.equals(
                    e.getDotdangkyid(), f.getDotdangkyid())) {
                throw new NghiepvuException(
                    "Không được đổi đợt của đề tài đã tạo");
            }
        }

        if (f.getBomonid() == null
                || !bomons.existsById(f.getBomonid())) {
            throw new NghiepvuException(
                "Bộ môn không hợp lệ");
        }

        if (user(username).getVaitro() != Vaitro.TRUONGKHOA
                && !Objects.equals(
                    currentTeacher(username).getBomonid(),
                    f.getBomonid())) {
            throw new AccessDeniedException(
                "Không được đề xuất cho bộ môn khác");
        }

        List<Long> guides = validGuides(f);

        e.setDotdangkyid(f.getDotdangkyid());
        e.setBomonid(f.getBomonid());
        e.setTendetai(f.getTendetai().trim());
        e.setMota(f.getMota());
        e.setTrangthai(TrangthaiDetai.CHODUYET);

        e = detais.save(e);

        huongdans.deleteByDetaiid(e.getId());
        huongdans.flush();

        for (Long guideId : guides) {
            Giangvienhuongdan h = new Giangvienhuongdan();
            h.setDetaiid(e.getId());
            h.setGiangvienid(guideId);
            huongdans.save(h);
        }

        return e;
    }

    public void deleteDetai(Long id, String username) {
        Detai e = detai(id);

        if (!canEditTopic(username, e)) {
            throw new AccessDeniedException(
                "Không có quyền xóa đề tài này");
        }

        activeProposal(lockDot(e.getDotdangkyid()));

        huongdans.deleteByDetaiid(id);
        huongdans.flush();
        detais.delete(e);
        detais.flush();
    }

    // ===== DUYET, TU CHOI, CONG BO =====

    private void xetDuyetPermission(Detai e, String username) {
        khoa(username);
    }

    public Detai duyet(Long id, boolean accept, String username) {
        khoa(username);

        Detai e = detai(id);
        Dotdangky d = lockDot(e.getDotdangkyid());

        if (d.getTrangthai() != TrangthaiDotdangky.GIAIDOAN1) {
            throw new NghiepvuException(
                "Chỉ xét duyệt đề tài trong GĐ1");
        }

        if (e.getTrangthai() != TrangthaiDetai.CHODUYET) {
            throw new NghiepvuException(
                "Chỉ xét duyệt đề tài đang CHỜ DUYỆT");
        }

        e.setTrangthai(
            accept ? TrangthaiDetai.DADUYET
                   : TrangthaiDetai.TUCHOI);

        return detais.save(e);
    }

    public Detai congBo(Long id, String username) {
        khoa(username);

        Detai e = detai(id);
        Dotdangky d = lockDot(e.getDotdangkyid());

        if (d.getTrangthai() != TrangthaiDotdangky.GIAIDOAN1) {
            throw new NghiepvuException(
                "Chỉ công bố đề tài trong GĐ1");
        }

        if (e.getTrangthai() != TrangthaiDetai.DADUYET) {
            throw new NghiepvuException(
                "Chỉ công bố đề tài ĐÃ DUYỆT");
        }

        long count = huongdans.countByDetaiid(e.getId());
        if (count < 1 || count > 2) {
            throw new NghiepvuException(
                "Đề tài phải có từ 1 đến 2 GVHD");
        }

        e.setTrangthai(TrangthaiDetai.CONGBO);
        return detais.save(e);
    }
}
