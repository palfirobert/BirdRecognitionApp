package com.example.birdrecognitionapp.interfaces;

import com.example.birdrecognitionapp.models.RecordingItem;

public interface OnDatabaseChangedListener {
    void onNewDatabaseEntryAdded(RecordingItem recordingItem);
}
