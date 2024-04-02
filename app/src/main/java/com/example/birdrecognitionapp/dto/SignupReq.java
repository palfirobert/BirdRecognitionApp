package com.example.birdrecognitionapp.dto;

import com.example.birdrecognitionapp.utils.PasswordUtils;

import java.util.UUID;

public class SignupReq {
    private String id;
    private String name;
    private String surname;
    private String email;
    private String password;

    public SignupReq() {
    }

    public SignupReq(String name, String surname, String email, String password) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = PasswordUtils.hashPassword(password);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
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
}
