-- REVIEW ONLY. Not run automatically.
-- Migration V004: Thêm ràng buộc UNIQUE và CHECK cho bảng Diem
-- Tác giả / Phụ trách: Hữu Lộc (phối hợp Thu Thảo)
-- Mục đích: Ngăn ngừa race condition tạo 2 bản ghi điểm khi gửi POST đồng thời (STT 10 - P1)

-- 1. Thêm ràng buộc UNIQUE (Dangkydetaiid, Giangvienid)
ALTER TABLE Diem 
    ADD CONSTRAINT Uk_diem_giangvien UNIQUE (Dangkydetaiid, Giangvienid);

-- 2. Thêm ràng buộc CHECK khoảng điểm [0, 10]
ALTER TABLE Diem 
    ADD CONSTRAINT Chk_diemso CHECK (Diemso >= 0 AND Diemso <= 10);
