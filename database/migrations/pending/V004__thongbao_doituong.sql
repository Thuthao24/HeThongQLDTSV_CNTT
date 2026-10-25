-- REVIEW / MANUAL APPROVAL ONLY. MySQL 8.0.16+ / InnoDB.
-- Apply after reviewing V002/V003 on an explicitly approved target, before deploying the new application.
-- No USE, auto runner, backfill, reset or mutation of existing notice/email/result data.
-- An existing notice with ZERO rows here remains visible to all three recipient roles.
CREATE TABLE thongbaodoituong (
    Thongbaoid BIGINT NOT NULL,
    Vaitro VARCHAR(20) NOT NULL,
    CONSTRAINT Uk_thongbaodoituong_role PRIMARY KEY (Thongbaoid,Vaitro),
    KEY Ix_thongbaodoituong_role (Vaitro,Thongbaoid),
    CONSTRAINT Fk_thongbaodoituong_notice FOREIGN KEY (Thongbaoid) REFERENCES thongbao(Id) ON DELETE CASCADE,
    CONSTRAINT Chk_thongbaodoituong_role CHECK (Vaitro IN ('SINHVIEN','GIANGVIEN','TRUONGBOMON'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
