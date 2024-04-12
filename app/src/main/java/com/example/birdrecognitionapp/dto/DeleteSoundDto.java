package com.example.birdrecognitionapp.dto;

public class DeleteSoundDto {
    private String user_id;
    private String blob_reference;
    private String file_name;

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

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public DeleteSoundDto(String user_id, String blob_reference, String file_name) {
        this.user_id = user_id;
        this.blob_reference = blob_reference;
        this.file_name = file_name;
    }
}
