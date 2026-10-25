-- Migration V006: Tao bang loimoithanhvien phuc vu tinh nang moi va xac nhan tham gia nhom
-- Ap dung sau V005 tren co so du lieu production (MySQL 8.0+ / InnoDB / utf8mb4)

CREATE TABLE IF NOT EXISTS loimoithanhvien (
    Id BIGINT AUTO_INCREMENT PRIMARY KEY,
    Nhomid BIGINT NOT NULL,
    Sinhvienid BIGINT NOT NULL,
    Nguoimoiid BIGINT NOT NULL,
    Trangthai VARCHAR(30) NOT NULL DEFAULT 'CHO_XAC_NHAN',
    Ngaytao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Ngayphanhoi DATETIME NULL,
    KEY Ix_loimoithanhvien_nhom (Nhomid),
    KEY Ix_loimoithanhvien_sinhvien (Sinhvienid, Trangthai),
    KEY Ix_loimoithanhvien_nguoimoi (Nguoimoiid),
    CONSTRAINT Fk_loimoithanhvien_nhom FOREIGN KEY (Nhomid) REFERENCES nhomsinhvien(Id) ON DELETE CASCADE,
    CONSTRAINT Fk_loimoithanhvien_sinhvien FOREIGN KEY (Sinhvienid) REFERENCES sinhvien(Id) ON DELETE CASCADE,
    CONSTRAINT Fk_loimoithanhvien_nguoimoi FOREIGN KEY (Nguoimoiid) REFERENCES sinhvien(Id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
