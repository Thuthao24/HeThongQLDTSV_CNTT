package com.example.quanlydetaisinhvien.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationEmailSettings.class)
public class NotificationEmailConfig {}
