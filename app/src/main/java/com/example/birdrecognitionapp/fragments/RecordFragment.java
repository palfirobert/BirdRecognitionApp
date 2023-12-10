package com.example.birdrecognitionapp.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.os.Parcelable;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.dto.SoundPredictionResponse;
import com.example.birdrecognitionapp.services.RecordingService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RecordFragment extends Fragment {

    @BindView(R.id.chronometer)
    Chronometer chronometer;

    @BindView(R.id.recording_status_txt)
    TextView recordingStatusText;

    @BindView(R.id.btn_record)
    FloatingActionButton recordButton;

    @BindView(R.id.btn_pause)
    Button btnPause;

    @BindView(R.id.button_1)
    Button predictionButtonOne;

    @BindView(R.id.button_2)
    Button predictionButtonTwo;

    @BindView(R.id.button_3)
    Button predictionButtonThree;

    @BindView(R.id.prediction_title)
    TextView predictionTitle;

    private boolean startRecording = true;
    private boolean pauseRecording = true;
    long timeWhenPaused = 0;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle the received data
            List<?> responseList = intent.getParcelableArrayListExtra("predictionList");
            List<SoundPredictionResponse>predictionList=new ArrayList(responseList);
            updateUI(predictionList);
        }
    };

    private void updateUI(List<SoundPredictionResponse> predictionList) {
        // Create a Map with common_name as key and CoiSoundPredictionResponse as value
        Map<String, SoundPredictionResponse> map = new HashMap<>();
        for (SoundPredictionResponse response : predictionList) {
            map.put(response.getCommon_name(), response);
        }

        List<SoundPredictionResponse>distinctPredictionList=new ArrayList(map.values());
        Collections.sort(distinctPredictionList,Collections.reverseOrder());
        // Print the map
        System.out.println(map);
        System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        System.out.println(distinctPredictionList);

        for (int i = 0; i < distinctPredictionList.size(); i++) {
            switch (i) {
                case 0:
                    predictionButtonOne.setVisibility(View.VISIBLE);
                    predictionButtonOne.setText(String.valueOf(distinctPredictionList.get(i).getCommon_name()));
                    break;
                case 1:
                    predictionButtonTwo.setVisibility(View.VISIBLE);
                    predictionButtonTwo.setText(String.valueOf(distinctPredictionList.get(i).getCommon_name()));
                    break;
                case 2:
                    predictionButtonThree.setVisibility(View.VISIBLE);
                    predictionButtonThree.setText(String.valueOf(distinctPredictionList.get(i).getCommon_name()));
                    break;
                // Handle additional cases if needed
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);
        ButterKnife.bind(this, recordView);
        this.predictionButtonOne.setVisibility(View.INVISIBLE);
        this.predictionButtonTwo.setVisibility(View.INVISIBLE);
        this.predictionButtonThree.setVisibility(View.INVISIBLE);
        this.predictionTitle.setVisibility(View.INVISIBLE);
        return recordView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnPause.setVisibility(View.INVISIBLE);
        // Register to receive broadcasts from the service
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver,
                new IntentFilter("send-predictions-to-record-fragment"));
    }

    @OnClick(R.id.btn_record)
    public void recordAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onRecord(startRecording);
                startRecording = !startRecording;
            } else { //request for the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

    }

    private void onRecord(boolean startRecording) {
        Intent intent = new Intent(getContext(), RecordingService.class);

        if (startRecording) {
            this.predictionButtonOne.setVisibility(View.INVISIBLE);
            this.predictionButtonTwo.setVisibility(View.INVISIBLE);
            this.predictionButtonThree.setVisibility(View.INVISIBLE);
            this.predictionTitle.setVisibility(View.INVISIBLE);

            recordButton.setImageResource(R.drawable.ic_media_stop);
            Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();

            getActivity().startService(intent);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            recordingStatusText.setText("Recording...");

        } else {
            recordButton.setImageResource(R.drawable.ic_mic_white);
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            timeWhenPaused = 0;
            recordingStatusText.setText("Tap the button to start recording");
            getActivity().stopService(intent);

        }
    }
}
