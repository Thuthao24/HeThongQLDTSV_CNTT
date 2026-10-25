package com.example.quanlydetaisinhvien.dto;

import java.time.LocalDateTime;

/** Public notice data; never exposes the account entity or credentials. */
public record ThongbaoViewDTO(Long id, String tieude, String noidung,
        String nguoidangHoten, LocalDateTime ngaydang,
        java.util.Set<com.example.quanlydetaisinhvien.entity.DoituongThongbao> recipientRoles) {
    public ThongbaoViewDTO { recipientRoles=java.util.Set.copyOf(recipientRoles); }
    public ThongbaoViewDTO(Long id,String tieude,String noidung,String author,LocalDateTime date) {
        this(id,tieude,noidung,author,date,com.example.quanlydetaisinhvien.entity.DoituongThongbao.all());
    }
    public String getRecipientLabels() {
        return recipientRoles.stream().sorted().map(com.example.quanlydetaisinhvien.entity.DoituongThongbao::getLabel)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
