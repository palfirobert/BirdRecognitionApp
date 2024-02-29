package com.example.birdrecognitionapp.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * class to send the recording from main activity to fragment 1
 */
public class MainActivityRecordFragmentSharedModel extends ViewModel {
    private final MutableLiveData<RecordingItem> recordingItemToPredict = new MutableLiveData<>();

    public void predictRecording(RecordingItem item) {
        recordingItemToPredict.setValue(item);
    }

    public LiveData<RecordingItem> getRecordingItemToPredict() {
        return recordingItemToPredict;
    }
}
