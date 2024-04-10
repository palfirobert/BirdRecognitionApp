package com.example.birdrecognitionapp.dto;

public class DeleteSoundDto {
    private String user_id;
    private String blob_reference;

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getBlob_reference() {
        return blob_reference;
    }

    public void setBlob_reference(String blob_reference) {
        this.blob_reference = blob_reference;
    }

    public DeleteSoundDto(String user_id, String blob_reference) {
        this.user_id = user_id;
        this.blob_reference = blob_reference;
    }
}
