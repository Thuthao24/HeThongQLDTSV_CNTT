package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.dto.ThongbaoViewDTO;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

/** Revision covers title, body and the canonical recipient set, including legacy effective roles. */
public final class ThongbaoRevision {
    private ThongbaoRevision() {}
    public static String of(ThongbaoViewDTO notice) { return digest(notice.tieude(),notice.noidung(),notice.recipientRoles()); }
    public static String digest(String title,String body) {
        return digest(title,body,com.example.quanlydetaisinhvien.entity.DoituongThongbao.all());
    }
    public static String digest(String title,String body,java.util.Set<com.example.quanlydetaisinhvien.entity.DoituongThongbao> roles) {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            byte[] titleBytes=title.getBytes(StandardCharsets.UTF_8);
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(titleBytes.length).array());
            digest.update(titleBytes);
            byte[] bodyBytes=body.getBytes(StandardCharsets.UTF_8);
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(bodyBytes.length).array());
            digest.update(bodyBytes);
            String audience=roles.stream().sorted().map(Enum::name).collect(java.util.stream.Collectors.joining(","));
            return HexFormat.of().formatHex(digest.digest(audience.getBytes(StandardCharsets.UTF_8)));
        } catch(NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
