package com.example.birdrecognitionapp.dto;

public class SecurityCodeDto {
    String email;
    Integer securityCode;

    public SecurityCodeDto(String email, Integer securityCode) {
        this.email = email;
        this.securityCode = securityCode;
    }

    public SecurityCodeDto() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(Integer securityCode) {
        this.securityCode = securityCode;
    }
}
