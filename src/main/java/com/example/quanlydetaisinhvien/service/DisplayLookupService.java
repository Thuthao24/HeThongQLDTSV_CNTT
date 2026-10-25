
package com.example.quanlydetaisinhvien.service;

import com.example.quanlydetaisinhvien.entity.*;
import com.example.quanlydetaisinhvien.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    // =========================================
    // 1. LỌC ID HỢP LỆ
    // =========================================

    private List<Long> ids(Collection<Long> input) {

        if (input == null || input.isEmpty()) {
            return List.of();
        }

        return input.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // =========================================
    // 2. THÔNG TIN TÀI KHOẢN ĐỂ HIỂN THỊ
    // =========================================

    /*
     * Chỉ chứa thông tin được phép hiển thị.
     * Không lấy mật khẩu hoặc mã băm mật khẩu.
     */
    public static final class AccountSummary {

        private final String hoten;
        private final String tendangnhap;
        private final String email;
        private final String vaitro;
        private final boolean kichhoat;

        public AccountSummary(Taikhoan account) {

            this.hoten = account.getHoten();

            this.tendangnhap =
                    account.getTendangnhap();

            this.email = account.getEmail();

            this.kichhoat = Boolean.TRUE.equals(
                    account.getKichhoat()
            );

            // Chuyển vai trò thành tiếng Việt
            this.vaitro =
                    account.getVaitro() == null
                            ? "Chưa xác định"
                            : switch (account.getVaitro()) {

                                case TRUONGKHOA ->
                                        "Trưởng khoa";

                                case TRUONGBOMON ->
                                        "Trưởng bộ môn";

                                case GIANGVIEN ->
                                        "Giảng viên";

                                case SINHVIEN ->
                                        "Sinh viên";
                            };
        }

        public String getHoten() {
            return hoten;
        }

        public String getTendangnhap() {
            return tendangnhap;
        }

        public String getEmail() {
            return email;
        }

        public String getVaitro() {
            return vaitro;
        }

        public boolean isKichhoat() {
            return kichhoat;
        }

        public boolean getKichhoat() {
            return kichhoat;
        }
    }

    // =========================================
    // 3. LẤY THÔNG TIN CÁC TÀI KHOẢN
    // =========================================

    public Map<Long, AccountSummary> accountProfiles(
            Collection<Long> input) {

        List<Long> keys = ids(input);

        if (keys.isEmpty()) {
            return Map.of();
        }

        return accounts.findAllById(keys)
                .stream()
                .collect(Collectors.toMap(
                        Taikhoan::getId,
                        AccountSummary::new
                ));
    }

    // =========================================
    // 4. LẤY HỌ TÊN THEO ID TÀI KHOẢN
    // =========================================

    public Map<Long, String> accountNames(
            Collection<Long> input) {

        List<Long> keys = ids(input);

        if (keys.isEmpty()) {
            return Map.of();
        }

        return accounts.findAllById(keys)
                .stream()
                .collect(Collectors.toMap(
                        Taikhoan::getId,
                        account -> Objects.toString(
                                account.getHoten(),
                                "Chưa cập nhật"
                        )
                ));
    }

    // =========================================
    // 5. LẤY NHÃN TÀI KHOẢN
    // =========================================

    public Map<Long, String> accountLabels(
            Collection<Long> input) {

        List<Long> keys = ids(input);

        if (keys.isEmpty()) {
            return Map.of();
        }

        return accounts.findAllById(keys)
                .stream()
                .collect(Collectors.toMap(
                        Taikhoan::getId,
                        account ->
                                Objects.toString(
                                        account.getHoten(),
                                        "Chưa cập nhật"
                                )
                                + " ("
                                + Objects.toString(
                                        account.getTendangnhap(),
                                        "—"
                                )
                                + ")"
                ));
    }

    // =========================================
    // 6. LẤY TÊN BỘ MÔN
    // =========================================

    public Map<Long, String> departmentNames(
            Collection<Long> input) {

        List<Long> keys = ids(input);

        if (keys.isEmpty()) {
            return Map.of();
        }

        return departments.findAllById(keys)
                .stream()
                .collect(Collectors.toMap(
                        Bomon::getId,
                        department -> Objects.toString(
                                department.getTenbomon(),
                                "Chưa xác định"
                        )
                ));
    }

    // =========================================
    // 7. LẤY TÊN GIẢNG VIÊN ĐỀ XUẤT
    // =========================================

    public Map<Long, String> lecturerNames(
            Collection<Long> input) {

        List<Long> keys = ids(input);

        if (keys.isEmpty()) {
            return Map.of();
        }

        List<Giangvien> teacherList =
                teachers.findAllById(keys);

        Map<Long, String> names = accountNames(
                teacherList.stream()
                        .map(Giangvien::getTaikhoanid)
                        .toList()
        );

        Map<Long, String> result = new HashMap<>();

        for (Giangvien teacher : teacherList) {

            String name = names.getOrDefault(
                    teacher.getTaikhoanid(),
                    teacher.getMagiangvien()
            );

            result.put(
                    teacher.getId(),
                    Objects.toString(
                            name,
                            "Chưa xác định"
                    )
            );
        }

        return result;
    }

    // =========================================
    // 8. LẤY TÊN ĐỢT ĐĂNG KÝ
    // =========================================

    public Map<Long, String> roundNames(
            Collection<Long> input) {

        List<Long> keys = ids(input);

        if (keys.isEmpty()) {
            return Map.of();
        }

        return rounds.findAllById(keys)
                .stream()
                .collect(Collectors.toMap(
                        Dotdangky::getId,
                        round -> Objects.toString(
                                round.getTendot(),
                                "Chưa xác định"
                        )
                ));
    }
}
