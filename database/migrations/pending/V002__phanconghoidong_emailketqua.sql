-- REVIEW ONLY. Not run automatically and NOT approved for execution on live MySQL.
-- Adds two empty tables. No backfill, UPDATE, ALTER or modification of published results.
-- Confirm the target database and approve execution separately before using this file.

CREATE TABLE phanconghoidong (
    Dangkydetaiid BIGINT NOT NULL,
    Hinhthuc ENUM('COHOIDONG', 'KHONGHOIDONG') NOT NULL,
    Hoidongid BIGINT NULL,
    PRIMARY KEY (Dangkydetaiid),
    KEY Ix_pchd_hoidong (Hoidongid),
    CONSTRAINT Fk_pchd_dangky FOREIGN KEY (Dangkydetaiid) REFERENCES dangkydetai(Id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT Fk_pchd_hoidong FOREIGN KEY (Hoidongid) REFERENCES hoidong(Id),
    CONSTRAINT Chk_pchd_hinhthuc CHECK (
        (Hinhthuc = 'COHOIDONG' AND Hoidongid IS NOT NULL)
        OR (Hinhthuc = 'KHONGHOIDONG' AND Hoidongid IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE emailketqua (
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
    CONSTRAINT Fk_emailketqua_result FOREIGN KEY (Ketquaid) REFERENCES ketqua(Id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT Fk_emailketqua_student FOREIGN KEY (Sinhvienid) REFERENCES sinhvien(Id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
