package com.example.quanlydetaisinhvien.dto.morong;

import jakarta.validation.constraints.*;
import com.example.quanlydetaisinhvien.entity.Loaidot;

public class SearchFilterDTO {
    @Size(max = 255, message = "Từ khóa tối đa 255 ký tự.")
    private String keyword = "";
    @Positive(message = "ID đợt phải dương.") private Long dotdangkyid;
    private Loaidot loaidot;
    @Min(0) private int page = 0;
    @Min(1) @Max(100) private int size = 20;
    @Size(max = 50) private String sort = "id,desc";

    @AssertTrue(message = "Trang hoặc kích thước trang vượt giới hạn truy vấn.")
    public boolean isOffsetValid() { return page >= 0 && size > 0 && (long) page * size <= Integer.MAX_VALUE; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword == null ? "" : keyword.strip(); }
    public Long getDotdangkyid() { return dotdangkyid; }
    public void setDotdangkyid(Long id) { this.dotdangkyid = id; }
    public Loaidot getLoaidot() { return loaidot; }
    public void setLoaidot(Loaidot value) { this.loaidot = value; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
