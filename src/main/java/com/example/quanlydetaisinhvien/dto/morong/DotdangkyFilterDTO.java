package com.example.quanlydetaisinhvien.dto.morong;

import com.example.quanlydetaisinhvien.entity.TrangthaiDotdangky;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class DotdangkyFilterDTO extends SearchFilterDTO {
    public enum MocThoiGian {
        BATDAUGIANGVIEN, KETTHUCGIANGVIEN, BATDAUSINHVIEN, KETTHUCSINHVIEN,
        HANCHAMPHANBIEN, NGAYBAOCAOHOIDONG
    }
    private TrangthaiDotdangky trangthai;
    @NotNull private MocThoiGian mocThoiGian = MocThoiGian.BATDAUSINHVIEN;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate thoiGianTu;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate thoiGianDen;

    @AssertTrue(message = "Khoảng ngày không hợp lệ (năm 1900–9998, từ ngày không sau đến ngày).")
    public boolean isDateRangeValid() { return Dates.valid(thoiGianTu, thoiGianDen); }
    public TrangthaiDotdangky getTrangthai() { return trangthai; }
    public void setTrangthai(TrangthaiDotdangky value) { trangthai = value; }
    public MocThoiGian getMocThoiGian() { return mocThoiGian; }
    public void setMocThoiGian(MocThoiGian value) { mocThoiGian = value; }
    public LocalDate getThoiGianTu() { return thoiGianTu; }
    public void setThoiGianTu(LocalDate value) { thoiGianTu = value; }
    public LocalDate getThoiGianDen() { return thoiGianDen; }
    public void setThoiGianDen(LocalDate value) { thoiGianDen = value; }
}
