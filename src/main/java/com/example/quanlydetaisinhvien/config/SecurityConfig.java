package com.example.quanlydetaisinhvien.config;

import com.example.quanlydetaisinhvien.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            CustomUserDetailsService userDetailsService) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/login", "/403").permitAll()
                .requestMatchers("/taikhoan/**", "/bomon/**", "/quanly/danhmuc/**").hasAnyRole("TRUONGKHOA")
                .requestMatchers("/giai-doan-1/**").hasAnyRole("TRUONGKHOA","TRUONGBOMON","GIANGVIEN")
                .requestMatchers("/dotdangky/**").hasAnyRole("TRUONGKHOA")
                .requestMatchers("/giangvien/**", "/sinhvien/**").hasAnyRole("TRUONGKHOA","TRUONGBOMON")
                .requestMatchers("/detai/**").hasAnyRole("TRUONGKHOA","TRUONGBOMON","GIANGVIEN")
                // Duyệt đăng ký là thao tác quản trị; kiểm tra trước rule bao quát.
                .requestMatchers(HttpMethod.POST, "/dangkydetai/*/duyet").hasRole("TRUONGKHOA")
                .requestMatchers("/nhom/**", "/dangkydetai/**", "/nopbaocao/**").hasAnyRole("SINHVIEN","TRUONGKHOA")
                .requestMatchers(HttpMethod.POST, "/phanbien/**").hasAnyRole("TRUONGKHOA", "TRUONGBOMON")
                .requestMatchers("/phanbien/**").hasAnyRole("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN")
                .requestMatchers(HttpMethod.POST, "/hoidong/**").hasRole("TRUONGKHOA")
                .requestMatchers(HttpMethod.POST, "/diem/**").hasAnyRole("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN")
                .requestMatchers("/hoidong/**", "/diem/**").hasAnyRole("TRUONGKHOA","TRUONGBOMON","GIANGVIEN")
                .requestMatchers("/ketqua/*/congbo", "/ketqua/*/phancong", "/ketqua/email/**").hasRole("TRUONGKHOA")
                .requestMatchers(HttpMethod.GET, "/ketqua", "/ketqua/**").hasAnyRole("SINHVIEN", "TRUONGKHOA")
                .requestMatchers("/ketqua", "/ketqua/**").hasRole("TRUONGKHOA")
                .requestMatchers("/thongbao/create", "/thongbao/*/edit").hasRole("TRUONGKHOA")
                .requestMatchers(HttpMethod.POST, "/thongbao/*/dadoc")
                    .hasAnyRole("SINHVIEN", "GIANGVIEN", "TRUONGBOMON")
                .requestMatchers(HttpMethod.POST, "/thongbao", "/thongbao/**").hasRole("TRUONGKHOA")
                .requestMatchers(HttpMethod.GET, "/thongbao", "/thongbao/**")
                    .hasAnyRole("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN", "SINHVIEN")
                .requestMatchers("/thongbao", "/thongbao/**").denyAll()
                .requestMatchers(HttpMethod.GET, "/morong/timkiem/detai")
                    .hasAnyRole("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN")
                .requestMatchers(HttpMethod.GET, "/morong/timkiem/dotdangky").hasRole("TRUONGKHOA")
                .requestMatchers(HttpMethod.GET, "/morong/timkiem/ketqua").hasAnyRole("TRUONGKHOA", "SINHVIEN")
                .requestMatchers(HttpMethod.GET, "/morong/thongke").hasRole("TRUONGKHOA")
                .requestMatchers(HttpMethod.GET, "/morong/xuat/detai.xlsx", "/morong/xuat/detai.pdf")
                    .hasAnyRole("TRUONGKHOA", "TRUONGBOMON", "GIANGVIEN")
                .requestMatchers(HttpMethod.GET, "/morong/xuat/dotdangky.xlsx", "/morong/xuat/dotdangky.pdf",
                        "/morong/xuat/thongke.xlsx", "/morong/xuat/thongke.pdf").hasRole("TRUONGKHOA")
                .requestMatchers(HttpMethod.GET, "/morong/xuat/ketqua.xlsx", "/morong/xuat/ketqua.pdf")
                    .hasAnyRole("TRUONGKHOA", "SINHVIEN")
                .requestMatchers(HttpMethod.GET, "/morong/xuat/**").authenticated()
                .requestMatchers("/morong", "/morong/**").denyAll()
                .requestMatchers("/").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
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
