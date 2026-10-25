package com.example.quanlydetaisinhvien.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.ketqua.email.scheduling-enabled", havingValue = "true")
public class KetquaEmailSchedulingConfig {}
