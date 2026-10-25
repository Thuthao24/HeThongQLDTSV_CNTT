package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** CRUD danh muc, khong su dung dependency moi. Controller gioi han truong khoa. */
@Service
@Transactional
@org.springframework.security.access.prepost.PreAuthorize("hasRole('TRUONGKHOA')")
public class QuanTriDanhMucService {
    private final BomonRepository bomons;private final GiangvienRepository gvs;
    private final SinhvienRepository svs;private final TaikhoanRepository accounts;private final PasswordEncoder encoder;
    public QuanTriDanhMucService(BomonRepository b,GiangvienRepository g,SinhvienRepository s,
                                 TaikhoanRepository t,PasswordEncoder encoder){
        this.bomons=b;this.gvs=g;this.svs=s;this.accounts=t;this.encoder=encoder;
    }
    public List<Bomon> bomonList(){return bomons.findAll();}
    public QuanTriBomonForm formBomon(Long id){Bomon b=boMon(id);QuanTriBomonForm f=new QuanTriBomonForm();f.setMabomon(b.getMabomon());f.setTenbomon(b.getTenbomon());return f;}
    private Bomon boMon(Long id){return bomons.findById(id).orElseThrow(()->new NghiepvuException("Không tìm thấy bộ môn"));}
    private void validText(String s,String field,int max){if(s==null||s.isBlank()||s.trim().length()>max)throw new NghiepvuException(field+" không được trống hoặc vượt "+max+" ký tự");}
    private void checkBoMon(Long id){if(id==null||!bomons.existsById(id))throw new NghiepvuException("Bộ môn không tồn tại");}
    public void saveBoMon(Long id,QuanTriBomonForm form){
        validText(form.getMabomon(),"Mã bộ môn",20);validText(form.getTenbomon(),"Tên bộ môn",150);
        String code=form.getMabomon().trim().toUpperCase();
        if((id==null ? bomons.existsByMabomonIgnoreCase(code) : bomons.existsByMabomonIgnoreCaseAndIdNot(code,id)))
            throw new NghiepvuException("Trùng mã bộ môn");
        Bomon b=id==null?new Bomon():boMon(id);b.setMabomon(code);b.setTenbomon(form.getTenbomon().trim());bomons.save(b);
    }
    public void deleteBoMon(Long id){
        Bomon b=boMon(id);
        if(gvs.existsByBomonid(id)||svs.existsByBomonid(id))
            throw new NghiepvuException("Bộ môn đang có giảng viên hoặc sinh viên");
        bomons.delete(b);bomons.flush();
    }
    public QuanTriDanhMucForm formGV(Long id){
        Giangvien gv=gvs.findById(id).orElseThrow(()->new NghiepvuException("Không tìm thấy giảng viên"));
        Taikhoan a=account(gv.getTaikhoanid());QuanTriDanhMucForm f=fromAccount(a);
        f.setMa(gv.getMagiangvien());f.setBomonid(gv.getBomonid());return f;
    }
    public QuanTriDanhMucForm formSV(Long id){
        Sinhvien sv=svs.findById(id).orElseThrow(()->new NghiepvuException("Không tìm thấy sinh viên"));
        Taikhoan a=account(sv.getTaikhoanid());QuanTriDanhMucForm f=fromAccount(a);
        f.setMa(sv.getMasinhvien());f.setBomonid(sv.getBomonid());f.setLop(sv.getLop());return f;
    }
    private QuanTriDanhMucForm fromAccount(Taikhoan a){
        QuanTriDanhMucForm f=new QuanTriDanhMucForm();f.setHoten(a.getHoten());f.setEmail(a.getEmail());
        f.setVaitro(a.getVaitro());f.setKichhoat(Boolean.TRUE.equals(a.getKichhoat()));return f;
    }
    private Taikhoan account(Long id){return accounts.findById(id).orElseThrow(()->new NghiepvuException("Tài khoản không tồn tại"));}
    private void validPerson(QuanTriDanhMucForm f,boolean create){
        validText(f.getHoten(),"Họ tên",150);checkBoMon(f.getBomonid());
        if(f.getEmail()!=null&&!f.getEmail().isBlank()&&(!f.getEmail().contains("@")||f.getEmail().length()>150))
            throw new NghiepvuException("Email chưa hợp lệ");
        if(create){
            validText(f.getMa(),"Mã giảng viên/sinh viên",20);
            if(accounts.findByTendangnhap(f.getMa().trim()).isPresent())throw new NghiepvuException("Mã đăng nhập đã tồn tại");
            if(f.getMatkhau()==null||f.getMatkhau().length()<8)throw new NghiepvuException("Mật khẩu khi tạo phải có ít nhất 8 ký tự");
        }
    }
    private void checkEmail(String email,Long accountId){
        if(email==null||email.isBlank())return;
        if((accountId==null ? accounts.existsByEmailIgnoreCase(email.trim()) : accounts.existsByEmailIgnoreCaseAndIdNot(email.trim(),accountId)))
            throw new NghiepvuException("Email đã tồn tại");
    }
    private Taikhoan buildAccount(QuanTriDanhMucForm f,Vaitro role){
        Taikhoan a=new Taikhoan();a.setTendangnhap(f.getMa().trim());a.setMatkhau(encoder.encode(f.getMatkhau()));
        a.setVaitro(role);a.setHoten(f.getHoten().trim());a.setEmail(normalEmail(f.getEmail()));a.setKichhoat(f.isKichhoat());
        return accounts.save(a);
    }
    private String normalEmail(String email){return email==null||email.isBlank()?null:email.trim();}
    public void saveGV(Long id,QuanTriDanhMucForm f){
        validPerson(f,id==null);
        Vaitro role=f.getVaitro();if(role!=Vaitro.GIANGVIEN&&role!=Vaitro.TRUONGBOMON)
            throw new NghiepvuException("Vai trò phải là GIANGVIEN hoặc TRUONGBOMON");
        if(id==null){
            if(gvs.existsByMagiangvienIgnoreCase(f.getMa().trim()))throw new NghiepvuException("Mã GV trùng");
            checkEmail(f.getEmail(),null);
            Taikhoan a=buildAccount(f,role);Giangvien g=new Giangvien();g.setTaikhoanid(a.getId());
            g.setMagiangvien(f.getMa().trim());g.setBomonid(f.getBomonid());gvs.save(g);
        } else {
            Giangvien g=gvs.findById(id).orElseThrow(()->new NghiepvuException("Không tìm thấy giảng viên"));
            Taikhoan a=account(g.getTaikhoanid());checkEmail(f.getEmail(),a.getId());
            a.setHoten(f.getHoten().trim());a.setEmail(normalEmail(f.getEmail()));a.setVaitro(role);a.setKichhoat(f.isKichhoat());
            accounts.save(a);g.setBomonid(f.getBomonid());gvs.save(g);
        }
    }
    public void saveSV(Long id,QuanTriDanhMucForm f){
        validPerson(f,id==null);
        if(f.getLop()!=null&&f.getLop().length()>50)throw new NghiepvuException("Lớp tối đa 50 ký tự");
        if(id==null){
            if(svs.existsByMasinhvienIgnoreCase(f.getMa().trim()))throw new NghiepvuException("Mã SV trùng");
            checkEmail(f.getEmail(),null);
            Taikhoan a=buildAccount(f,Vaitro.SINHVIEN);Sinhvien sv=new Sinhvien();sv.setTaikhoanid(a.getId());
            sv.setMasinhvien(f.getMa().trim());sv.setLop(f.getLop());sv.setBomonid(f.getBomonid());svs.save(sv);
        } else {
            Sinhvien sv=svs.findById(id).orElseThrow(()->new NghiepvuException("Không tìm thấy sinh viên"));
            Taikhoan a=account(sv.getTaikhoanid());checkEmail(f.getEmail(),a.getId());
            a.setHoten(f.getHoten().trim());a.setEmail(normalEmail(f.getEmail()));a.setKichhoat(f.isKichhoat());accounts.save(a);
            sv.setLop(f.getLop());sv.setBomonid(f.getBomonid());svs.save(sv);
        }
    }
    public void deleteGV(Long id){
        Giangvien g=gvs.findById(id).orElseThrow(()->new NghiepvuException("Giảng viên không tồn tại"));
        Taikhoan a=account(g.getTaikhoanid());gvs.delete(g);gvs.flush();accounts.delete(a);accounts.flush();
    }
    public void deleteSV(Long id){
        Sinhvien s=svs.findById(id).orElseThrow(()->new NghiepvuException("Sinh viên không tồn tại"));
        Taikhoan a=account(s.getTaikhoanid());svs.delete(s);svs.flush();accounts.delete(a);accounts.flush();
    }
}
