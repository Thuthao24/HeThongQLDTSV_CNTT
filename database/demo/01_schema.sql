-- REVIEW ONLY: not executed. All writes are qualified to quanlydetaisinhvien_demo.
-- Use a NEW dedicated connection with no pending transaction; DDL implicitly commits.
-- Workbench: stop on first error. Do not run 00-05 as one batch (04 is a RESET).
-- Fresh demo database only. Stop if any table already exists.
CREATE TABLE quanlydetaisinhvien_demo.bomon (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Mabomon VARCHAR(20) NOT NULL UNIQUE,
    Tenbomon VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.taikhoan (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tendangnhap VARCHAR(50) NOT NULL UNIQUE,
    Matkhau VARCHAR(100) NOT NULL,
    Hoten VARCHAR(150) NOT NULL,
    Email VARCHAR(150) UNIQUE,
    Vaitro ENUM('TRUONGKHOA','TRUONGBOMON','GIANGVIEN','SINHVIEN') NOT NULL,
    Kichhoat BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.giangvien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Taikhoanid BIGINT NOT NULL UNIQUE,
    Magiangvien VARCHAR(20) NOT NULL UNIQUE,
    Bomonid BIGINT NOT NULL,
    FOREIGN KEY (Taikhoanid) REFERENCES quanlydetaisinhvien_demo.taikhoan(Id),
    FOREIGN KEY (Bomonid) REFERENCES quanlydetaisinhvien_demo.bomon(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.sinhvien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Taikhoanid BIGINT NOT NULL UNIQUE,
    Masinhvien VARCHAR(20) NOT NULL UNIQUE,
    Bomonid BIGINT NOT NULL,
    Lop VARCHAR(50),
    FOREIGN KEY (Taikhoanid) REFERENCES quanlydetaisinhvien_demo.taikhoan(Id),
    FOREIGN KEY (Bomonid) REFERENCES quanlydetaisinhvien_demo.bomon(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.dotdangky (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.detai (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dotdangkyid BIGINT NOT NULL,
    Bomonid BIGINT NOT NULL,
    Tendetai VARCHAR(255) NOT NULL,
    Mota TEXT,
    Giangviendexuatid BIGINT NOT NULL,
    Trangthai ENUM('CHODUYET','DADUYET','CONGBO','TUCHOI') DEFAULT 'CHODUYET',
    FOREIGN KEY (Dotdangkyid) REFERENCES quanlydetaisinhvien_demo.dotdangky(Id),
    FOREIGN KEY (Bomonid) REFERENCES quanlydetaisinhvien_demo.bomon(Id),
    FOREIGN KEY (Giangviendexuatid) REFERENCES quanlydetaisinhvien_demo.giangvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.giangvienhuongdan (
    Detaiid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    PRIMARY KEY (Detaiid, Giangvienid),
    FOREIGN KEY (Detaiid) REFERENCES quanlydetaisinhvien_demo.detai(Id),
    FOREIGN KEY (Giangvienid) REFERENCES quanlydetaisinhvien_demo.giangvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.nhomsinhvien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dotdangkyid BIGINT NOT NULL,
    Tennhom VARCHAR(100) NOT NULL,
    Nhomtruongid BIGINT NOT NULL,
    FOREIGN KEY (Dotdangkyid) REFERENCES quanlydetaisinhvien_demo.dotdangky(Id),
    FOREIGN KEY (Nhomtruongid) REFERENCES quanlydetaisinhvien_demo.sinhvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.thanhviennhom (
    Nhomid BIGINT NOT NULL,
    Sinhvienid BIGINT NOT NULL,
    PRIMARY KEY (Nhomid, Sinhvienid),
    FOREIGN KEY (Nhomid) REFERENCES quanlydetaisinhvien_demo.nhomsinhvien(Id),
    FOREIGN KEY (Sinhvienid) REFERENCES quanlydetaisinhvien_demo.sinhvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.dangkydetai (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Nhomid BIGINT NOT NULL UNIQUE,
    Detaiid BIGINT NOT NULL,
    Trangthai ENUM('CHODUYET','DADUYET','TUCHOI') DEFAULT 'CHODUYET',
    Ngaydangky DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Nhomid) REFERENCES quanlydetaisinhvien_demo.nhomsinhvien(Id),
    FOREIGN KEY (Detaiid) REFERENCES quanlydetaisinhvien_demo.detai(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.nopbaocao (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL,
    Sinhvienid BIGINT NOT NULL,
    Duongdanfile VARCHAR(500) NOT NULL,
    Ngaynop DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Dangkydetaiid) REFERENCES quanlydetaisinhvien_demo.dangkydetai(Id),
    FOREIGN KEY (Sinhvienid) REFERENCES quanlydetaisinhvien_demo.sinhvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.hoidong (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dotdangkyid BIGINT NOT NULL,
    Tenhoidong VARCHAR(150) NOT NULL,
    Ngaybaocao DATETIME,
    Phong VARCHAR(50),
    FOREIGN KEY (Dotdangkyid) REFERENCES quanlydetaisinhvien_demo.dotdangky(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.thanhvienhoidong (
    Hoidongid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    Vaitro ENUM('CHUTICH','THUKY','THANHVIEN') NOT NULL,
    PRIMARY KEY (Hoidongid, Giangvienid),
    FOREIGN KEY (Hoidongid) REFERENCES quanlydetaisinhvien_demo.hoidong(Id),
    FOREIGN KEY (Giangvienid) REFERENCES quanlydetaisinhvien_demo.giangvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.giangvienphanbien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    UNIQUE (Dangkydetaiid, Giangvienid),
    FOREIGN KEY (Dangkydetaiid) REFERENCES quanlydetaisinhvien_demo.dangkydetai(Id),
    FOREIGN KEY (Giangvienid) REFERENCES quanlydetaisinhvien_demo.giangvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.diem (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL,
    Giangvienid BIGINT NOT NULL,
    Diemso DECIMAL(4,2) NOT NULL,
    CHECK (Diemso BETWEEN 0 AND 10),
    Nhanxet VARCHAR(500),
    UNIQUE (Dangkydetaiid, Giangvienid),
    FOREIGN KEY (Dangkydetaiid) REFERENCES quanlydetaisinhvien_demo.dangkydetai(Id),
    FOREIGN KEY (Giangvienid) REFERENCES quanlydetaisinhvien_demo.giangvien(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.ketqua (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Dangkydetaiid BIGINT NOT NULL UNIQUE,
    Diemcuoi DECIMAL(4,2),
    CHECK (Diemcuoi IS NULL OR Diemcuoi BETWEEN 0 AND 10),
    Dacongbo BOOLEAN NOT NULL DEFAULT FALSE,
    Ngaycongbo DATETIME NULL,
    FOREIGN KEY (Dangkydetaiid) REFERENCES quanlydetaisinhvien_demo.dangkydetai(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.thongbao (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Tieude VARCHAR(255) NOT NULL,
    Noidung TEXT NOT NULL,
    Nguoidangid BIGINT NOT NULL,
    Ngaydang DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (Nguoidangid) REFERENCES quanlydetaisinhvien_demo.taikhoan(Id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.phanconghoidong (
    Dangkydetaiid BIGINT NOT NULL,
    Hinhthuc ENUM('COHOIDONG', 'KHONGHOIDONG') NOT NULL,
    Hoidongid BIGINT NULL,
    PRIMARY KEY (Dangkydetaiid),
    KEY Ix_pchd_hoidong (Hoidongid),
    CONSTRAINT Fk_pchd_dangky FOREIGN KEY (Dangkydetaiid) REFERENCES quanlydetaisinhvien_demo.dangkydetai(Id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT Fk_pchd_hoidong FOREIGN KEY (Hoidongid) REFERENCES quanlydetaisinhvien_demo.hoidong(Id),
    CONSTRAINT Chk_pchd_hinhthuc CHECK (
        (Hinhthuc = 'COHOIDONG' AND Hoidongid IS NOT NULL)
        OR (Hinhthuc = 'KHONGHOIDONG' AND Hoidongid IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE quanlydetaisinhvien_demo.emailketqua (
    Id BIGINT NOT NULL AUTO_INCREMENT,
    Ketquaid BIGINT NOT NULL,
    Sinhvienid BIGINT NOT NULL,
    Email VARCHAR(150) NULL,
    Tieude VARCHAR(200) NOT NULL,
    Noidung TEXT NOT NULL,
    Trangthai ENUM('PENDING', 'SENDING', 'SENT', 'MISSING_EMAIL', 'FAILED', 'UNCERTAIN') NOT NULL,
    Solanthu INT NOT NULL DEFAULT 0,
    Ngaytao DATETIME NOT NULL,
    Lanthu DATETIME NULL,
    Ngaygui DATETIME NULL,
    Loi VARCHAR(500) NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY Uk_emailketqua_recipient (Ketquaid, Sinhvienid),
    KEY Ix_emailketqua_pending (Trangthai, Id),
    KEY Ix_emailketqua_lease (Trangthai, Lanthu),
    CONSTRAINT Fk_emailketqua_result FOREIGN KEY (Ketquaid) REFERENCES quanlydetaisinhvien_demo.ketqua(Id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT Fk_emailketqua_student FOREIGN KEY (Sinhvienid) REFERENCES quanlydetaisinhvien_demo.sinhvien(Id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
