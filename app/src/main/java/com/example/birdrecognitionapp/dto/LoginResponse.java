package com.example.birdrecognitionapp.dto;

public class LoginResponse {
    private String message;
    private String error;

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
}

