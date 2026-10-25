package com.example.quanlydetaisinhvien.security;

import com.example.quanlydetaisinhvien.entity.Taikhoan;
import com.example.quanlydetaisinhvien.repository.TaikhoanRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final TaikhoanRepository taikhoanRepository;

    public CustomUserDetailsService(TaikhoanRepository taikhoanRepository) {
        this.taikhoanRepository = taikhoanRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Taikhoan tk = taikhoanRepository.findByTendangnhap(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay tai khoan"));

        return User.withUsername(tk.getTendangnhap())
                .password(tk.getMatkhau())
                .roles(tk.getVaitro().name())
                .disabled(Boolean.FALSE.equals(tk.getKichhoat()))
                .build();
    }
}
