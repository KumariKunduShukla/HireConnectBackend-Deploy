package com.hireconnect.auth.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfig {
    // Spring Boot auto-configures JavaMailSender from application.yml properties.
    // Do NOT manually create a JavaMailSenderImpl bean here — it overrides
    // auto-configuration and produces an empty, unconfigured mail sender.
}