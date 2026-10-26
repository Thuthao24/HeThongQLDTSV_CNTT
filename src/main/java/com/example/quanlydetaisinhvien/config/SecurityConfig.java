```java
package com.example.quanlydetaisinhvien.config;

import com.example.quanlydetaisinhvien.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CustomUserDetailsService userDetailsService
    ) throws Exception {

        http
            .userDetailsService(userDetailsService)

            .authorizeHttpRequests(auth -> auth

                // ==================== PUBLIC ====================
                .requestMatchers(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/login",
                        "/403"
                ).permitAll()

                // ==================== TÀI KHOẢN ====================
                .requestMatchers(
                        HttpMethod.GET,
                        "/taikhoan",
                        "/taikhoan/canhan"
                ).authenticated()

                .requestMatchers("/taikhoan/**").denyAll()

                // ==================== DANH MỤC ====================
                .requestMatchers(
                        "/bomon/**",
                        "/quanly/danhmuc/**"
                ).hasRole("TRUONGKHOA")

                // ==================== GIAI ĐOẠN 1 - ĐỀ TÀI ====================

                // Giảng viên / Trưởng BM / Trưởng khoa đều được mở form đề xuất
                .requestMatchers(
                        HttpMethod.GET,
                        "/giai-doan-1/detai/tao"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                // Giảng viên / Trưởng BM / Trưởng khoa đều được lưu đề xuất
                .requestMatchers(
                        HttpMethod.POST,
                        "/giai-doan-1/detai/luu"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                // Chỉ Trưởng khoa được duyệt / công bố
                .requestMatchers(
                        HttpMethod.POST,
                        "/giai-doan-1/detai/*/duyet",
                        "/giai-doan-1/detai/*/congbo"
                ).hasRole("TRUONGKHOA")

                // Sửa / xóa đề tài:
                // giữ theo nghiệp vụ hiện tại, chưa siết thêm nếu service đã tự kiểm tra chủ sở hữu/quyền
                .requestMatchers(
                        "/giai-doan-1/detai/*/sua",
                        "/giai-doan-1/detai/*/xoa"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                // Các URL giai-doan-1 còn lại
                .requestMatchers("/giai-doan-1/**")
                .hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                // ==================== ĐỢT ĐĂNG KÝ ====================
                .requestMatchers("/dotdangky/**")
                .hasRole("TRUONGKHOA")

                // ==================== GIẢNG VIÊN / SINH VIÊN ====================
                .requestMatchers(
                        "/giangvien/**",
                        "/sinhvien/**"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON"
                )

                // Danh sách đề tài:
                // GIANGVIEN KHÔNG được xem
                .requestMatchers("/detai/**")
                .hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON"
                )

                // ==================== ĐĂNG KÝ ĐỀ TÀI ====================
                .requestMatchers(
                        HttpMethod.POST,
                        "/dangkydetai/*/huy"
                ).hasRole("SINHVIEN")

                .requestMatchers(
                        HttpMethod.POST,
                        "/dangkydetai/*/duyet"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                .requestMatchers(
                        "/dangkydetai",
                        "/dangkydetai/**"
                ).hasAnyRole(
                        "SINHVIEN",
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                // ==================== NHÓM / NỘP BÁO CÁO ====================
                .requestMatchers(
                        "/nhom/**",
                        "/nopbaocao/**"
                ).hasAnyRole(
                        "SINHVIEN",
                        "TRUONGKHOA"
                )

                // ==================== PHÂN CÔNG GVPB ====================

                // Xem trang phân công
                .requestMatchers(
                        HttpMethod.GET,
                        "/phanbien",
                        "/phanbien/"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                // Thêm / sửa / xóa / lưu phân công
                .requestMatchers("/phanbien/**")
                .hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON"
                )

                // ==================== HỘI ĐỒNG / ĐIỂM ====================
                .requestMatchers(
                        HttpMethod.POST,
                        "/hoidong/**"
                ).hasRole("TRUONGKHOA")

                .requestMatchers(
                        HttpMethod.POST,
                        "/diem/**"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                .requestMatchers(
                        "/hoidong/**",
                        "/diem/**"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                // ==================== KẾT QUẢ ====================
                .requestMatchers(
                        "/ketqua/*/congbo",
                        "/ketqua/*/phancong",
                        "/ketqua/email/**"
                ).hasRole("TRUONGKHOA")

                .requestMatchers(
                        HttpMethod.GET,
                        "/ketqua",
                        "/ketqua/**"
                ).hasAnyRole(
                        "SINHVIEN",
                        "TRUONGKHOA"
                )

                .requestMatchers(
                        "/ketqua",
                        "/ketqua/**"
                ).hasRole("TRUONGKHOA")

                // ==================== THÔNG BÁO ====================
                .requestMatchers(
                        "/thongbao/create",
                        "/thongbao/*/edit"
                ).hasRole("TRUONGKHOA")

                .requestMatchers(
                        HttpMethod.POST,
                        "/thongbao/*/dadoc"
                ).hasAnyRole(
                        "SINHVIEN",
                        "GIANGVIEN",
                        "TRUONGBOMON"
                )

                .requestMatchers(
                        HttpMethod.POST,
                        "/thongbao",
                        "/thongbao/**"
                ).hasRole("TRUONGKHOA")

                .requestMatchers(
                        HttpMethod.GET,
                        "/thongbao",
                        "/thongbao/**"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN",
                        "SINHVIEN"
                )

                .requestMatchers(
                        "/thongbao",
                        "/thongbao/**"
                ).denyAll()

                // ==================== MỞ RỘNG ====================
                .requestMatchers(
                        HttpMethod.GET,
                        "/morong/timkiem/detai"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                .requestMatchers(
                        HttpMethod.GET,
                        "/morong/timkiem/**"
                ).authenticated()

                .requestMatchers(
                        HttpMethod.GET,
                        "/morong/thongke"
                ).hasRole("TRUONGKHOA")

                .requestMatchers(
                        HttpMethod.GET,
                        "/morong/xuat/detai.xlsx",
                        "/morong/xuat/detai.pdf"
                ).hasAnyRole(
                        "TRUONGKHOA",
                        "TRUONGBOMON",
                        "GIANGVIEN"
                )

                .requestMatchers(
                        HttpMethod.GET,
                        "/morong/xuat/thongke.xlsx",
                        "/morong/xuat/thongke.pdf"
                ).hasRole("TRUONGKHOA")

                .requestMatchers(
                        HttpMethod.GET,
                        "/morong/xuat/**"
                ).authenticated()

                .requestMatchers(
                        "/morong",
                        "/morong/**"
                ).denyAll()

                // ==================== HOME ====================
                .requestMatchers("/")
                .authenticated()

                .anyRequest()
                .authenticated()
            )

            .exceptionHandling(ex ->
                ex.accessDeniedPage("/403")
            )

            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
```
