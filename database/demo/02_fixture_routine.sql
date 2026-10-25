-- REVIEW ONLY: not executed. All writes are qualified to quanlydetaisinhvien_demo.
-- Use a NEW dedicated connection with no pending transaction; DDL implicitly commits.
-- Workbench: stop on first error. Do not run 00-05 as one batch (04 is a RESET).
-- Stop the demo application before INIT/RESET. Never reset while workers/users are active.
DROP PROCEDURE IF EXISTS quanlydetaisinhvien_demo.hn_reset_demo_v2;
DELIMITER $$
CREATE PROCEDURE quanlydetaisinhvien_demo.hn_reset_demo_v2(IN selected_database VARCHAR(128),
    IN p_operation VARCHAR(8), IN confirmation VARCHAR(160))
SQL SECURITY INVOKER
BEGIN
    DECLARE existing_rows BIGINT DEFAULT 0;
    DECLARE transaction_started BOOLEAN DEFAULT FALSE;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        -- A schema/token error must not roll back the caller's unrelated transaction.
        IF transaction_started THEN ROLLBACK; END IF;
        RESIGNAL;
    END;
    -- DATABASE() inside a procedure refers to the routine schema. The caller passes its selection.
    IF COALESCE(selected_database, '') <> 'quanlydetaisinhvien_demo' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select quanlydetaisinhvien_demo first; nothing changed';
    END IF;
    IF COALESCE(p_operation, '') NOT IN ('INIT', 'RESET') OR
       CAST(COALESCE(confirmation, '') AS BINARY) <> CAST(CONCAT(p_operation, ' quanlydetaisinhvien_demo') AS BINARY) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Explicit demo INIT/RESET confirmation required; nothing changed';
    END IF;
    IF @@SESSION.autocommit <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use a fresh dedicated autocommit connection; nothing changed';
    END IF;
    -- No SESSION/GLOBAL: reject an already active transaction before START TRANSACTION
    -- could implicitly commit it. Does not change session/global transaction defaults.
    SET TRANSACTION READ WRITE;
    START TRANSACTION;
    SET transaction_started = TRUE;
    SELECT (SELECT COUNT(*) FROM quanlydetaisinhvien_demo.bomon) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.taikhoan) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.giangvien) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.sinhvien) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.dotdangky) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.detai) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.giangvienhuongdan) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.nhomsinhvien) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.thanhviennhom) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.dangkydetai) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.nopbaocao) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.hoidong) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.thanhvienhoidong) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.giangvienphanbien) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.diem) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.ketqua) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.thongbao) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.phanconghoidong) +
(SELECT COUNT(*) FROM quanlydetaisinhvien_demo.emailketqua) INTO existing_rows;
    IF p_operation = 'INIT' AND existing_rows <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'INIT requires all 19 demo tables to be empty; nothing changed';
    END IF;
-- BEGIN DEMO DATA
DELETE FROM quanlydetaisinhvien_demo.emailketqua;
DELETE FROM quanlydetaisinhvien_demo.phanconghoidong;
DELETE FROM quanlydetaisinhvien_demo.thongbao;
DELETE FROM quanlydetaisinhvien_demo.ketqua;
DELETE FROM quanlydetaisinhvien_demo.diem;
DELETE FROM quanlydetaisinhvien_demo.giangvienphanbien;
DELETE FROM quanlydetaisinhvien_demo.thanhvienhoidong;
DELETE FROM quanlydetaisinhvien_demo.hoidong;
DELETE FROM quanlydetaisinhvien_demo.nopbaocao;
DELETE FROM quanlydetaisinhvien_demo.dangkydetai;
DELETE FROM quanlydetaisinhvien_demo.thanhviennhom;
DELETE FROM quanlydetaisinhvien_demo.nhomsinhvien;
DELETE FROM quanlydetaisinhvien_demo.giangvienhuongdan;
DELETE FROM quanlydetaisinhvien_demo.detai;
DELETE FROM quanlydetaisinhvien_demo.dotdangky;
DELETE FROM quanlydetaisinhvien_demo.sinhvien;
DELETE FROM quanlydetaisinhvien_demo.giangvien;
DELETE FROM quanlydetaisinhvien_demo.taikhoan;
DELETE FROM quanlydetaisinhvien_demo.bomon;
INSERT INTO quanlydetaisinhvien_demo.bomon(Id,Mabomon,Tenbomon) VALUES
('1','DEMO_BM01','DEMO Bộ môn 01'),
('2','DEMO_BM02','DEMO Bộ môn 02'),
('3','DEMO_BM03','DEMO Bộ môn 03'),
('4','DEMO_BM04','DEMO Bộ môn 04'),
('5','DEMO_BM05','DEMO Bộ môn 05');
INSERT INTO quanlydetaisinhvien_demo.taikhoan(Id,Tendangnhap,Matkhau,Hoten,Email,Vaitro,Kichhoat) VALUES
('1','TK001','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO TK001','tk001@example.invalid','TRUONGKHOA','1'),
('2','GV001','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV001','gv001@example.invalid','GIANGVIEN','1'),
('3','GV002','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV002','gv002@example.invalid','GIANGVIEN','1'),
('4','GV003','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV003','gv003@example.invalid','GIANGVIEN','1'),
('5','GV004','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV004','gv004@example.invalid','GIANGVIEN','1'),
('6','GV005','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV005','gv005@example.invalid','GIANGVIEN','1'),
('7','GV006','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV006','gv006@example.invalid','GIANGVIEN','1'),
('8','GV007','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV007','gv007@example.invalid','GIANGVIEN','1'),
('9','GV008','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV008','gv008@example.invalid','GIANGVIEN','1'),
('10','GV009','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV009','gv009@example.invalid','GIANGVIEN','1'),
('11','GV010','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV010','gv010@example.invalid','GIANGVIEN','1'),
('12','GV011','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV011','gv011@example.invalid','GIANGVIEN','1'),
('13','GV012','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV012','gv012@example.invalid','GIANGVIEN','1'),
('14','GV013','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV013','gv013@example.invalid','GIANGVIEN','1'),
('15','GV014','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV014','gv014@example.invalid','GIANGVIEN','1'),
('16','GV015','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV015','gv015@example.invalid','GIANGVIEN','1'),
('17','GV016','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV016','gv016@example.invalid','GIANGVIEN','1'),
('18','GV017','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV017','gv017@example.invalid','GIANGVIEN','1'),
('19','GV018','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV018','gv018@example.invalid','GIANGVIEN','1'),
('20','GV019','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV019','gv019@example.invalid','GIANGVIEN','1'),
('21','GV020','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO GV020','gv020@example.invalid','GIANGVIEN','1'),
('1001','23110001','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110001','sv01@example.invalid','SINHVIEN','1'),
('1002','23110002','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110002','sv02@example.invalid','SINHVIEN','1'),
('1003','23110003','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110003','sv03@example.invalid','SINHVIEN','1'),
('1004','23110004','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110004','sv04@example.invalid','SINHVIEN','1'),
('1005','23110005','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110005','sv05@example.invalid','SINHVIEN','1'),
('1006','23110006','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110006','sv06@example.invalid','SINHVIEN','1'),
('1007','23110007','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110007','sv07@example.invalid','SINHVIEN','1'),
('1008','23110008','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110008','sv08@example.invalid','SINHVIEN','1'),
('1009','23110009','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110009','sv09@example.invalid','SINHVIEN','1'),
('1010','23110010','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110010','sv10@example.invalid','SINHVIEN','1'),
('1011','23110011','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110011','sv11@example.invalid','SINHVIEN','1'),
('1012','23110012','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110012','sv12@example.invalid','SINHVIEN','1'),
('1013','23110013','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110013','sv13@example.invalid','SINHVIEN','1'),
('1014','23110014','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110014','sv14@example.invalid','SINHVIEN','1'),
('1015','23110015','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110015','sv15@example.invalid','SINHVIEN','1'),
('1016','23110016','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110016','sv16@example.invalid','SINHVIEN','1'),
('1017','23110017','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110017','sv17@example.invalid','SINHVIEN','1'),
('1018','23110018','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110018','sv18@example.invalid','SINHVIEN','1'),
('1019','23110019','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110019','sv19@example.invalid','SINHVIEN','1'),
('1020','23110020','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110020','sv20@example.invalid','SINHVIEN','1'),
('1021','23110021','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110021','sv21@example.invalid','SINHVIEN','1'),
('1022','23110022','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110022','sv22@example.invalid','SINHVIEN','1'),
('1023','23110023','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110023','sv23@example.invalid','SINHVIEN','1'),
('1024','23110024','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Sinh viên 23110024','sv24@example.invalid','SINHVIEN','1'),
('2001','demo_truongbomon','$2a$10$e5fNVLLU9eplXjbJzUCjX.oUs.YDcvXug2Fxcuch4yTy0d1rdFtMG','DEMO Trưởng bộ môn','demo-tbm@example.invalid','TRUONGBOMON','1');
INSERT INTO quanlydetaisinhvien_demo.giangvien(Id,Taikhoanid,Magiangvien,Bomonid) VALUES
('1','1','TK001','1'),
('2','2','GV001','1'),
('3','3','GV002','2'),
('4','4','GV003','3'),
('5','5','GV004','4'),
('6','6','GV005','5'),
('7','7','GV006','1'),
('8','8','GV007','2'),
('9','9','GV008','3'),
('10','10','GV009','4'),
('11','11','GV010','5'),
('12','12','GV011','1'),
('13','13','GV012','2'),
('14','14','GV013','3'),
('15','15','GV014','4'),
('16','16','GV015','5'),
('17','17','GV016','1'),
('18','18','GV017','2'),
('19','19','GV018','3'),
('20','20','GV019','4'),
('21','21','GV020','5');
INSERT INTO quanlydetaisinhvien_demo.sinhvien(Id,Taikhoanid,Masinhvien,Bomonid,Lop) VALUES
('1','1001','23110001','1','23DT2'),
('2','1002','23110002','2','23ET3'),
('3','1003','23110003','3','23FT4'),
('4','1004','23110004','4','23GT5'),
('5','1005','23110005','5','23HT1'),
('6','1006','23110006','1','23DT2'),
('7','1007','23110007','2','23ET3'),
('8','1008','23110008','3','23FT4'),
('9','1009','23110009','4','23GT5'),
('10','1010','23110010','5','23HT1'),
('11','1011','23110011','1','23DT2'),
('12','1012','23110012','2','23ET3'),
('13','1013','23110013','3','23FT4'),
('14','1014','23110014','4','23GT5'),
('15','1015','23110015','5','23HT1'),
('16','1016','23110016','1','23DT2'),
('17','1017','23110017','2','23ET3'),
('18','1018','23110018','3','23FT4'),
('19','1019','23110019','4','23GT5'),
('20','1020','23110020','5','23HT1'),
('21','1021','23110021','1','23DT2'),
('22','1022','23110022','2','23ET3'),
('23','1023','23110023','3','23FT4'),
('24','1024','23110024','4','23GT5');
INSERT INTO quanlydetaisinhvien_demo.dotdangky(Id,Tendot,Loaidot,Batdaugiangvien,Ketthucgiangvien,Batdausinhvien,Ketthucsinhvien,Hanchamphanbien,Ngaybaocaohoidong,Trangthai) VALUES
('1','DEMO MONHOC','MONHOC','2026-08-01 00:00:00','2026-08-31 23:59:59','2026-10-01 00:00:00','2026-10-09 23:59:59',NULL,NULL,'DONG'),
('2','DEMO NCKH','NCKH','2026-08-01 00:00:00','2026-08-31 23:59:59','2026-10-01 00:00:00','2026-10-09 23:59:59',NULL,NULL,'DONG'),
('3','DEMO TLCN','TLCN','2026-08-01 00:00:00','2026-08-31 23:59:59','2026-10-01 00:00:00','2026-10-09 23:59:59',NULL,NULL,'DONG'),
('4','DEMO KLTN','KLTN','2026-08-01 00:00:00','2026-08-31 23:59:59','2026-10-01 00:00:00','2026-10-09 23:59:59',NULL,NULL,'DONG');
INSERT INTO quanlydetaisinhvien_demo.detai(Id,Dotdangkyid,Bomonid,Tendetai,Giangviendexuatid,Trangthai) VALUES
('1','1','1','Hệ thống quản lý đề tài sinh viên','2','CONGBO'),
('2','2','2','Cổng thông tin đăng ký khóa luận tốt nghiệp','3','CONGBO'),
('3','3','3','Hệ thống quản lý thực tập doanh nghiệp','4','CONGBO'),
('4','4','4','Hệ thống quản lý phòng học thông minh','5','CONGBO'),
('5','1','5','Hệ thống quản lý thư viện số','6','CONGBO'),
('6','2','1','Website thương mại điện tử đa nhà bán','7','CONGBO'),
('7','3','2','Hệ thống quản lý tuyển dụng thông minh','8','CONGBO'),
('8','4','3','Nền tảng quản lý bất động sản','9','CONGBO');
INSERT INTO quanlydetaisinhvien_demo.giangvienhuongdan(Detaiid,Giangvienid) VALUES
('1','2'),
('2','3'),
('3','4'),
('4','5'),
('4','11'),
('5','6'),
('6','7'),
('7','8'),
('8','9'),
('8','15');
INSERT INTO quanlydetaisinhvien_demo.nhomsinhvien(Id,Dotdangkyid,Tennhom,Nhomtruongid) VALUES
('1','1','Nhóm CNTT 01','1'),
('2','2','Nhóm CNTT 02','4'),
('3','3','Nhóm CNTT 03','7'),
('4','4','Nhóm CNTT 04','10'),
('5','1','Nhóm CNTT 05','13'),
('6','2','Nhóm CNTT 06','16'),
('7','3','Nhóm CNTT 07','19'),
('8','4','Nhóm CNTT 08','22');
INSERT INTO quanlydetaisinhvien_demo.thanhviennhom(Nhomid,Sinhvienid) VALUES
('1','1'),
('1','2'),
('1','3'),
('2','4'),
('2','5'),
('2','6'),
('3','7'),
('3','8'),
('3','9'),
('4','10'),
('4','11'),
('4','12'),
('5','13'),
('5','14'),
('5','15'),
('6','16'),
('6','17'),
('6','18'),
('7','19'),
('7','20'),
('7','21'),
('8','22'),
('8','23'),
('8','24');
INSERT INTO quanlydetaisinhvien_demo.dangkydetai(Id,Nhomid,Detaiid,Trangthai,Ngaydangky) VALUES
('1','1','1','DADUYET','2026-10-02 09:00:00'),
('2','2','2','DADUYET','2026-10-03 09:00:00'),
('3','3','3','DADUYET','2026-10-04 09:00:00'),
('4','4','4','DADUYET','2026-10-05 09:00:00'),
('5','5','5','DADUYET','2026-10-06 09:00:00'),
('6','6','6','DADUYET','2026-10-07 09:00:00'),
('7','7','7','DADUYET','2026-10-08 09:00:00'),
('8','8','8','DADUYET','2026-10-09 09:00:00');
INSERT INTO quanlydetaisinhvien_demo.hoidong(Id,Dotdangkyid,Tenhoidong,Ngaybaocao,Phong) VALUES
('101','3','Hội đồng DEMO TLCN 01','2026-10-12 08:00:00','DEMO-A101'),
('102','4','Hội đồng DEMO KLTN 01','2026-10-13 08:00:00','DEMO-B102'),
('103','4','Hội đồng DEMO KLTN 02','2026-10-14 08:00:00','DEMO-B103');
INSERT INTO quanlydetaisinhvien_demo.thanhvienhoidong(Hoidongid,Giangvienid,Vaitro) VALUES
('101','12','CHUTICH'),
('101','13','THUKY'),
('101','14','THANHVIEN'),
('102','15','CHUTICH'),
('102','16','THUKY'),
('102','17','THANHVIEN'),
('103','16','CHUTICH'),
('103','17','THUKY'),
('103','18','THANHVIEN');
INSERT INTO quanlydetaisinhvien_demo.giangvienphanbien(Id,Dangkydetaiid,Giangvienid) VALUES
('1','1','7'),
('2','2','8'),
('3','3','12'),
('4','4','15'),
('5','5','9'),
('6','6','10'),
('7','7','11'),
('8','8','16');
INSERT INTO quanlydetaisinhvien_demo.diem(Id,Dangkydetaiid,Giangvienid,Diemso,Nhanxet) VALUES
('1','1','7','8.2','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('2','2','8','7.6','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('3','3','12','8','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('4','3','13','8.5','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('5','3','14','9','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('6','4','15','7','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('7','4','16','8','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('8','4','17','9','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('9','5','9','8','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('10','7','11','7.5','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('11','8','16','9','Nhận xét DEMO — chỉ phục vụ kiểm thử.'),
('12','8','17','8','Nhận xét DEMO — chỉ phục vụ kiểm thử.');
INSERT INTO quanlydetaisinhvien_demo.ketqua(Id,Dangkydetaiid,Diemcuoi,Dacongbo,Ngaycongbo) VALUES
('1','1','8.2','0',NULL),
('2','2','7.6','0',NULL),
('3','3','8.5','0',NULL),
('4','4','8','0',NULL),
('5','5','8','1','2026-10-10 08:00:00'),
('6','6',NULL,'0',NULL),
('7','7','7.5','0',NULL),
('8','8',NULL,'0',NULL);
INSERT INTO quanlydetaisinhvien_demo.phanconghoidong(Dangkydetaiid,Hinhthuc,Hoidongid) VALUES
('1','KHONGHOIDONG',NULL),
('2','KHONGHOIDONG',NULL),
('3','COHOIDONG','101'),
('4','COHOIDONG','102'),
('5','KHONGHOIDONG',NULL),
('6','KHONGHOIDONG',NULL),
('7','KHONGHOIDONG',NULL),
('8','COHOIDONG','103');
-- END DEMO DATA
    COMMIT;
    SET transaction_started = FALSE;
END$$
DELIMITER ;
