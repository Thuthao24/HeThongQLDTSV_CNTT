package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DisplayLookupService {

    private final TaikhoanRepository accounts;
    private final BomonRepository departments;
    private final GiangvienRepository teachers;
    private final DotdangkyRepository rounds;

    public DisplayLookupService(
            TaikhoanRepository accounts,
            BomonRepository departments,
            GiangvienRepository teachers,
            DotdangkyRepository rounds) {
        this.accounts = accounts;
        this.departments = departments;
        this.teachers = teachers;
        this.rounds = rounds;
    }

    private List<Long> ids(Collection<Long> input) {
        if (input == null) return List.of();
        return input.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public Map<Long, String> accountNames(Collection<Long> input) {
        var keys = ids(input);
        if (keys.isEmpty()) return Map.of();

        return accounts.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        Taikhoan::getId,
                        Taikhoan::getHoten
                ));
    }

    public Map<Long, String> accountLabels(Collection<Long> input) {
        var keys = ids(input);
        if (keys.isEmpty()) return Map.of();

        return accounts.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        Taikhoan::getId,
                        a -> a.getHoten() + " (" + a.getTendangnhap() + ")"
                ));
    }

    public Map<Long, String> departmentNames(Collection<Long> input) {
        var keys = ids(input);
        if (keys.isEmpty()) return Map.of();

        return departments.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        Bomon::getId,
                        Bomon::getTenbomon
                ));
    }

    public Map<Long, String> lecturerNames(Collection<Long> input) {
        var keys = ids(input);
        if (keys.isEmpty()) return Map.of();

        List<Giangvien> list = teachers.findAllById(keys);

        Map<Long, String> names = accountNames(
                list.stream()
                        .map(Giangvien::getTaikhoanid)
                        .toList()
        );

        Map<Long, String> result = new HashMap<>();

        for (Giangvien gv : list) {
            result.put(
                    gv.getId(),
                    names.getOrDefault(
                            gv.getTaikhoanid(),
                            gv.getMagiangvien()
                    )
            );
        }

        return result;
    }

    public Map<Long, String> roundNames(Collection<Long> input) {
        var keys = ids(input);
        if (keys.isEmpty()) return Map.of();

        return rounds.findAllById(keys).stream()
                .collect(Collectors.toMap(
                        Dotdangky::getId,
                        Dotdangky::getTendot
                ));
    }
}