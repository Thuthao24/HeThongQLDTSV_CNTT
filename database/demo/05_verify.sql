-- READ ONLY. Expected initial counts: see demo_manifest.json. No query touches the original DB.
SELECT DATABASE() AS Selected_database, CURRENT_USER() AS Connected_account;
SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'quanlydetaisinhvien_demo' ORDER BY TABLE_NAME;

SELECT 'taikhoan' AS Table_name, COUNT(*) AS Exact_rows FROM quanlydetaisinhvien_demo.taikhoan
UNION ALL SELECT 'sinhvien', COUNT(*) FROM quanlydetaisinhvien_demo.sinhvien
UNION ALL SELECT 'dangkydetai', COUNT(*) FROM quanlydetaisinhvien_demo.dangkydetai
UNION ALL SELECT 'diem', COUNT(*) FROM quanlydetaisinhvien_demo.diem
UNION ALL SELECT 'ketqua', COUNT(*) FROM quanlydetaisinhvien_demo.ketqua
UNION ALL SELECT 'phanconghoidong', COUNT(*) FROM quanlydetaisinhvien_demo.phanconghoidong
UNION ALL SELECT 'emailketqua', COUNT(*) FROM quanlydetaisinhvien_demo.emailketqua;

-- UNION deduplicates a lecturer who is both reviewer and council member.
WITH required_graders AS (
 SELECT Dangkydetaiid, Giangvienid FROM quanlydetaisinhvien_demo.giangvienphanbien
 UNION
 SELECT p.Dangkydetaiid, m.Giangvienid
 FROM quanlydetaisinhvien_demo.phanconghoidong p
 JOIN quanlydetaisinhvien_demo.thanhvienhoidong m ON m.Hoidongid = p.Hoidongid
 WHERE p.Hinhthuc = 'COHOIDONG'
)
SELECT r.Dangkydetaiid, r.Giangvienid AS Missing_grader
FROM required_graders r LEFT JOIN quanlydetaisinhvien_demo.diem d
 ON d.Dangkydetaiid = r.Dangkydetaiid AND d.Giangvienid = r.Giangvienid
WHERE d.Id IS NULL ORDER BY r.Dangkydetaiid, r.Giangvienid;
-- Expected: registration 6 -> teacher 10; registration 8 -> teacher 18.

SELECT k.Id, dd.Loaidot, p.Hinhthuc, p.Hoidongid, COUNT(d.Id) AS Existing_scores,
 ROUND(AVG(d.Diemso), 2) AS Partial_average, k.Diemcuoi, k.Dacongbo, k.Ngaycongbo
FROM quanlydetaisinhvien_demo.ketqua k
JOIN quanlydetaisinhvien_demo.dangkydetai r ON r.Id = k.Dangkydetaiid
JOIN quanlydetaisinhvien_demo.detai t ON t.Id = r.Detaiid
JOIN quanlydetaisinhvien_demo.dotdangky dd ON dd.Id = t.Dotdangkyid
JOIN quanlydetaisinhvien_demo.phanconghoidong p ON p.Dangkydetaiid = r.Id
LEFT JOIN quanlydetaisinhvien_demo.diem d ON d.Dangkydetaiid = r.Id
GROUP BY k.Id, dd.Loaidot, p.Hinhthuc, p.Hoidongid, k.Diemcuoi, k.Dacongbo, k.Ngaycongbo ORDER BY k.Id;
-- A partial average is diagnostic, never a substitute for missing scores/final score.

-- Both queries must return zero rows.
SELECT d.Id AS Forbidden_supervisor_score FROM quanlydetaisinhvien_demo.diem d
JOIN quanlydetaisinhvien_demo.dangkydetai r ON r.Id = d.Dangkydetaiid
JOIN quanlydetaisinhvien_demo.giangvienhuongdan h ON h.Detaiid = r.Detaiid AND h.Giangvienid = d.Giangvienid;
SELECT Id, Email FROM quanlydetaisinhvien_demo.taikhoan WHERE Email NOT LIKE '%@example.invalid' OR Email IS NULL;

SELECT Hoidongid, COUNT(*) AS Members FROM quanlydetaisinhvien_demo.thanhvienhoidong GROUP BY Hoidongid;

-- Must return zero rows: an assigned council must have met before publication/demo now.
-- DATETIME is interpreted using the agreed demo server time zone UTC+07:00.
SELECT h.Id AS Council_id, h.Ngaybaocao, k.Id AS Result_id, k.Ngaycongbo
FROM quanlydetaisinhvien_demo.hoidong h
JOIN quanlydetaisinhvien_demo.phanconghoidong p ON p.Hoidongid = h.Id
JOIN quanlydetaisinhvien_demo.ketqua k ON k.Dangkydetaiid = p.Dangkydetaiid
WHERE h.Ngaybaocao IS NULL OR h.Ngaybaocao > CURRENT_TIMESTAMP
 OR (k.Dacongbo = TRUE AND (k.Ngaycongbo IS NULL OR h.Ngaybaocao > k.Ngaycongbo));

SELECT Id, Ketquaid, Sinhvienid, Email, Trangthai, Solanthu, Ngaytao, Ngaygui
FROM quanlydetaisinhvien_demo.emailketqua ORDER BY Id;
-- Empty initially; after publishing result 1: 3 PENDING rows, Solanthu=0, Ngaygui=NULL.
