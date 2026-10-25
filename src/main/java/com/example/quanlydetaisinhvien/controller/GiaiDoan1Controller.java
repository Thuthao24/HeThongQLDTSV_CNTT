package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.GiaiDoan1.*;
import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.GiaiDoan1Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/giai-doan-1")
public class GiaiDoan1Controller {
    private final GiaiDoan1Service svc;
    private final com.example.quanlydetaisinhvien.service.DisplayLookupService lookup;

public GiaiDoan1Controller(
        GiaiDoan1Service svc,
        com.example.quanlydetaisinhvien.service.DisplayLookupService lookup) {
    this.svc = svc;
    this.lookup = lookup;
}
    private String username(Authentication a){return a.getName();}
    private boolean khoa(Authentication a){return a.getAuthorities().stream().anyMatch(x->"ROLE_TRUONGKHOA".equals(x.getAuthority()));}
    private void requireKhoa(Authentication a){if(!khoa(a)) throw new AccessDeniedException("Chỉ trưởng khoa được phép");}
    private void modelForDot(Model m,DotForm f,Long id){
        m.addAttribute("title",id==null?"Tạo đợt đăng ký":"Sửa đợt đăng ký");
        m.addAttribute("form",f);m.addAttribute("id",id);m.addAttribute("loaidots",Loaidot.values());
    }
    @GetMapping("/dot/tao")
    public String taoDot(Model m,Authentication a){requireKhoa(a);modelForDot(m,new DotForm(),null);return "giai-doan-1/dot-form";}
    @GetMapping("/dot/{id}/sua")
    public String suaDot(@PathVariable Long id,Model m,Authentication a){
        requireKhoa(a);Dotdangky d=svc.dot(id);
        if(d.getTrangthai()!=TrangthaiDotdangky.NHAP)throw new NghiepvuException("Chỉ sửa đợt NHÁP");
        modelForDot(m,DotForm.from(d),id);return "giai-doan-1/dot-form";
    }
    @PostMapping("/dot/luu")
    public String luuDot(@ModelAttribute DotForm form,@RequestParam(required=false) Long id,
                         Authentication a,RedirectAttributes r){
        try {requireKhoa(a);Dotdangky d=svc.saveDot(id,form,username(a));success(r,"Đã lưu đợt đăng ký #"+d.getId());return "redirect:/dotdangky";}
        catch(NghiepvuException ex){error(r,ex.getMessage());return "redirect:"+(id==null?"/giai-doan-1/dot/tao":"/giai-doan-1/dot/"+id+"/sua");}
    }
    @PostMapping("/dot/{id}/xoa")
    public String xoaDot(@PathVariable Long id,Authentication a,RedirectAttributes r){
        try {requireKhoa(a);svc.deleteDot(id,username(a));success(r,"Đã xóa đợt đăng ký");}
        catch(NghiepvuException|DataIntegrityViolationException ex){error(r,"Không thể xóa đợt. Kiểm tra đề tài, nhóm và dữ liệu liên quan.");}
        return "redirect:/dotdangky";
    }
    @PostMapping("/dot/{id}/chuyen")
    public String chuyen(@PathVariable Long id,@RequestParam TrangthaiDotdangky trangthai,
                        Authentication a,RedirectAttributes r){
        try {requireKhoa(a);svc.chuyenGiaiDoan(id,trangthai,username(a));success(r,"Đã chuyển đợt sang "+trangthai);}
        catch(NghiepvuException ex){error(r,ex.getMessage());}
        return "redirect:/dotdangky";
    }
    private void modelForTopic(Model m,DeTaiForm f,Long id,Authentication a){
        m.addAttribute("title",id==null?"Đề xuất đề tài":"Chỉnh sửa đề tài");m.addAttribute("form",f);
        m.addAttribute("id",id);m.addAttribute("dots",svc.dsDotGiaiDoan1());
        m.addAttribute("bomons", svc.dsBoMon());
var teachers = svc.dsGiangVienChoices();
m.addAttribute("giangviens", teachers);
m.addAttribute("teacherLabels", lookup.accountLabels(
        teachers.stream().map(Giangvien::getTaikhoanid).toList()));
    }
    @GetMapping("/detai/tao")
    public String taoDeTai(Model m,Authentication a){
        if(!svc.canPropose(username(a)))throw new AccessDeniedException("Cần tài khoản giảng viên hợp lệ");
        modelForTopic(m,new DeTaiForm(),null,a);return "giai-doan-1/detai-form";
    }
    @GetMapping("/detai/{id}/sua")
    public String suaDeTai(@PathVariable Long id,Model m,Authentication a){
        Detai e=svc.detai(id);
        if(!svc.canEditTopic(username(a),e))throw new AccessDeniedException("Không có quyền sửa đề tài");
        List<Giangvienhuongdan> list=svc.dsGvhd(id);
        DeTaiForm f=DeTaiForm.from(e,list.isEmpty()?null:list.get(0).getGiangvienid(),
                          list.size()<2?null:list.get(1).getGiangvienid());
        modelForTopic(m,f,id,a);return "giai-doan-1/detai-form";
    }
    @PostMapping("/detai/luu")
    public String luuDetai(@ModelAttribute DeTaiForm form,@RequestParam(required=false) Long id,
                            Authentication a,RedirectAttributes r){
        try {Detai e=svc.saveDetai(id,form,username(a));success(r,"Đã lưu đề tài #"+e.getId()+" và GVHD");return "redirect:/detai";}
        catch(NghiepvuException ex){error(r,ex.getMessage());return "redirect:"+(id==null?"/giai-doan-1/detai/tao":"/giai-doan-1/detai/"+id+"/sua");}
    }
    @PostMapping("/detai/{id}/xoa")
    public String xoaDeTai(@PathVariable Long id,Authentication a,RedirectAttributes r){
        try {svc.deleteDetai(id,username(a));success(r,"Đã xóa đề tài");}
        catch(NghiepvuException|DataIntegrityViolationException ex){error(r,"Không thể xóa đề tài đang được sử dụng hoặc đã xét duyệt");}
        return "redirect:/detai";
    }
    @PostMapping("/detai/{id}/duyet")
    public String duyet(@PathVariable Long id,@RequestParam boolean chapnhan,Authentication a,RedirectAttributes r){
        try {svc.duyet(id,chapnhan,username(a));success(r,chapnhan?"Đã duyệt đề tài":"Đã từ chối đề tài");}
        catch(NghiepvuException ex){error(r,ex.getMessage());}
        return "redirect:/detai";
    }
    @PostMapping("/detai/{id}/congbo")
    public String congbo(@PathVariable Long id,Authentication a,RedirectAttributes r){
        try {svc.congBo(id,username(a));success(r,"Đã công bố đề tài cho đợt đăng ký");}
        catch(NghiepvuException ex){error(r,ex.getMessage());}
        return "redirect:/detai";
    }
    private void error(RedirectAttributes r,String s){r.addFlashAttribute("errorMessage",s);}
    private void success(RedirectAttributes r,String s){r.addFlashAttribute("successMessage",s);}
}
