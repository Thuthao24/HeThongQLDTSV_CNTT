-- =========================================================
-- CAP NHAT DANG NHAP SINH VIEN BANG MA SO SINH VIEN
-- KHONG XOA DATABASE / KHONG XOA DU LIEU
-- Mat khau demo: Abc@12345
-- =========================================================
USE Quanlydetaisinhvien;

-- Dong bo Tendangnhap cua tai khoan sinh vien = Masinhvien
UPDATE Taikhoan tk
JOIN Sinhvien sv ON sv.Taikhoanid = tk.Id
SET tk.Tendangnhap = sv.Masinhvien
WHERE tk.Vaitro = 'SINHVIEN';

-- Cap nhat BCrypt hop le cho tai khoan demo trong database hien tai
UPDATE Taikhoan
SET Matkhau = '$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.'
WHERE Vaitro IN ('SINHVIEN','GIANGVIEN','TRUONGBOMON','TRUONGKHOA');

-- Kiem tra ket qua
SELECT sv.Masinhvien, tk.Tendangnhap, tk.Hoten, tk.Vaitro, tk.Kichhoat
FROM Sinhvien sv
JOIN Taikhoan tk ON tk.Id = sv.Taikhoanid
ORDER BY sv.Masinhvien
LIMIT 20;
