package com.example.birdrecognitionapp.dto;

public class GetUserSoundsDto {

    private String user_id;

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public GetUserSoundsDto(String user_id) {
        this.user_id = user_id;
    }

    public GetUserSoundsDto() {
    }
}
