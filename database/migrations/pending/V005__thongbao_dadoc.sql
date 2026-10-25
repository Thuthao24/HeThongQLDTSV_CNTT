-- REVIEW / MANUAL APPROVAL ONLY. Apply after V004 on an explicitly approved database.
-- STOP ALL notice writers until V005 commits and the matching application is deployed.
-- IDs are positive BIGINT AUTO_INCREMENT; gaps are allowed. Never reset/reuse IDs after activation.
-- No JVM startup initialization, no historical backfill, no email/result mutation.
CREATE TABLE thongbaodocmoc (
    Id INT NOT NULL,
    Thongbaoidmoc BIGINT NOT NULL,
    Ngaykichhoat DATETIME NOT NULL,
    PRIMARY KEY (Id),
    CONSTRAINT Chk_thongbaodocmoc_single CHECK (Id=1),
    CONSTRAINT Chk_thongbaodocmoc_boundary CHECK (Thongbaoidmoc>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE thongbaodadoc (
    Taikhoanid BIGINT NOT NULL,
    Thongbaoid BIGINT NOT NULL,
    Ngaydoc DATETIME NOT NULL,
    PRIMARY KEY (Taikhoanid,Thongbaoid),
    KEY Ix_thongbaodadoc_notice (Thongbaoid,Taikhoanid),
    CONSTRAINT Fk_thongbaodadoc_account FOREIGN KEY (Taikhoanid) REFERENCES taikhoan(Id) ON DELETE CASCADE,
    CONSTRAINT Fk_thongbaodadoc_notice FOREIGN KEY (Thongbaoid) REFERENCES thongbao(Id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Exactly once, while all notice writers are stopped. Do not recompute this value on restart/redeploy.
INSERT INTO thongbaodocmoc (Id,Thongbaoidmoc,Ngaykichhoat)
SELECT 1,COALESCE(MAX(Id),0),CURRENT_TIMESTAMP FROM thongbao;
