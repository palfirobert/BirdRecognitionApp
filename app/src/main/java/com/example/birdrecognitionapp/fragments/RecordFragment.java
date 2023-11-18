package com.example.birdrecognitionapp.fragments;


import android.content.Intent;
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


import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.activities.MainActivity;
import com.example.birdrecognitionapp.services.RecordingService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.sql.SQLOutput;

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

    private boolean startRecording = true;
    private boolean pauseRecording = true;
    long timeWhenPaused = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);
        ButterKnife.bind(this, recordView);
        return recordView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnPause.setVisibility(View.INVISIBLE);
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
