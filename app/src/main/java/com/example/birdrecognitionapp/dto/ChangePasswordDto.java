package com.example.birdrecognitionapp.dto;

import com.example.birdrecognitionapp.utils.PasswordUtils;

public class ChangePasswordDto {
    String email;
    String newPassword;

    public ChangePasswordDto(String email, String newPassword) {
        this.email = email;
        this.newPassword = PasswordUtils.hashPassword(newPassword);
    }

    public ChangePasswordDto() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
