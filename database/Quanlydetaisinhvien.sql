
DROP DATABASE IF EXISTS Quanlydetaisinhvien;
CREATE DATABASE Quanlydetaisinhvien
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Quanlydetaisinhvien;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE Bomon (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Mabomon VARCHAR(20) NOT NULL UNIQUE,
    Tenbomon VARCHAR(150) NOT NULL
);

CREATE TABLE Taikhoan (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tendangnhap VARCHAR(50) NOT NULL UNIQUE,
    Matkhau VARCHAR(100) NOT NULL,
    Hoten VARCHAR(150) NOT NULL,
    Email VARCHAR(150) UNIQUE,
    Vaitro ENUM('TRUONGKHOA','TRUONGBOMON','GIANGVIEN','SINHVIEN') NOT NULL,
    Kichhoat BOOLEAN DEFAULT TRUE
);

CREATE TABLE Giangvien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Taikhoanid BIGINT NOT NULL UNIQUE,
    Magiangvien VARCHAR(20) NOT NULL UNIQUE,
    Bomonid BIGINT NOT NULL,
    FOREIGN KEY (Taikhoanid) REFERENCES Taikhoan(Id),
    FOREIGN KEY (Bomonid) REFERENCES Bomon(Id)
);

CREATE TABLE Sinhvien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Taikhoanid BIGINT NOT NULL UNIQUE,
    Masinhvien VARCHAR(20) NOT NULL UNIQUE,
    Bomonid BIGINT NOT NULL,
    Lop VARCHAR(50),
    FOREIGN KEY (Taikhoanid) REFERENCES Taikhoan(Id),
    FOREIGN KEY (Bomonid) REFERENCES Bomon(Id)
);

CREATE TABLE Dotdangky (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tendot VARCHAR(200) NOT NULL,
    Loaidot ENUM('MONHOC','NCKH','TLCN','KLTN') NOT NULL,
    Batdaugiangvien DATETIME NOT NULL,
    Ketthucgiangvien DATETIME NOT NULL,
    Batdausinhvien DATETIME NOT NULL,
    Ketthucsinhvien DATETIME NOT NULL,
    Hanchamphanbien DATETIME NULL,
    Ngaybaocaohoidong DATETIME NULL,
    Trangthai ENUM('NHAP','GIAIDOAN1','CONGBO','GIAIDOAN2','DONG') DEFAULT 'NHAP'
);

CREATE TABLE Detai (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dotdangkyid BIGINT NOT NULL,
    Bomonid BIGINT NOT NULL,
    Tendetai VARCHAR(255) NOT NULL,
    Mota TEXT,
    Giangviendexuatid BIGINT NOT NULL,
    Trangthai ENUM('CHODUYET','DADUYET','CONGBO','TUCHOI') DEFAULT 'CHODUYET',
    FOREIGN KEY (Dotdangkyid) REFERENCES Dotdangky(Id),
    FOREIGN KEY (Bomonid) REFERENCES Bomon(Id),
    FOREIGN KEY (Giangviendexuatid) REFERENCES Giangvien(Id)
);

CREATE TABLE Giangvienhuongdan (
    Detaiid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    PRIMARY KEY (Detaiid, Giangvienid),
    FOREIGN KEY (Detaiid) REFERENCES Detai(Id),
    FOREIGN KEY (Giangvienid) REFERENCES Giangvien(Id)
);

CREATE TABLE Nhomsinhvien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dotdangkyid BIGINT NOT NULL,
    Tennhom VARCHAR(100) NOT NULL,
    Nhomtruongid BIGINT NOT NULL,
    FOREIGN KEY (Dotdangkyid) REFERENCES Dotdangky(Id),
    FOREIGN KEY (Nhomtruongid) REFERENCES Sinhvien(Id)
);

CREATE TABLE Thanhviennhom (
    Nhomid BIGINT NOT NULL,
    Sinhvienid BIGINT NOT NULL,
    PRIMARY KEY (Nhomid, Sinhvienid),
    FOREIGN KEY (Nhomid) REFERENCES Nhomsinhvien(Id),
    FOREIGN KEY (Sinhvienid) REFERENCES Sinhvien(Id)
);

CREATE TABLE Dangkydetai (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Nhomid BIGINT NOT NULL UNIQUE,
    Detaiid BIGINT NOT NULL,
    Trangthai ENUM('CHODUYET','DADUYET','TUCHOI') DEFAULT 'CHODUYET',
    Ngaydangky DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Nhomid) REFERENCES Nhomsinhvien(Id),
    FOREIGN KEY (Detaiid) REFERENCES Detai(Id)
);

CREATE TABLE Nopbaocao (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL,
    Sinhvienid BIGINT NOT NULL,
    Duongdanfile VARCHAR(500) NOT NULL,
    Ngaynop DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Dangkydetaiid) REFERENCES Dangkydetai(Id),
    FOREIGN KEY (Sinhvienid) REFERENCES Sinhvien(Id)
);

CREATE TABLE Hoidong (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dotdangkyid BIGINT NOT NULL,
    Tenhoidong VARCHAR(150) NOT NULL,
    Ngaybaocao DATETIME,
    Phong VARCHAR(50),
    FOREIGN KEY (Dotdangkyid) REFERENCES Dotdangky(Id)
);

CREATE TABLE Thanhvienhoidong (
    Hoidongid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    Vaitro ENUM('CHUTICH','THUKY','THANHVIEN') NOT NULL,
    PRIMARY KEY (Hoidongid, Giangvienid),
    FOREIGN KEY (Hoidongid) REFERENCES Hoidong(Id),
    FOREIGN KEY (Giangvienid) REFERENCES Giangvien(Id)
);

CREATE TABLE Giangvienphanbien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    FOREIGN KEY (Dangkydetaiid) REFERENCES Dangkydetai(Id),
    FOREIGN KEY (Giangvienid) REFERENCES Giangvien(Id)
);

CREATE TABLE Diem (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    Diemso DECIMAL(4,2) NOT NULL,
    Nhanxet VARCHAR(500),
    FOREIGN KEY (Dangkydetaiid) REFERENCES Dangkydetai(Id),
    FOREIGN KEY (Giangvienid) REFERENCES Giangvien(Id)
);

CREATE TABLE Ketqua (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL UNIQUE,
    Diemcuoi DECIMAL(4,2),
    Dacongbo BOOLEAN DEFAULT FALSE,
    Ngaycongbo DATETIME NULL,
    FOREIGN KEY (Dangkydetaiid) REFERENCES Dangkydetai(Id)
);

CREATE TABLE Thongbao (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tieude VARCHAR(255) NOT NULL,
    Noidung TEXT NOT NULL,
    Nguoidangid BIGINT NOT NULL,
    Ngaydang DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Nguoidangid) REFERENCES Taikhoan(Id)
);

INSERT INTO Bomon(Id,Mabomon,Tenbomon) VALUES
(1,'CNPM','Cong nghe phan mem'),
(2,'HTTT','He thong thong tin'),
(3,'AI','Tri tue nhan tao'),
(4,'MMT','Mang may tinh');

INSERT INTO Taikhoan(Id,Tendangnhap,Matkhau,Hoten,Email,Vaitro,Kichhoat) VALUES
(1,'dean','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Minh Khoa','dean@cntt.edu.vn','TRUONGKHOA',TRUE),
(2,'gv001','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Hoang Nam','gv001@cntt.edu.vn','TRUONGBOMON',TRUE),
(3,'gv002','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Quang Huy','gv002@cntt.edu.vn','TRUONGBOMON',TRUE),
(4,'gv003','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Thu Ha','gv003@cntt.edu.vn','TRUONGBOMON',TRUE),
(5,'gv004','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Thanh Tung','gv004@cntt.edu.vn','TRUONGBOMON',TRUE),
(6,'gv005','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Ngoc Lan','gv005@cntt.edu.vn','GIANGVIEN',TRUE),
(7,'gv006','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Do Quoc Khanh','gv006@cntt.edu.vn','GIANGVIEN',TRUE),
(8,'gv007','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Bui Minh Anh','gv007@cntt.edu.vn','GIANGVIEN',TRUE),
(9,'gv008','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Gia Bao','gv008@cntt.edu.vn','GIANGVIEN',TRUE),
(10,'gv009','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Thanh Son','gv009@cntt.edu.vn','GIANGVIEN',TRUE),
(11,'gv010','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Duc Long','gv010@cntt.edu.vn','GIANGVIEN',TRUE),
(12,'gv011','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Phan Thi Mai','gv011@cntt.edu.vn','GIANGVIEN',TRUE),
(101,'23110001','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Van An','23110001@student.edu.vn','SINHVIEN',TRUE),
(102,'23110002','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Minh Anh','23110002@student.edu.vn','SINHVIEN',TRUE),
(103,'23110003','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Hoang Bao','23110003@student.edu.vn','SINHVIEN',TRUE),
(104,'23110004','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Gia Han','23110004@student.edu.vn','SINHVIEN',TRUE),
(105,'23110005','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Quoc Huy','23110005@student.edu.vn','SINHVIEN',TRUE),
(106,'23110006','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Khanh Linh','23110006@student.edu.vn','SINHVIEN',TRUE),
(107,'23110007','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Nhat Minh','23110007@student.edu.vn','SINHVIEN',TRUE),
(108,'23110008','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Thao My','23110008@student.edu.vn','SINHVIEN',TRUE),
(109,'23110009','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Duc Phuc','23110009@student.edu.vn','SINHVIEN',TRUE),
(110,'23110010','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Minh Quan','23110010@student.edu.vn','SINHVIEN',TRUE),
(111,'23110011','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Thanh Tam','23110011@student.edu.vn','SINHVIEN',TRUE),
(112,'23110012','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Ngoc Vy','23110012@student.edu.vn','SINHVIEN',TRUE),
(113,'23110013','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Gia Bao','23110013@student.edu.vn','SINHVIEN',TRUE),
(114,'23110014','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Tuan Kiet','23110014@student.edu.vn','SINHVIEN',TRUE),
(115,'23110015','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Thuy Duong','23110015@student.edu.vn','SINHVIEN',TRUE),
(116,'23110016','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Hai Dang','23110016@student.edu.vn','SINHVIEN',TRUE),
(117,'23110017','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Quoc Viet','23110017@student.edu.vn','SINHVIEN',TRUE),
(118,'23110018','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Minh Chau','23110018@student.edu.vn','SINHVIEN',TRUE),
(119,'23110019','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Anh Thu','23110019@student.edu.vn','SINHVIEN',TRUE),
(120,'23110020','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Gia Minh','23110020@student.edu.vn','SINHVIEN',TRUE),
(121,'23110021','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Hoai Nam','23110021@student.edu.vn','SINHVIEN',TRUE),
(122,'23110022','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Phuong Uyen','23110022@student.edu.vn','SINHVIEN',TRUE),
(123,'23110023','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Duc Anh','23110023@student.edu.vn','SINHVIEN',TRUE),
(124,'23110024','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Quynh Nhu','23110024@student.edu.vn','SINHVIEN',TRUE),
(125,'23110025','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Nhat Hao','23110025@student.edu.vn','SINHVIEN',TRUE),
(126,'23110026','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Thanh Dat','23110026@student.edu.vn','SINHVIEN',TRUE),
(127,'23110027','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Bao Ngoc','23110027@student.edu.vn','SINHVIEN',TRUE),
(128,'23110028','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Huu Phuoc','23110028@student.edu.vn','SINHVIEN',TRUE),
(129,'23110029','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Minh Khang','23110029@student.edu.vn','SINHVIEN',TRUE),
(130,'23110030','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Ngoc Mai','23110030@student.edu.vn','SINHVIEN',TRUE),
(131,'23110031','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Anh Khoa','23110031@student.edu.vn','SINHVIEN',TRUE),
(132,'23110032','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Tuan Anh','23110032@student.edu.vn','SINHVIEN',TRUE),
(133,'23110033','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Khanh Vy','23110033@student.edu.vn','SINHVIEN',TRUE),
(134,'23110034','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Duc Huy','23110034@student.edu.vn','SINHVIEN',TRUE),
(135,'23110035','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Phuong Linh','23110035@student.edu.vn','SINHVIEN',TRUE),
(136,'23110036','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Quoc Bao','23110036@student.edu.vn','SINHVIEN',TRUE),
(137,'23110037','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Gia Linh','23110037@student.edu.vn','SINHVIEN',TRUE),
(138,'23110038','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Minh Tri','23110038@student.edu.vn','SINHVIEN',TRUE),
(139,'23110039','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Bao Chau','23110039@student.edu.vn','SINHVIEN',TRUE),
(140,'23110040','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Hoang Nam','23110040@student.edu.vn','SINHVIEN',TRUE),
(141,'23110041','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Ngoc Anh','23110041@student.edu.vn','SINHVIEN',TRUE),
(142,'23110042','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Thu Trang','23110042@student.edu.vn','SINHVIEN',TRUE),
(143,'23110043','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Thanh Tung','23110043@student.edu.vn','SINHVIEN',TRUE),
(144,'23110044','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Hai Yen','23110044@student.edu.vn','SINHVIEN',TRUE),
(145,'23110045','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Tuan Khang','23110045@student.edu.vn','SINHVIEN',TRUE),
(146,'23110046','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen My Linh','23110046@student.edu.vn','SINHVIEN',TRUE),
(147,'23110047','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Minh Quan','23110047@student.edu.vn','SINHVIEN',TRUE),
(148,'23110048','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Bao Han','23110048@student.edu.vn','SINHVIEN',TRUE),
(149,'23110049','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Quoc Anh','23110049@student.edu.vn','SINHVIEN',TRUE),
(150,'23110050','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Anh Kiet','23110050@student.edu.vn','SINHVIEN',TRUE),
(151,'23110051','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Duc Minh','23110051@student.edu.vn','SINHVIEN',TRUE),
(152,'23110052','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Phuong Thao','23110052@student.edu.vn','SINHVIEN',TRUE),
(153,'23110053','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Gia Huy','23110053@student.edu.vn','SINHVIEN',TRUE),
(154,'23110054','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Nhat Linh','23110054@student.edu.vn','SINHVIEN',TRUE),
(155,'23110055','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Hoai Thu','23110055@student.edu.vn','SINHVIEN',TRUE),
(156,'23110056','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Nguyen Thanh Phong','23110056@student.edu.vn','SINHVIEN',TRUE),
(157,'23110057','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Tran Duc Thinh','23110057@student.edu.vn','SINHVIEN',TRUE),
(158,'23110058','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Le Ngoc Han','23110058@student.edu.vn','SINHVIEN',TRUE),
(159,'23110059','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Pham Minh Thu','23110059@student.edu.vn','SINHVIEN',TRUE),
(160,'23110060','$2y$10$3623ddbuke2GzQI7dDicneHEsJ8yMW1mi9LlRNBAQwDsALm5AK3z.','Vo Gia Bao','23110060@student.edu.vn','SINHVIEN',TRUE);

INSERT INTO Giangvien(Id,Taikhoanid,Magiangvien,Bomonid) VALUES
(1,1,'GV000',1),
(2,2,'GV001',1),
(3,3,'GV002',2),
(4,4,'GV003',3),
(5,5,'GV004',4),
(6,6,'GV005',1),
(7,7,'GV006',2),
(8,8,'GV007',3),
(9,9,'GV008',4),
(10,10,'GV009',1),
(11,11,'GV010',2),
(12,12,'GV011',3);

INSERT INTO Sinhvien(Id,Taikhoanid,Masinhvien,Bomonid,Lop) VALUES
(1,101,'23110001',1,'K23-1'),
(2,102,'23110002',2,'K23-2'),
(3,103,'23110003',3,'K23-3'),
(4,104,'23110004',4,'K23-4'),
(5,105,'23110005',1,'K23-1'),
(6,106,'23110006',2,'K23-2'),
(7,107,'23110007',3,'K23-3'),
(8,108,'23110008',4,'K23-4'),
(9,109,'23110009',1,'K23-1'),
(10,110,'23110010',2,'K23-2'),
(11,111,'23110011',3,'K23-3'),
(12,112,'23110012',4,'K23-4'),
(13,113,'23110013',1,'K23-1'),
(14,114,'23110014',2,'K23-2'),
(15,115,'23110015',3,'K23-3'),
(16,116,'23110016',4,'K23-4'),
(17,117,'23110017',1,'K23-1'),
(18,118,'23110018',2,'K23-2'),
(19,119,'23110019',3,'K23-3'),
(20,120,'23110020',4,'K23-4'),
(21,121,'23110021',1,'K23-1'),
(22,122,'23110022',2,'K23-2'),
(23,123,'23110023',3,'K23-3'),
(24,124,'23110024',4,'K23-4'),
(25,125,'23110025',1,'K23-1'),
(26,126,'23110026',2,'K23-2'),
(27,127,'23110027',3,'K23-3'),
(28,128,'23110028',4,'K23-4'),
(29,129,'23110029',1,'K23-1'),
(30,130,'23110030',2,'K23-2'),
(31,131,'23110031',3,'K23-3'),
(32,132,'23110032',4,'K23-4'),
(33,133,'23110033',1,'K23-1'),
(34,134,'23110034',2,'K23-2'),
(35,135,'23110035',3,'K23-3'),
(36,136,'23110036',4,'K23-4'),
(37,137,'23110037',1,'K23-1'),
(38,138,'23110038',2,'K23-2'),
(39,139,'23110039',3,'K23-3'),
(40,140,'23110040',4,'K23-4'),
(41,141,'23110041',1,'K23-1'),
(42,142,'23110042',2,'K23-2'),
(43,143,'23110043',3,'K23-3'),
(44,144,'23110044',4,'K23-4'),
(45,145,'23110045',1,'K23-1'),
(46,146,'23110046',2,'K23-2'),
(47,147,'23110047',3,'K23-3'),
(48,148,'23110048',4,'K23-4'),
(49,149,'23110049',1,'K23-1'),
(50,150,'23110050',2,'K23-2'),
(51,151,'23110051',3,'K23-3'),
(52,152,'23110052',4,'K23-4'),
(53,153,'23110053',1,'K23-1'),
(54,154,'23110054',2,'K23-2'),
(55,155,'23110055',3,'K23-3'),
(56,156,'23110056',4,'K23-4'),
(57,157,'23110057',1,'K23-1'),
(58,158,'23110058',2,'K23-2'),
(59,159,'23110059',3,'K23-3'),
(60,160,'23110060',4,'K23-4');

INSERT INTO Dotdangky(Id,Tendot,Loaidot,Batdaugiangvien,Ketthucgiangvien,Batdausinhvien,Ketthucsinhvien,Hanchamphanbien,Ngaybaocaohoidong,Trangthai) VALUES
(1,'Dot mon Lap trinh Web HK1 2026-2027','MONHOC','2026-08-01 08:00:00','2026-08-10 17:00:00','2026-08-11 08:00:00','2026-08-20 17:00:00',NULL,NULL,'DONG'),
(2,'Dot NCKH sinh vien 2026-2027','NCKH','2026-08-15 08:00:00','2026-08-25 17:00:00','2026-08-26 08:00:00','2026-09-05 17:00:00',NULL,NULL,'DONG'),
(3,'Dot TLCN HK1 2026-2027','TLCN','2026-07-01 08:00:00','2026-07-10 17:00:00','2026-07-11 08:00:00','2026-07-20 17:00:00','2026-11-20 17:00:00',NULL,'DONG'),
(4,'Dot KLTN 2026-2027','KLTN','2026-08-20 08:00:00','2026-09-01 17:00:00','2026-09-02 08:00:00','2026-09-15 17:00:00','2026-12-10 17:00:00','2026-12-20 08:00:00','DONG'),
(5,'Dot mon Cong nghe phan mem HK2 2026-2027','MONHOC','2027-01-05 08:00:00','2027-01-15 17:00:00','2027-01-16 08:00:00','2027-01-25 17:00:00',NULL,NULL,'NHAP'),
(6,'Dot NCKH sinh vien 2027','NCKH','2027-02-01 08:00:00','2027-02-15 17:00:00','2027-02-16 08:00:00','2027-03-01 17:00:00',NULL,NULL,'NHAP');

INSERT INTO Detai(Id,Dotdangkyid,Bomonid,Tendetai,Mota,Giangviendexuatid,Trangthai) VALUES
(1,1,1,'He thong quan ly de tai sinh vien','Mo ta chi tiet cho He thong quan ly de tai sinh vien',2,'CONGBO'),
(2,2,2,'He thong dat phong hoc truc tuyen','Mo ta chi tiet cho He thong dat phong hoc truc tuyen',3,'CONGBO'),
(3,3,3,'Cong thong tin sinh vien','Mo ta chi tiet cho Cong thong tin sinh vien',4,'CONGBO'),
(4,4,4,'He thong quan ly thu vien','Mo ta chi tiet cho He thong quan ly thu vien',5,'CONGBO'),
(5,1,1,'He thong quan ly thuc tap','Mo ta chi tiet cho He thong quan ly thuc tap',6,'CONGBO'),
(6,2,2,'He thong quan ly diem danh','Mo ta chi tiet cho He thong quan ly diem danh',7,'CONGBO'),
(7,3,3,'Phan tich du lieu hoc tap','Mo ta chi tiet cho Phan tich du lieu hoc tap',8,'CONGBO'),
(8,4,4,'He thong canh bao hoc vu','Mo ta chi tiet cho He thong canh bao hoc vu',9,'CONGBO'),
(9,1,1,'Nhan dien bien bao giao thong','Mo ta chi tiet cho Nhan dien bien bao giao thong',10,'CONGBO'),
(10,2,2,'Tro ly hoc tap AI','Mo ta chi tiet cho Tro ly hoc tap AI',11,'CONGBO'),
(11,3,3,'He thong tu van mon hoc','Mo ta chi tiet cho He thong tu van mon hoc',12,'CONGBO'),
(12,4,4,'Nhan dien khuon mat diem danh','Mo ta chi tiet cho Nhan dien khuon mat diem danh',2,'CONGBO'),
(13,1,1,'He thong tuyen dung thong minh','Mo ta chi tiet cho He thong tuyen dung thong minh',3,'CONGBO'),
(14,2,2,'He thong danh gia CV','Mo ta chi tiet cho He thong danh gia CV',4,'CONGBO'),
(15,3,3,'Theo doi tien do do an','Mo ta chi tiet cho Theo doi tien do do an',5,'CONGBO'),
(16,4,4,'He thong quan ly bai tap lon','Mo ta chi tiet cho He thong quan ly bai tap lon',6,'CONGBO'),
(17,1,1,'Nen tang quan ly bat dong san','Mo ta chi tiet cho Nen tang quan ly bat dong san',7,'CONGBO'),
(18,2,2,'He thong quan ly khach hang','Mo ta chi tiet cho He thong quan ly khach hang',8,'CONGBO'),
(19,3,3,'He thong phan tich ket qua hoc tap','Mo ta chi tiet cho He thong phan tich ket qua hoc tap',9,'CONGBO'),
(20,4,4,'He thong goi y de tai','Mo ta chi tiet cho He thong goi y de tai',10,'CONGBO'),
(21,1,1,'He thong quan ly phong thi','Mo ta chi tiet cho He thong quan ly phong thi',11,'CONGBO'),
(22,2,2,'He thong quan ly tai san','Mo ta chi tiet cho He thong quan ly tai san',12,'CONGBO'),
(23,3,3,'He thong quan ly su kien sinh vien','Mo ta chi tiet cho He thong quan ly su kien sinh vien',2,'CONGBO'),
(24,4,4,'He thong thong bao hoc vu','Mo ta chi tiet cho He thong thong bao hoc vu',3,'CONGBO');

INSERT INTO Giangvienhuongdan(Detaiid,Giangvienid) VALUES
(1,2),
(2,3),
(3,4),
(3,5),
(4,5),
(5,6),
(6,7),
(6,8),
(7,8),
(8,9),
(9,10),
(9,11),
(10,11),
(11,12),
(12,2),
(12,3),
(13,3),
(14,4),
(15,5),
(15,6),
(16,6),
(17,7),
(18,8),
(18,9),
(19,9),
(20,10),
(21,11),
(21,12),
(22,12),
(23,2),
(24,3),
(24,4);

INSERT INTO Nhomsinhvien(Id,Dotdangkyid,Tennhom,Nhomtruongid) VALUES
(1,1,'Nhom 01',1),
(2,2,'Nhom 02',4),
(3,3,'Nhom 03',7),
(4,4,'Nhom 04',10),
(5,1,'Nhom 05',13),
(6,2,'Nhom 06',16),
(7,3,'Nhom 07',19),
(8,4,'Nhom 08',22),
(9,1,'Nhom 09',25),
(10,2,'Nhom 10',28),
(11,3,'Nhom 11',31),
(12,4,'Nhom 12',34),
(13,1,'Nhom 13',37),
(14,2,'Nhom 14',40),
(15,3,'Nhom 15',43),
(16,4,'Nhom 16',46),
(17,1,'Nhom 17',49),
(18,2,'Nhom 18',52),
(19,3,'Nhom 19',55),
(20,4,'Nhom 20',58);

INSERT INTO Thanhviennhom(Nhomid,Sinhvienid) VALUES
(1,1),
(1,2),
(1,3),
(2,4),
(2,5),
(2,6),
(3,7),
(3,8),
(3,9),
(4,10),
(4,11),
(4,12),
(5,13),
(5,14),
(5,15),
(6,16),
(6,17),
(6,18),
(7,19),
(7,20),
(7,21),
(8,22),
(8,23),
(8,24),
(9,25),
(9,26),
(9,27),
(10,28),
(10,29),
(10,30),
(11,31),
(11,32),
(11,33),
(12,34),
(12,35),
(12,36),
(13,37),
(13,38),
(13,39),
(14,40),
(14,41),
(14,42),
(15,43),
(15,44),
(15,45),
(16,46),
(16,47),
(16,48),
(17,49),
(17,50),
(17,51),
(18,52),
(18,53),
(18,54),
(19,55),
(19,56),
(19,57),
(20,58),
(20,59),
(20,60);

INSERT INTO Dangkydetai(Id,Nhomid,Detaiid,Trangthai,Ngaydangky) VALUES
(1,1,1,'DADUYET','2026-09-03 09:00:00'),
(2,2,2,'DADUYET','2026-09-04 09:00:00'),
(3,3,3,'DADUYET','2026-09-05 09:00:00'),
(4,4,4,'DADUYET','2026-09-06 09:00:00'),
(5,5,5,'DADUYET','2026-09-07 09:00:00'),
(6,6,6,'DADUYET','2026-09-08 09:00:00'),
(7,7,7,'DADUYET','2026-09-09 09:00:00'),
(8,8,8,'DADUYET','2026-09-10 09:00:00'),
(9,9,9,'DADUYET','2026-09-02 09:00:00'),
(10,10,10,'DADUYET','2026-09-03 09:00:00'),
(11,11,11,'DADUYET','2026-09-04 09:00:00'),
(12,12,12,'DADUYET','2026-09-05 09:00:00'),
(13,13,13,'DADUYET','2026-09-06 09:00:00'),
(14,14,14,'DADUYET','2026-09-07 09:00:00'),
(15,15,15,'DADUYET','2026-09-08 09:00:00'),
(16,16,16,'DADUYET','2026-09-09 09:00:00'),
(17,17,17,'DADUYET','2026-09-10 09:00:00'),
(18,18,18,'DADUYET','2026-09-02 09:00:00'),
(19,19,19,'DADUYET','2026-09-03 09:00:00'),
(20,20,20,'DADUYET','2026-09-04 09:00:00');

INSERT INTO Nopbaocao(Id,Dangkydetaiid,Sinhvienid,Duongdanfile,Ngaynop) VALUES
(1,1,1,'/uploads/nhom1/baocao.pdf','2026-10-01 20:00:00'),
(2,2,4,'/uploads/nhom2/baocao.pdf','2026-10-01 20:00:00'),
(3,3,7,'/uploads/nhom3/baocao.pdf','2026-10-01 20:00:00'),
(4,4,10,'/uploads/nhom4/baocao.pdf','2026-10-01 20:00:00'),
(5,5,13,'/uploads/nhom5/baocao.pdf','2026-10-01 20:00:00'),
(6,6,16,'/uploads/nhom6/baocao.pdf','2026-10-01 20:00:00'),
(7,7,19,'/uploads/nhom7/baocao.pdf','2026-10-01 20:00:00'),
(8,8,22,'/uploads/nhom8/baocao.pdf','2026-10-01 20:00:00'),
(9,9,25,'/uploads/nhom9/baocao.pdf','2026-10-01 20:00:00'),
(10,10,28,'/uploads/nhom10/baocao.pdf','2026-10-01 20:00:00'),
(11,11,31,'/uploads/nhom11/baocao.pdf','2026-10-01 20:00:00'),
(12,12,34,'/uploads/nhom12/baocao.pdf','2026-10-01 20:00:00'),
(13,13,37,'/uploads/nhom13/baocao.pdf','2026-10-01 20:00:00'),
(14,14,40,'/uploads/nhom14/baocao.pdf','2026-10-01 20:00:00'),
(15,15,43,'/uploads/nhom15/baocao.pdf','2026-10-01 20:00:00'),
(16,16,46,'/uploads/nhom16/baocao.pdf','2026-10-01 20:00:00'),
(17,17,49,'/uploads/nhom17/baocao.pdf','2026-10-01 20:00:00'),
(18,18,52,'/uploads/nhom18/baocao.pdf','2026-10-01 20:00:00'),
(19,19,55,'/uploads/nhom19/baocao.pdf','2026-10-01 20:00:00'),
(20,20,58,'/uploads/nhom20/baocao.pdf','2026-10-01 20:00:00');

INSERT INTO Hoidong(Id,Dotdangkyid,Tenhoidong,Ngaybaocao,Phong) VALUES
(1,1,'Hoi dong 01','2026-12-11 08:00:00','A201'),
(2,2,'Hoi dong 02','2026-12-12 08:00:00','A202'),
(3,3,'Hoi dong 03','2026-12-13 08:00:00','A203'),
(4,4,'Hoi dong 04','2026-12-14 08:00:00','A204'),
(5,1,'Hoi dong 05','2026-12-15 08:00:00','A205'),
(6,2,'Hoi dong 06','2026-12-16 08:00:00','A206'),
(7,3,'Hoi dong 07','2026-12-17 08:00:00','A207'),
(8,4,'Hoi dong 08','2026-12-18 08:00:00','A208');

INSERT INTO Thanhvienhoidong(Hoidongid,Giangvienid,Vaitro) VALUES
(1,1,'CHUTICH'),
(1,2,'THUKY'),
(1,3,'THANHVIEN'),
(2,4,'CHUTICH'),
(2,5,'THUKY'),
(2,6,'THANHVIEN'),
(3,7,'CHUTICH'),
(3,8,'THUKY'),
(3,9,'THANHVIEN'),
(4,10,'CHUTICH'),
(4,11,'THUKY'),
(4,12,'THANHVIEN'),
(5,1,'CHUTICH'),
(5,2,'THUKY'),
(5,3,'THANHVIEN'),
(6,4,'CHUTICH'),
(6,5,'THUKY'),
(6,6,'THANHVIEN'),
(7,7,'CHUTICH'),
(7,8,'THUKY'),
(7,9,'THANHVIEN'),
(8,10,'CHUTICH'),
(8,11,'THUKY'),
(8,12,'THANHVIEN');

INSERT INTO Giangvienphanbien(Id,Dangkydetaiid,Giangvienid) VALUES
(1,1,5),
(2,2,6),
(3,3,7),
(4,4,8),
(5,5,9),
(6,6,10),
(7,7,11),
(8,8,12),
(9,9,1),
(10,10,2),
(11,11,3),
(12,12,4),
(13,13,5),
(14,14,6),
(15,15,7),
(16,16,8),
(17,17,9),
(18,18,10),
(19,19,11),
(20,20,12);

INSERT INTO Diem(Id,Dangkydetaiid,Giangvienid,Diemso,Nhanxet) VALUES
(1,1,6,7.7,'Nhan xet mau cho du lieu demo'),
(2,1,7,8.0,'Nhan xet mau cho du lieu demo'),
(3,1,8,8.3,'Nhan xet mau cho du lieu demo'),
(4,2,7,8.4,'Nhan xet mau cho du lieu demo'),
(5,2,8,8.7,'Nhan xet mau cho du lieu demo'),
(6,2,9,9.0,'Nhan xet mau cho du lieu demo'),
(7,3,8,9.1,'Nhan xet mau cho du lieu demo'),
(8,3,9,9.4,'Nhan xet mau cho du lieu demo'),
(9,3,10,7.2,'Nhan xet mau cho du lieu demo'),
(10,4,9,7.3,'Nhan xet mau cho du lieu demo'),
(11,4,10,7.6,'Nhan xet mau cho du lieu demo'),
(12,4,11,7.9,'Nhan xet mau cho du lieu demo'),
(13,5,10,8.0,'Nhan xet mau cho du lieu demo'),
(14,5,11,8.3,'Nhan xet mau cho du lieu demo'),
(15,5,12,8.6,'Nhan xet mau cho du lieu demo'),
(16,6,11,8.7,'Nhan xet mau cho du lieu demo'),
(17,6,12,9.0,'Nhan xet mau cho du lieu demo'),
(18,6,1,9.3,'Nhan xet mau cho du lieu demo'),
(19,7,12,9.4,'Nhan xet mau cho du lieu demo'),
(20,7,1,7.2,'Nhan xet mau cho du lieu demo'),
(21,7,2,7.5,'Nhan xet mau cho du lieu demo'),
(22,8,1,7.6,'Nhan xet mau cho du lieu demo'),
(23,8,2,7.9,'Nhan xet mau cho du lieu demo'),
(24,8,3,8.2,'Nhan xet mau cho du lieu demo'),
(25,9,2,8.3,'Nhan xet mau cho du lieu demo'),
(26,9,3,8.6,'Nhan xet mau cho du lieu demo'),
(27,9,4,8.9,'Nhan xet mau cho du lieu demo'),
(28,10,3,9.0,'Nhan xet mau cho du lieu demo'),
(29,10,4,9.3,'Nhan xet mau cho du lieu demo'),
(30,10,5,7.1,'Nhan xet mau cho du lieu demo'),
(31,11,4,7.2,'Nhan xet mau cho du lieu demo'),
(32,11,5,7.5,'Nhan xet mau cho du lieu demo'),
(33,11,6,7.8,'Nhan xet mau cho du lieu demo'),
(34,12,5,7.9,'Nhan xet mau cho du lieu demo'),
(35,12,6,8.2,'Nhan xet mau cho du lieu demo'),
(36,12,7,8.5,'Nhan xet mau cho du lieu demo'),
(37,13,6,8.6,'Nhan xet mau cho du lieu demo'),
(38,13,7,8.9,'Nhan xet mau cho du lieu demo'),
(39,13,8,9.2,'Nhan xet mau cho du lieu demo'),
(40,14,7,9.3,'Nhan xet mau cho du lieu demo'),
(41,14,8,7.1,'Nhan xet mau cho du lieu demo'),
(42,14,9,7.4,'Nhan xet mau cho du lieu demo'),
(43,15,8,7.5,'Nhan xet mau cho du lieu demo'),
(44,15,9,7.8,'Nhan xet mau cho du lieu demo'),
(45,15,10,8.1,'Nhan xet mau cho du lieu demo'),
(46,16,9,8.2,'Nhan xet mau cho du lieu demo'),
(47,16,10,8.5,'Nhan xet mau cho du lieu demo'),
(48,16,11,8.8,'Nhan xet mau cho du lieu demo'),
(49,17,10,8.9,'Nhan xet mau cho du lieu demo'),
(50,17,11,9.2,'Nhan xet mau cho du lieu demo'),
(51,17,12,7.0,'Nhan xet mau cho du lieu demo'),
(52,18,11,7.1,'Nhan xet mau cho du lieu demo'),
(53,18,12,7.4,'Nhan xet mau cho du lieu demo'),
(54,18,1,7.7,'Nhan xet mau cho du lieu demo'),
(55,19,12,7.8,'Nhan xet mau cho du lieu demo'),
(56,19,1,8.1,'Nhan xet mau cho du lieu demo'),
(57,19,2,8.4,'Nhan xet mau cho du lieu demo'),
(58,20,1,8.5,'Nhan xet mau cho du lieu demo'),
(59,20,2,8.8,'Nhan xet mau cho du lieu demo'),
(60,20,3,9.1,'Nhan xet mau cho du lieu demo');

INSERT INTO Ketqua(Id,Dangkydetaiid,Diemcuoi,Dacongbo,Ngaycongbo) VALUES
(1,1,8.0,TRUE,'2026-12-25 08:00:00'),
(2,2,8.7,TRUE,'2026-12-25 08:00:00'),
(3,3,8.57,TRUE,'2026-12-25 08:00:00'),
(4,4,7.6,TRUE,'2026-12-25 08:00:00'),
(5,5,8.3,TRUE,'2026-12-25 08:00:00'),
(6,6,9.0,TRUE,'2026-12-25 08:00:00'),
(7,7,8.03,TRUE,'2026-12-25 08:00:00'),
(8,8,7.9,TRUE,'2026-12-25 08:00:00'),
(9,9,8.6,TRUE,'2026-12-25 08:00:00'),
(10,10,8.47,TRUE,'2026-12-25 08:00:00'),
(11,11,7.5,TRUE,'2026-12-25 08:00:00'),
(12,12,8.2,TRUE,'2026-12-25 08:00:00'),
(13,13,8.9,TRUE,'2026-12-25 08:00:00'),
(14,14,7.93,TRUE,'2026-12-25 08:00:00'),
(15,15,7.8,TRUE,'2026-12-25 08:00:00'),
(16,16,8.5,TRUE,'2026-12-25 08:00:00'),
(17,17,8.37,TRUE,'2026-12-25 08:00:00'),
(18,18,7.4,TRUE,'2026-12-25 08:00:00'),
(19,19,8.1,TRUE,'2026-12-25 08:00:00'),
(20,20,8.8,TRUE,'2026-12-25 08:00:00');

INSERT INTO Thongbao(Id,Tieude,Noidung,Nguoidangid) VALUES
(1,'Mo dot dang ky de tai','Giang vien dang ky de tai dung thoi gian.',1),
(2,'Cong bo danh sach de tai','Danh sach de tai da duoc cong bo.',1),
(3,'Mo dang ky cho sinh vien','Cac nhom sinh vien dang ky de tai da cong bo.',1),
(4,'Thong bao NCKH','Khoa CNTT trien khai dot NCKH.',1),
(5,'Nhac han phan bien TLCN','GVPB hoan tat cham diem dung han.',1),
(6,'Lich bao cao KLTN','Lich bao cao hoi dong KLTN da duoc cong bo.',1),
(7,'Thong bao chung','Sinh vien theo doi thong bao tren he thong.',1),
(8,'Cong bo ket qua','Ket qua de tai da duoc cong bo.',1),
(9,'Cap nhat danh sach hoi dong','Danh sach hoi dong da duoc cap nhat.',1),
(10,'Nhac nop bao cao','Nhom truong nop bao cao dung han.',1);

SET FOREIGN_KEY_CHECKS = 1;

-- TEST NHANH
SELECT COUNT(*) AS Sobomon FROM Bomon;
SELECT COUNT(*) AS Sotaikhoan FROM Taikhoan;
SELECT COUNT(*) AS Sogiangvien FROM Giangvien;
SELECT COUNT(*) AS Sosinhvien FROM Sinhvien;
SELECT COUNT(*) AS Sodotdangky FROM Dotdangky;
SELECT COUNT(*) AS Sodetai FROM Detai;
SELECT COUNT(*) AS Sonhom FROM Nhomsinhvien;
SELECT COUNT(*) AS Sodangky FROM Dangkydetai;
SELECT COUNT(*) AS Sodiem FROM Diem;
SELECT COUNT(*) AS Soketqua FROM Ketqua;
