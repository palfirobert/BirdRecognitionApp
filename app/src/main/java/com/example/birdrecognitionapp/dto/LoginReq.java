package com.example.birdrecognitionapp.dto;

import java.util.Objects;

public class LoginReq {
    private String email;
    private String password;
    public LoginReq(){}

    public LoginReq(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginReq loginReq = (LoginReq) o;
        return Objects.equals(email, loginReq.email) && Objects.equals(password, loginReq.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, password);
    }
}
