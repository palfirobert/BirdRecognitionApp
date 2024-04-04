package com.example.birdrecognitionapp.dto;

public class UserDetailsDto {
    private String user_id;
    private String language;
    private Boolean use_location;

    public UserDetailsDto(){}

    public UserDetailsDto(String user_id, String language, Boolean use_location) {
        this.user_id = user_id;
        this.language = language;
        this.use_location = use_location;
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

    public Boolean getUse_location() {
        return use_location;
    }

    public void setUse_location(Boolean use_location) {
        this.use_location = use_location;
    }

    @Override
    public String toString() {
        return "UserDetails{" +
                "user_id='" + user_id + '\'' +
                ", language='" + language + '\'' +
                ", use_location=" + use_location +
                '}';
    }
}
