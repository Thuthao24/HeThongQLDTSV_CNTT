-- READ ONLY. Review before running against the DEMO connection. No schema/data writes.
-- Stop if selected_database is not exactly quanlydetaisinhvien_demo.
SELECT DATABASE() AS selected_database, VERSION() AS server_version,
       @@hostname AS server_host, @@port AS server_port,
       @@lower_case_table_names AS lower_case_table_names,
       @@transaction_isolation AS transaction_isolation, @@sql_mode AS sql_mode;

SELECT TABLE_NAME, TABLE_TYPE, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'quanlydetaisinhvien_demo'
  AND LOWER(TABLE_NAME) IN ('thongbao','sinhvien','taikhoan','emailthongbao','emailketqua','phanconghoidong')
ORDER BY TABLE_NAME;

SELECT COUNT(*) AS emailthongbao_tables_already_present
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'quanlydetaisinhvien_demo' AND LOWER(TABLE_NAME) = 'emailthongbao';

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, EXTRA, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'quanlydetaisinhvien_demo'
  AND LOWER(TABLE_NAME) IN ('thongbao','sinhvien','taikhoan','emailthongbao','emailketqua')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'quanlydetaisinhvien_demo'
  AND LOWER(TABLE_NAME) IN ('thongbao','sinhvien','emailthongbao','emailketqua')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- Includes existing student constraints from other modules; these are NOT changed by V003.
SELECT k.TABLE_NAME, k.CONSTRAINT_NAME, k.COLUMN_NAME,
       k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME, r.DELETE_RULE, r.UPDATE_RULE
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.REFERENTIAL_CONSTRAINTS r
  ON r.CONSTRAINT_SCHEMA=k.CONSTRAINT_SCHEMA AND r.CONSTRAINT_NAME=k.CONSTRAINT_NAME
  AND r.TABLE_NAME=k.TABLE_NAME
WHERE k.TABLE_SCHEMA='quanlydetaisinhvien_demo'
  AND k.REFERENCED_TABLE_SCHEMA='quanlydetaisinhvien_demo'
  AND (LOWER(k.TABLE_NAME) IN ('emailthongbao','emailketqua')
       OR LOWER(k.REFERENCED_TABLE_NAME)='sinhvien')
ORDER BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION;

SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, ENFORCED
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA='quanlydetaisinhvien_demo'
  AND (LOWER(TABLE_NAME)='emailthongbao'
       OR LOWER(CONSTRAINT_NAME) IN ('fk_emailthongbao_notice','chk_emailthongbao_attempt'))
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

SELECT t.TABLE_NAME, t.CONSTRAINT_NAME, c.CHECK_CLAUSE
FROM information_schema.TABLE_CONSTRAINTS t
JOIN information_schema.CHECK_CONSTRAINTS c
  ON c.CONSTRAINT_SCHEMA=t.CONSTRAINT_SCHEMA AND c.CONSTRAINT_NAME=t.CONSTRAINT_NAME
WHERE t.CONSTRAINT_SCHEMA='quanlydetaisinhvien_demo' AND LOWER(t.TABLE_NAME)='emailthongbao';

-- Only execute the following if the preceding metadata confirms these lowercase demo tables exist.
SELECT COUNT(*) AS eligible_recipient_profiles, COALESCE(MAX(s.Id),0) AS last_recipient_id
FROM quanlydetaisinhvien_demo.sinhvien s
JOIN quanlydetaisinhvien_demo.taikhoan a ON a.Id=s.Taikhoanid
WHERE a.Vaitro='SINHVIEN' AND a.Kichhoat=TRUE;

-- Save before/after outputs while the demo application is stopped.
SELECT 'thongbao' AS table_name, COUNT(*) AS exact_rows FROM quanlydetaisinhvien_demo.thongbao
UNION ALL SELECT 'sinhvien',COUNT(*) FROM quanlydetaisinhvien_demo.sinhvien
UNION ALL SELECT 'taikhoan',COUNT(*) FROM quanlydetaisinhvien_demo.taikhoan
UNION ALL SELECT 'emailketqua',COUNT(*) FROM quanlydetaisinhvien_demo.emailketqua
UNION ALL SELECT 'phanconghoidong',COUNT(*) FROM quanlydetaisinhvien_demo.phanconghoidong;

SELECT Trangthai, COUNT(*) AS exact_rows, COALESCE(SUM(Solanthu),0) AS attempts
FROM quanlydetaisinhvien_demo.emailketqua GROUP BY Trangthai ORDER BY Trangthai;

SELECT Id, Ketquaid, Sinhvienid, Trangthai, Solanthu, Ngaytao, Lanthu, Ngaygui
FROM quanlydetaisinhvien_demo.emailketqua ORDER BY Id;
