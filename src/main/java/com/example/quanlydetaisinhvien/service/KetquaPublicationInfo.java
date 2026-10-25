package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.Hoidong;
import com.example.quanlydetaisinhvien.entity.Phanconghoidong;
import java.util.List;

public record KetquaPublicationInfo(String reason, boolean canAssign, boolean allowNoCouncil,
        Phanconghoidong assignment, List<Hoidong> councils) {}
