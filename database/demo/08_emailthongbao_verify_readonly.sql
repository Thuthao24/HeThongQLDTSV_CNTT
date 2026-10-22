-- READ ONLY. Run ONLY after the user has approved and applied 06_emailthongbao_pending.sql.
-- Re-run 07_emailthongbao_preflight_readonly.sql too, comparing old-table outputs.
SELECT DATABASE() AS selected_database;

SELECT COUNT(*) AS outbox_rows, COALESCE(SUM(Solanthu),0) AS attempts,
       COUNT(Ngaygui) AS sent_timestamps
FROM quanlydetaisinhvien_demo.emailthongbao;

SELECT Trangthai, COUNT(*) AS exact_rows
FROM quanlydetaisinhvien_demo.emailthongbao GROUP BY Trangthai ORDER BY Trangthai;

-- Must return zero rows: UNIQUE remains effective even if a notice/student is deleted.
SELECT Thongbaosnapshotid, Sinhvienid, COUNT(*) AS duplicates
FROM quanlydetaisinhvien_demo.emailthongbao
GROUP BY Thongbaosnapshotid, Sinhvienid HAVING COUNT(*)>1;

-- Must return zero rows: the sole live FK is Thongbaoid -> thongbao.Id.
SELECT e.Id AS broken_notice_reference
FROM quanlydetaisinhvien_demo.emailthongbao e
LEFT JOIN quanlydetaisinhvien_demo.thongbao t ON t.Id=e.Thongbaoid
WHERE e.Thongbaoid IS NOT NULL AND t.Id IS NULL;

-- Historical student IDs without live profiles are permitted; audit is intentionally retained.
SELECT COUNT(*) AS historical_recipients_without_live_profile
FROM quanlydetaisinhvien_demo.emailthongbao e
LEFT JOIN quanlydetaisinhvien_demo.sinhvien s ON s.Id=e.Sinhvienid
WHERE s.Id IS NULL;

SELECT Thongbaosnapshotid, COUNT(*) AS recipients,
       SUM(Trangthai='PENDING') AS pending, SUM(Trangthai='MISSING_EMAIL') AS missing_email,
       SUM(Solanthu) AS attempts, COUNT(Ngaygui) AS sent_timestamps
FROM quanlydetaisinhvien_demo.emailthongbao GROUP BY Thongbaosnapshotid ORDER BY Thongbaosnapshotid DESC;

SELECT Id, Thongbaoid, Thongbaosnapshotid, Sinhvienid, Trangthai, Solanthu, Ngaytao, Lanthu, Ngaygui
FROM quanlydetaisinhvien_demo.emailthongbao ORDER BY Id DESC LIMIT 20;
