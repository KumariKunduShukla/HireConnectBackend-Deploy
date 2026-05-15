package com.hireconnect.auth.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class OtpUtil {
    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}