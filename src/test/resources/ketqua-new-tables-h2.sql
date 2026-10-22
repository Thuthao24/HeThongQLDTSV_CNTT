-- Test-only FKs: the application's existing entities store raw IDs.
ALTER TABLE phanconghoidong ADD CONSTRAINT Fk_pchd_dangky
    FOREIGN KEY (Dangkydetaiid) REFERENCES dangkydetai(Id) ON DELETE CASCADE;
ALTER TABLE phanconghoidong ADD CONSTRAINT Fk_pchd_hoidong
    FOREIGN KEY (Hoidongid) REFERENCES hoidong(Id) ON DELETE RESTRICT;
ALTER TABLE emailketqua ADD CONSTRAINT Fk_emailketqua_result
    FOREIGN KEY (Ketquaid) REFERENCES ketqua(Id) ON DELETE RESTRICT;
ALTER TABLE emailketqua ADD CONSTRAINT Fk_emailketqua_student
    FOREIGN KEY (Sinhvienid) REFERENCES sinhvien(Id) ON DELETE RESTRICT;
