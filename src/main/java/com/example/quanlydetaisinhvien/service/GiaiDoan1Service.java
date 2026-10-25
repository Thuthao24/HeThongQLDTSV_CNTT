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

/** Giai doan 1: de xuat -> xet duyet -> cong bo -> mo dang ky SV. */
@Service
@Transactional
public class GiaiDoan1Service {
    private final DotdangkyRepository dots;
    private final DetaiRepository detais;
    private final GiangvienRepository gv;
    private final GiangvienhuongdanRepository huongdans;
    private final BomonRepository bomons;
    private final TaikhoanRepository taikhoans;

    public GiaiDoan1Service(DotdangkyRepository dots, DetaiRepository detais, GiangvienRepository gv,
                            GiangvienhuongdanRepository huongdans, BomonRepository bomons, TaikhoanRepository taikhoans){
        this.dots=dots; this.detais=detais; this.gv=gv; this.huongdans=huongdans;
        this.bomons=bomons; this.taikhoans=taikhoans;
    }
    public Dotdangky dot(Long id){return dots.findById(id).orElseThrow(()->new NghiepvuException("Không tìm thấy đợt đăng ký"));}
    private Dotdangky lockDot(Long id){return dots.findByIdForUpdate(id).orElseThrow(()->new NghiepvuException("Không tìm thấy đợt đăng ký"));}
    public Detai detai(Long id){return detais.findById(id).orElseThrow(()->new NghiepvuException("Không tìm thấy đề tài"));}
    public List<Dotdangky> dsDot(){return dots.findAll();}
    public List<Dotdangky> dsDotGiaiDoan1(){return dots.findByTrangthaiIn(java.util.List.of(TrangthaiDotdangky.GIAIDOAN1));}
    public List<Detai> dsDetai(){return detais.findAll();}
    public List<Giangvienhuongdan> dsGvhd(Long id){return huongdans.findByDetaiid(id);}
    public List<Giangvien> dsGiangVien(){return gv.findAll();}

@Transactional(readOnly = true)
public List<Giangvien> dsGiangVienChoices() {
    return gv.findAll(org.springframework.data.domain.PageRequest.of(
            0, 200,
            org.springframework.data.domain.Sort.by("magiangvien")
    )).getContent();
}

@Transactional(readOnly = true)
public List<Dotdangky> dsDotChoices() {
    return dots.findAll(org.springframework.data.domain.PageRequest.of(
            0, 100,
            org.springframework.data.domain.Sort.by("id").descending()
    )).getContent();
}
    public List<Bomon> dsBoMon(){return bomons.findAll();}
    private Taikhoan user(String username){return taikhoans.findByTendangnhap(username)
            .orElseThrow(()->new AccessDeniedException("Không xác định được tài khoản"));}
    private void khoa(String username){if(user(username).getVaitro()!=Vaitro.TRUONGKHOA) throw new AccessDeniedException("Chỉ trưởng khoa được thực hiện");}
    private Giangvien currentTeacher(String username){
        Taikhoan a=user(username);
        if (a.getVaitro()!=Vaitro.GIANGVIEN && a.getVaitro()!=Vaitro.TRUONGBOMON && a.getVaitro()!=Vaitro.TRUONGKHOA)
            throw new AccessDeniedException("Chỉ giảng viên được đề xuất");
        return gv.findByTaikhoanid(a.getId()).orElseThrow(()->new AccessDeniedException("Tài khoản chưa liên kết giảng viên"));
    }
    public boolean canPropose(String username){
        if(username==null)return false;
        Taikhoan a=user(username);
        return (a.getVaitro()==Vaitro.GIANGVIEN || a.getVaitro()==Vaitro.TRUONGBOMON || a.getVaitro()==Vaitro.TRUONGKHOA)
                && gv.findByTaikhoanid(a.getId()).isPresent();
    }
    public boolean canEditTopic(String username, Detai e){
        Taikhoan a=user(username);
        if(e.getTrangthai()!=TrangthaiDetai.CHODUYET && e.getTrangthai()!=TrangthaiDetai.TUCHOI) return false;
        if(a.getVaitro()==Vaitro.TRUONGKHOA) return true;
        return gv.findByTaikhoanid(a.getId()).map(x->x.getId().equals(e.getGiangviendexuatid())).orElse(false);
    }
    /** Chỉ kiểm tra quyền đề tài với tối đa 2 truy vấn tài khoản/giảng viên cho cả trang. */
    @Transactional(readOnly=true)
    public Set<Long> editableTopicIds(String username, List<Detai> topics) {
        Taikhoan account = user(username);
        boolean isDean = account.getVaitro() == Vaitro.TRUONGKHOA;
        Long lecturerId = isDean ? null : gv.findByTaikhoanid(account.getId())
                .map(Giangvien::getId).orElse(null);
        Set<Long> allowed = new HashSet<>();
        for (Detai topic : topics) {
            if (topic.getTrangthai() != TrangthaiDetai.CHODUYET
                    && topic.getTrangthai() != TrangthaiDetai.TUCHOI) continue;
            if (isDean || (lecturerId != null && lecturerId.equals(topic.getGiangviendexuatid())))
                allowed.add(topic.getId());
        }
        return allowed;
    }

    
private void validTimes(DotForm f) {
    if (f == null || f.getTendot() == null || f.getTendot().isBlank()
            || f.getTendot().length() > 200 || f.getLoaidot() == null) {
        throw new NghiepvuException("Tên đợt hoặc loại đợt chưa hợp lệ");
    }

    LocalDateTime g1 = f.getBatdaugiangvien();
    LocalDateTime g2 = f.getKetthucgiangvien();
    LocalDateTime s1 = f.getBatdausinhvien();
    LocalDateTime s2 = f.getKetthucsinhvien();

    if (g1 == null || g2 == null || s1 == null || s2 == null) {
        throw new NghiepvuException(
            "Vui lòng nhập đầy đủ bốn mốc thời gian đăng ký của giảng viên và sinh viên");
    }

    if (!g1.isBefore(g2) || !s1.isBefore(s2) || s1.isBefore(g2)) {
        throw new NghiepvuException(
            "Thời gian phải theo thứ tự: GV bắt đầu < GV kết thúc <= SV bắt đầu < SV kết thúc");
    }

    Loaidot loai = f.getLoaidot();
    LocalDateTime hanPB = f.getHanchamphanbien();
    LocalDateTime ngayHD = f.getNgaybaocaohoidong();

    if ((loai == Loaidot.TLCN || loai == Loaidot.KLTN) && hanPB == null) {
        throw new NghiepvuException(
            "Tiểu luận chuyên ngành và khóa luận tốt nghiệp bắt buộc nhập hạn chấm phản biện");
    }

    if (loai == Loaidot.KLTN && ngayHD == null) {
        throw new NghiepvuException(
            "Khóa luận tốt nghiệp bắt buộc nhập ngày báo cáo hội đồng");
    }

    if ((loai == Loaidot.MONHOC || loai == Loaidot.NCKH) && hanPB != null) {
        throw new NghiepvuException(
            "Môn học và nghiên cứu khoa học không có hạn chấm phản biện");
    }

    if (loai != Loaidot.KLTN && ngayHD != null) {
        throw new NghiepvuException(
            "Chỉ khóa luận tốt nghiệp được nhập ngày báo cáo hội đồng");
    }

    if (hanPB != null && !hanPB.isAfter(s2)) {
        throw new NghiepvuException(
            "Hạn chấm phản biện phải sau thời gian kết thúc đăng ký sinh viên");
    }

    if (ngayHD != null && hanPB != null && ngayHD.isBefore(hanPB)) {
        throw new NghiepvuException(
            "Ngày báo cáo hội đồng không được trước hạn chấm phản biện");
    }
}
    public Dotdangky saveDot(Long id, DotForm f, String username){
        khoa(username); validTimes(f);
        Dotdangky d=id==null?new Dotdangky():dot(id);
        if(id!=null && d.getTrangthai()!=TrangthaiDotdangky.NHAP)
            throw new NghiepvuException("Chỉ sửa đợt đang ở trạng thái NHÁP");
        d.setTendot(f.getTendot().trim()); d.setLoaidot(f.getLoaidot());
        d.setBatdaugiangvien(f.getBatdaugiangvien()); d.setKetthucgiangvien(f.getKetthucgiangvien());
        d.setBatdausinhvien(f.getBatdausinhvien()); d.setKetthucsinhvien(f.getKetthucsinhvien());
        d.setHanchamphanbien(f.getHanchamphanbien()); d.setNgaybaocaohoidong(f.getNgaybaocaohoidong());
        if(id==null)d.setTrangthai(TrangthaiDotdangky.NHAP);
        return dots.save(d);
    }
    public void deleteDot(Long id,String username){
        khoa(username); Dotdangky d=dot(id);
        if(d.getTrangthai()!=TrangthaiDotdangky.NHAP||detais.existsByDotdangkyid(id))
            throw new NghiepvuException("Chỉ xóa đợt NHÁP chưa có đề tài");
        dots.delete(d);
    }
    public Dotdangky chuyenGiaiDoan(Long id, TrangthaiDotdangky next,String username){
        khoa(username); Dotdangky d=lockDot(id); TrangthaiDotdangky old=d.getTrangthai();
        if(old==TrangthaiDotdangky.NHAP && next==TrangthaiDotdangky.GIAIDOAN1){
            // Open the first phase; dates still gate teacher submissions.
        } else if(old==TrangthaiDotdangky.GIAIDOAN1 && next==TrangthaiDotdangky.CONGBO){
            if(!detais.existsByDotdangkyidAndTrangthai(id, TrangthaiDetai.CONGBO))
                throw new NghiepvuException("Cần công bố ít nhất một đề tài đã duyệt");
            if(detais.existsByDotdangkyidAndTrangthai(id, TrangthaiDetai.CHODUYET))
                throw new NghiepvuException("Còn đề tài đang chờ xét duyệt");
        } else if(old==TrangthaiDotdangky.CONGBO && next==TrangthaiDotdangky.GIAIDOAN2){
            LocalDateTime now=LocalDateTime.now();
            if(now.isBefore(d.getBatdausinhvien())||now.isAfter(d.getKetthucsinhvien()))
                throw new NghiepvuException("Chỉ mở GĐ2 trong thời gian sinh viên đăng ký");
        } else if(old==TrangthaiDotdangky.GIAIDOAN2 && next==TrangthaiDotdangky.DONG){
            // Close enrollment explicitly.
        } else throw new NghiepvuException("Chuyển trạng thái không hợp lệ: " + old + " → " + next);
        d.setTrangthai(next); return dots.save(d);
    }
    private void activeProposal(Dotdangky d){
        if(d.getTrangthai()!=TrangthaiDotdangky.GIAIDOAN1)
            throw new NghiepvuException("Đợt đăng ký chưa ở Giai đoạn 1");
        LocalDateTime now=LocalDateTime.now();
        if(now.isBefore(d.getBatdaugiangvien())||now.isAfter(d.getKetthucgiangvien()))
            throw new NghiepvuException("Ngoài thời gian GV đăng ký đề tài");
    }
    private List<Long> validGuides(DeTaiForm f){
        if(f.getGvhd1()==null)throw new NghiepvuException("Cần ít nhất một giảng viên hướng dẫn");
        List<Long> g=new ArrayList<>();g.add(f.getGvhd1());
        if(f.getGvhd2()!=null){if(f.getGvhd2().equals(f.getGvhd1()))throw new NghiepvuException("Hai GVHD phải khác nhau"); g.add(f.getGvhd2());}
        for(Long id:g)if(!gv.existsById(id))throw new NghiepvuException("GVHD không tồn tại: " + id);for (Long id : g) {
            Giangvien teacher = gv.findById(id)
                .orElseThrow(() -> new NghiepvuException(
                    "Giảng viên hướng dẫn không tồn tại"));
            if (!teacher.getBomonid().equals(f.getBomonid())) {
                    throw new NghiepvuException(
                         "GVHD phải thuộc đúng bộ môn của đề tài");
        }
    }
        return g;
    }
    public Detai saveDetai(Long id,DeTaiForm f,String username){
        if(f.getDotdangkyid()==null||f.getTendetai()==null||f.getTendetai().isBlank()||f.getTendetai().length()>255)
            throw new NghiepvuException("Cần chọn đợt và nhập tên đề tài (tối đa 255 ký tự)");
        activeProposal(lockDot(f.getDotdangkyid()));
        Detai e;
        if(id==null){ e=new Detai();e.setGiangviendexuatid(currentTeacher(username).getId()); }
        else {
            e=detai(id);
            if(!canEditTopic(username,e))throw new AccessDeniedException("Không có quyền sửa đề tài này");
            if(!Objects.equals(e.getDotdangkyid(),f.getDotdangkyid()))
                throw new NghiepvuException("Không đổi đợt đăng ký của đề tài đã tạo");
        }
        if(f.getBomonid()==null||!bomons.existsById(f.getBomonid()))throw new NghiepvuException("Bộ môn không hợp lệ");
        // Only the associated department may submit a topic; head-of-faculty can manage any.
        if(user(username).getVaitro()!=Vaitro.TRUONGKHOA && !currentTeacher(username).getBomonid().equals(f.getBomonid()))
            throw new AccessDeniedException("Không thể đề xuất cho bộ môn khác");
        List<Long> guides=validGuides(f);
        e.setDotdangkyid(f.getDotdangkyid());e.setBomonid(f.getBomonid());e.setTendetai(f.getTendetai().trim());
        e.setMota(f.getMota());e.setTrangthai(TrangthaiDetai.CHODUYET);
        e=detais.save(e);
        huongdans.deleteByDetaiid(e.getId());
        huongdans.flush();
        for(Long gid:guides){Giangvienhuongdan h=new Giangvienhuongdan(); h.setDetaiid(e.getId());h.setGiangvienid(gid);huongdans.save(h);}
        return e;
    }
    public void deleteDetai(Long id,String username){
        Detai e=detai(id);
        if(!canEditTopic(username,e))throw new AccessDeniedException("Không được xóa đề tài đã duyệt hoặc của giảng viên khác");
        activeProposal(dot(e.getDotdangkyid()));
        huongdans.deleteByDetaiid(id);huongdans.flush();detais.delete(e);
    }
    private void xetDuyetPermission(
        Detai e, String username) {
    khoa(username);
    }
    public Detai duyet(Long id,boolean accept,String username){
        Detai e=detai(id); xetDuyetPermission(e,username);
        if(lockDot(e.getDotdangkyid()).getTrangthai()!=TrangthaiDotdangky.GIAIDOAN1)
            throw new NghiepvuException("Chỉ xét duyệt trong GĐ1");
        if(e.getTrangthai()!=TrangthaiDetai.CHODUYET)throw new NghiepvuException("Chỉ xét duyệt đề tài CHỜ DUYỆT");
        e.setTrangthai(accept?TrangthaiDetai.DADUYET:TrangthaiDetai.TUCHOI);return detais.save(e);
    }
    public Detai congBo(Long id,String username){
        Detai e=detai(id);xetDuyetPermission(e,username);
        if(lockDot(e.getDotdangkyid()).getTrangthai()!=TrangthaiDotdangky.GIAIDOAN1)
            throw new NghiepvuException("Chỉ công bố đề tài trong GĐ1");
        if(e.getTrangthai()!=TrangthaiDetai.DADUYET)throw new NghiepvuException("Chỉ công bố đề tài ĐÃ DUYỆT");
        long count=huongdans.countByDetaiid(e.getId());
        if(count<1||count>2)throw new NghiepvuException("Đề tài phải có 1–2 GV hướng dẫn");
        e.setTrangthai(TrangthaiDetai.CONGBO);return detais.save(e);
    }
}
