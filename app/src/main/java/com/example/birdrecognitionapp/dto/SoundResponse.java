package com.example.birdrecognitionapp.dto;

import com.example.birdrecognitionapp.models.RecordingItem;

import java.util.List;

public class SoundResponse {
    private List<RecordingItem> sounds;

    // Getter
    public List<RecordingItem> getSounds() {
        return sounds;
    }

    // Setter
    public void setSounds(List<RecordingItem> sounds) {
        this.sounds = sounds;
    }
}
