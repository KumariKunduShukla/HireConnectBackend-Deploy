package com.hireconnect.subscription.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        String id = keyId == null ? "" : keyId.trim();
        String secret = keySecret == null ? "" : keySecret.trim();
        if (id.isEmpty() || secret.isEmpty()) {
            throw new IllegalStateException(
                    "Razorpay is not configured. Set razorpay.key.id and razorpay.key.secret in application-local.yml "
                            + "or environment variables RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        return new RazorpayClient(id, secret);
    }
}