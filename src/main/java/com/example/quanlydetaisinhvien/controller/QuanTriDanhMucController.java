package com.example.quanlydetaisinhvien.controller;

import com.example.quanlydetaisinhvien.dto.*;
import com.example.quanlydetaisinhvien.entity.Vaitro;
import com.example.quanlydetaisinhvien.exception.NghiepvuException;
import com.example.quanlydetaisinhvien.service.QuanTriDanhMucService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD cho danh muc chinh, chi TRUONGKHOA duoc goi theo SecurityConfig. */
@Controller
@RequestMapping("/quanly/danhmuc")
public class QuanTriDanhMucController {
    private final QuanTriDanhMucService svc;
    public QuanTriDanhMucController(QuanTriDanhMucService s){svc=s;}
    @GetMapping("/bomon/tao") public String bomonTao(Model m){bomonModel(m,new QuanTriBomonForm(),null);return "quanly/bomon-form";}
    @GetMapping("/bomon/{id}/sua") public String bomonSua(@PathVariable Long id,Model m){bomonModel(m,svc.formBomon(id),id);return "quanly/bomon-form";}
    private void bomonModel(Model m,QuanTriBomonForm f,Long id){m.addAttribute("form",f);m.addAttribute("id",id);m.addAttribute("title",id==null?"Thêm bộ môn":"Sửa bộ môn");}
    @PostMapping("/bomon/luu") public String bomonLuu(@RequestParam(required=false) Long id,@ModelAttribute QuanTriBomonForm f,RedirectAttributes r){
        try{svc.saveBoMon(id,f);r.addFlashAttribute("successMessage","Đã lưu bộ môn");}catch(NghiepvuException e){r.addFlashAttribute("errorMessage",e.getMessage());}
        return "redirect:/bomon";
    }
    @PostMapping("/bomon/{id}/xoa") public String bomonXoa(@PathVariable Long id,RedirectAttributes r){
        try{svc.deleteBoMon(id);r.addFlashAttribute("successMessage","Đã xóa bộ môn");}
        catch(NghiepvuException|DataIntegrityViolationException e){r.addFlashAttribute("errorMessage","Không thể xóa: bộ môn có dữ liệu phụ thuộc");}
        return "redirect:/bomon";
    }
    private void person(Model m,String type,QuanTriDanhMucForm f,Long id){
        m.addAttribute("title",id==null?"Tạo "+type:"Sửa "+type);m.addAttribute("id",id);
        m.addAttribute("form",f);m.addAttribute("type",type);m.addAttribute("bomons",svc.bomonList());
        m.addAttribute("roles",new Vaitro[]{Vaitro.GIANGVIEN,Vaitro.TRUONGBOMON});
    }
    @GetMapping("/giangvien/tao") public String gvTao(Model m){QuanTriDanhMucForm f=new QuanTriDanhMucForm();f.setVaitro(Vaitro.GIANGVIEN);person(m,"giangvien",f,null);return "quanly/person-form";}
    @GetMapping("/giangvien/{id}/sua") public String gvSua(@PathVariable Long id,Model m){person(m,"giangvien",svc.formGV(id),id);return "quanly/person-form";}
    @PostMapping("/giangvien/luu") public String gvLuu(@RequestParam(required=false) Long id,@ModelAttribute QuanTriDanhMucForm f,RedirectAttributes r){
        try{svc.saveGV(id,f);r.addFlashAttribute("successMessage","Đã lưu giảng viên");}
        catch(NghiepvuException e){r.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/giangvien";
    }
    @PostMapping("/giangvien/{id}/xoa") public String gvXoa(@PathVariable Long id,RedirectAttributes r){
        try{svc.deleteGV(id);r.addFlashAttribute("successMessage","Đã xóa giảng viên");}
        catch(NghiepvuException|DataIntegrityViolationException e){r.addFlashAttribute("errorMessage","Không thể xóa giảng viên đang có dữ liệu liên quan");}return "redirect:/giangvien";
    }
    @GetMapping("/sinhvien/tao") public String svTao(Model m){person(m,"sinhvien",new QuanTriDanhMucForm(),null);return "quanly/person-form";}
    @GetMapping("/sinhvien/{id}/sua") public String svSua(@PathVariable Long id,Model m){person(m,"sinhvien",svc.formSV(id),id);return "quanly/person-form";}
    @PostMapping("/sinhvien/luu") public String svLuu(@RequestParam(required=false) Long id,@ModelAttribute QuanTriDanhMucForm f,RedirectAttributes r){
        try{svc.saveSV(id,f);r.addFlashAttribute("successMessage","Đã lưu sinh viên");}
        catch(NghiepvuException e){r.addFlashAttribute("errorMessage",e.getMessage());}return "redirect:/sinhvien";
    }
    @PostMapping("/sinhvien/{id}/xoa") public String svXoa(@PathVariable Long id,RedirectAttributes r){
        try{svc.deleteSV(id);r.addFlashAttribute("successMessage","Đã xóa sinh viên");}
        catch(NghiepvuException|DataIntegrityViolationException e){r.addFlashAttribute("errorMessage","Không thể xóa sinh viên đã tham gia nghiệp vụ");}return "redirect:/sinhvien";
    }
}
