USE Quanlydetaisinhvien;

-- 1. Them tai khoan & sinh vien moi (Mat khau: Abc@12345)
INSERT INTO taikhoan (Id, Tendangnhap, Matkhau, Hoten, Email, Vaitro, Kichhoat) VALUES
(1020, '23110020', '$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.', 'Hoàng Đức Anh', '23110020@student.edu.vn', 'SINHVIEN', 1),
(1021, '23110021', '$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.', 'Lê Thùy Chi', '23110021@student.edu.vn', 'SINHVIEN', 1),
(1022, '23110022', '$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.', 'Ngô Bá Đạt', '23110022@student.edu.vn', 'SINHVIEN', 1),
(1023, '23110023', '$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.', 'Trần Thảo Ly', '23110023@student.edu.vn', 'SINHVIEN', 1),
(1024, '23110024', '$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.', 'Vũ Minh Quân', '23110024@student.edu.vn', 'SINHVIEN', 1)
ON DUPLICATE KEY UPDATE Hoten = VALUES(Hoten), Matkhau = VALUES(Matkhau);

INSERT INTO sinhvien (Id, Masinhvien, Taikhoanid, Bomonid) VALUES
(20, '23110020', 1020, 1),
(21, '23110021', 1021, 1),
(22, '23110022', 1022, 2),
(23, '23110023', 1023, 2),
(24, '23110024', 1024, 3)
ON DUPLICATE KEY UPDATE Masinhvien = VALUES(Masinhvien);

-- 2. Tao Nhom 10 (Dot 2) - Test chuc nang Loi moi thanh vien (Nhom truong: 23110020)
INSERT INTO nhomsinhvien (Id, Tennhom, Dotdangkyid, Nhomtruongid) VALUES
(10, 'Nhóm Khởi Nghiệp AI', 2, 20)
ON DUPLICATE KEY UPDATE Tennhom = VALUES(Tennhom);

INSERT INTO thanhviennhom (Nhomid, Sinhvienid) VALUES
(10, 20)
ON DUPLICATE KEY UPDATE Sinhvienid = VALUES(Sinhvienid);

-- Gui loi moi den 23110021 (Le Thuy Chi)
INSERT INTO loimoithanhvien (Id, Nhomid, Sinhvienid, Nguoimoiid, Trangthai, Ngaytao) VALUES
(100, 10, 21, 20, 'CHO_XAC_NHAN', NOW())
ON DUPLICATE KEY UPDATE Trangthai = 'CHO_XAC_NHAN';

-- 3. Tao Nhom 11 (Dot 2) - Test de tai CHODUYET & si so 3 SV (Nhom truong: 23110009)
INSERT INTO nhomsinhvien (Id, Tennhom, Dotdangkyid, Nhomtruongid) VALUES
(11, 'Nhóm BigData & IoT', 2, 9)
ON DUPLICATE KEY UPDATE Tennhom = VALUES(Tennhom);

INSERT INTO thanhviennhom (Nhomid, Sinhvienid) VALUES
(11, 9),
(11, 10),
(11, 11)
ON DUPLICATE KEY UPDATE Sinhvienid = VALUES(Sinhvienid);

-- Dang ky de tai 6 (AI diem danh bang khuon mat - GVHD: gv001) o trang thai CHODUYET
INSERT INTO dangkydetai (Id, Nhomid, Detaiid, Trangthai, Ngaydangky) VALUES
(11, 11, 6, 'CHODUYET', NOW())
ON DUPLICATE KEY UPDATE Trangthai = VALUES(Trangthai);

-- 4. Tao Nhom 12 (Dot 2) - Test nop bao cao lan dau (DADUYET, chua nop bao cao)
INSERT INTO nhomsinhvien (Id, Tennhom, Dotdangkyid, Nhomtruongid) VALUES
(12, 'Nhóm Phát triển Game Engine', 2, 2)
ON DUPLICATE KEY UPDATE Tennhom = VALUES(Tennhom);

INSERT INTO thanhviennhom (Nhomid, Sinhvienid) VALUES
(12, 2),
(12, 3),
(12, 4)
ON DUPLICATE KEY UPDATE Sinhvienid = VALUES(Sinhvienid);

-- Dang ky de tai 5 (Tim kiem tai lieu hoc tap - GVHD: gv002) o trang thai DADUYET
INSERT INTO dangkydetai (Id, Nhomid, Detaiid, Trangthai, Ngaydangky) VALUES
(12, 12, 5, 'DADUYET', NOW())
ON DUPLICATE KEY UPDATE Trangthai = VALUES(Trangthai);

-- 5. Them cac thong bao da dang voi phan quyen doituong
INSERT INTO thongbao (Id, Tieude, Noidung, Nguoidangid, Ngaydang) VALUES
(3, 'Kế hoạch báo cáo nghiệm thu khóa luận và đồ án đợt 2 năm học 2026-2027', 'Phòng Đào tạo và Khoa CNTT thông báo kế hoạch tổ chức báo cáo nghiệm thu khóa luận đợt 2. Các nhóm sinh viên hoàn thành nộp tài liệu báo cáo đầy đủ trước ngày đánh giá. Giảng viên hướng dẫn và các hội đồng theo dõi danh sách để sắp xếp lịch đánh giá chính xác.', 1, NOW() - INTERVAL 2 HOUR),
(4, 'Hướng dẫn quy chuẩn nộp báo cáo đề tài trực tuyến dành cho sinh viên', 'Sinh viên lưu ý tải tệp báo cáo định dạng chuẩn (.pdf, .docx, .zip, .rar), dung lượng không vượt quá 20MB. Nhóm trưởng là người đại diện nộp và cập nhật tài liệu của nhóm. Khi đề tài đã có kết quả điểm đánh giá, hệ thống sẽ tự động khóa phần nộp để bảo toàn dữ liệu.', 1, NOW() - INTERVAL 1 HOUR),
(5, 'Thông báo phân công cán bộ phản biện và lịch họp hội đồng đánh giá', 'Kính gửi các Thầy/Cô thuộc các bộ môn Công nghệ phần mềm, Hệ thống thông tin, Khoa học máy tính và Mạng máy tính. Đề nghị các Thầy/Cô kiểm tra danh sách phân công phản biện và lịch làm việc hội đồng trên cổng thông tin.', 1, NOW() - INTERVAL 30 MINUTE)
ON DUPLICATE KEY UPDATE Tieude = VALUES(Tieude), Noidung = VALUES(Noidung);

-- Thiet lap doi tuong nhan thong bao
DELETE FROM thongbaodoituong WHERE Thongbaoid IN (3, 4, 5);

-- Thong bao 3: Ca 3 doi tuong deu thay
INSERT INTO thongbaodoituong (Thongbaoid, Vaitro) VALUES
(3, 'SINHVIEN'),
(3, 'GIANGVIEN'),
(3, 'TRUONGBOMON'),
-- Thong bao 4: Danh rieng cho Sinh vien
(4, 'SINHVIEN'),
-- Thong bao 5: Danh cho Giang vien & Truong bo mon
(5, 'GIANGVIEN'),
(5, 'TRUONGBOMON');
