package com.example.quanlydetaisinhvien.dto;

import java.time.LocalDateTime;

/** Public notice data; never exposes the account entity or credentials. */
public record ThongbaoViewDTO(Long id, String tieude, String noidung,
        String nguoidangHoten, LocalDateTime ngaydang) {}
