package com.example.birdrecognitionapp.dto;

public class ObservationSheetDto {
    String observationDate;
    String species;
    Integer number;
    String observer;
    String uploadDate;
    String location;
    String userId;
    Integer soundId;

    public ObservationSheetDto(String observationDate, String species, Integer number, String observer, String uploadDate, String location, String userId, Integer soundId) {
        this.observationDate = observationDate;
        this.species = species;
        this.number = number;
        this.observer = observer;
        this.uploadDate = uploadDate;
        this.location = location;
        this.userId = userId;
        this.soundId = soundId;
    }

    public String getObservationDate() {
        return observationDate;
    }

    public void setObservationDate(String observationDate) {
        this.observationDate = observationDate;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getObserver() {
        return observer;
    }

    public void setObserver(String observer) {
        this.observer = observer;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getSoundId() {
        return soundId;
    }

    public void setSoundId(Integer soundId) {
        this.soundId = soundId;
    }
}
