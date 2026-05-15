package com.hireconnect.auth.entity;

import jakarta.validation.constraints.NotBlank;

public class TokenRequest {
    @NotBlank(message = "Token is missing")
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}