package com.example.birdrecognitionapp.dto;

public class LoginResponse {
    private String message;
    private String error;
    private String user_id;
    private String language;
    private Integer use_location;
    private String surname;
    private String email;
    private String name;
    private String password;
    private String token;
    public LoginResponse(){}

    public LoginResponse(String message, String error, String user_id, String language, Integer use_location, String surname, String email, String name, String password) {
        this.message = message;
        this.error = error;
        this.user_id = user_id;
        this.language = language;
        this.use_location = use_location;
        this.surname = surname;
        this.email = email;
        this.name = name;
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoginResponse(String message, String error, String user_id, String language, Integer use_location, String surname, String email, String name) {
        this.message = message;
        this.error = error;
        this.user_id = user_id;
        this.language = language;
        this.use_location = use_location;
        this.surname = surname;
        this.email = email;
        this.name = name;
    }

    public LoginResponse(String message, String user_id, String language, Integer use_location) {
        this.message = message;
        this.user_id = user_id;
        this.language = language;
        this.use_location = use_location;
    }

    public LoginResponse(String message, String error, String user_id, String language, Integer use_location) {
        this.message = message;
        this.error = error;
        this.user_id = user_id;
        this.language = language;
        this.use_location = use_location;
    }

    // Getter and setter for message
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Getter and setter for error
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getUse_location() {
        return use_location;
    }

    public void setUse_location(Integer use_location) {
        this.use_location = use_location;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "message='" + message + '\'' +
                ", error='" + error + '\'' +
                ", user_id='" + user_id + '\'' +
                ", language='" + language + '\'' +
                ", use_location=" + use_location +
                ", surname='" + surname + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

