package com.example.quanlydetaisinhvien.dto.morong;

import com.example.quanlydetaisinhvien.entity.TrangthaiDangkydetai;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class KetquaFilterDTO extends SearchFilterDTO {
    private TrangthaiDangkydetai trangthai;
    private Boolean dacongbo;
    @DecimalMin("0.00") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2) private BigDecimal diemTu;
    @DecimalMin("0.00") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2) private BigDecimal diemDen;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate ngayCongboTu;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate ngayCongboDen;

    @AssertTrue(message = "Điểm từ không được lớn hơn điểm đến.")
    public boolean isScoreRangeValid() { return diemTu == null || diemDen == null || diemTu.compareTo(diemDen) <= 0; }
    @AssertTrue(message = "Khoảng ngày công bố không hợp lệ (năm 1900–9998).")
    public boolean isDateRangeValid() { return Dates.valid(ngayCongboTu, ngayCongboDen); }
    public TrangthaiDangkydetai getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiDangkydetai value) { trangthai = value; }
    public Boolean getDacongbo() { return dacongbo; }
    public void setDacongbo(Boolean value) { dacongbo = value; }
    public BigDecimal getDiemTu() { return diemTu; }
    public void setDiemTu(BigDecimal value) { diemTu = value; }
    public BigDecimal getDiemDen() { return diemDen; }
    public void setDiemDen(BigDecimal value) { diemDen = value; }
    public LocalDate getNgayCongboTu() { return ngayCongboTu; }
    public void setNgayCongboTu(LocalDate value) { ngayCongboTu = value; }
    public LocalDate getNgayCongboDen() { return ngayCongboDen; }
    public void setNgayCongboDen(LocalDate value) { ngayCongboDen = value; }
}
