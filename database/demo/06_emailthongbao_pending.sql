-- REVIEW ONLY. Separate optional migration; NOT sourced by init/reset/fixture scripts.
-- Apply only AFTER user approval, with --database=quanlydetaisinhvien_demo.
-- Fully qualified names protect the original/shared database. Fails if table already exists.
-- Sinhvienid is a historical snapshot; no FK may block the student module's deletes.
CREATE TABLE quanlydetaisinhvien_demo.emailthongbao (
    Id BIGINT NOT NULL AUTO_INCREMENT,
    Thongbaoid BIGINT NULL,
    Thongbaosnapshotid BIGINT NOT NULL,
    Sinhvienid BIGINT NOT NULL,
    Nguoinhan VARCHAR(200) NOT NULL,
    Email VARCHAR(150) NULL,
    Tieude VARCHAR(320) NOT NULL,
    Noidung TEXT NOT NULL,
    Trangthai ENUM('PENDING','SENDING','SENT','FAILED','MISSING_EMAIL','UNCERTAIN') NOT NULL,
    Solanthu INT NOT NULL DEFAULT 0,
    Ngaytao DATETIME NOT NULL,
    Lanthu DATETIME NULL,
    Ngaygui DATETIME NULL,
    Loi VARCHAR(500) NULL,
    PRIMARY KEY (Id),
    UNIQUE KEY Uk_emailthongbao_recipient (Thongbaosnapshotid,Sinhvienid),
    KEY Ix_emailthongbao_pending (Trangthai,Id),
    KEY Ix_emailthongbao_lease (Trangthai,Lanthu),
    CONSTRAINT Fk_emailthongbao_notice FOREIGN KEY (Thongbaoid) REFERENCES quanlydetaisinhvien_demo.thongbao(Id) ON DELETE SET NULL,
    CONSTRAINT Chk_emailthongbao_attempt CHECK (Solanthu >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
