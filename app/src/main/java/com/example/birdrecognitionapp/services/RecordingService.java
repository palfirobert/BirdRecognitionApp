package com.example.birdrecognitionapp.services;



import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.birdrecognitionapp.activities.MainActivity;

import java.io.File;
import java.io.IOException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordingService extends Service {

    MediaRecorder mediaRecorder;
    long startingTimeMillis=0;
    long elapsedTimeMillis=0;

    File file;
    String filename;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("SA INTRAT");
        startRecording();

        return START_STICKY;
    }

    private void startRecording() {

            Long timeStampLong = System.currentTimeMillis() / 1000;
            String ts = timeStampLong.toString();
            file = new File(Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/");
            file.mkdirs();
            System.out.println(file.exists());
            System.out.println(file.getPath());
            filename = file.getPath();
            filename += "/audio" + ts + ".mp3";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(filename);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioEncodingBitRate(128);
            mediaRecorder.setAudioSamplingRate(44100);
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                startingTimeMillis = System.currentTimeMillis();

            } catch (IOException e) {
                e.printStackTrace();

            }

    }

    @Override
    public void onDestroy() {
        if (mediaRecorder!=null)
        {
            stopRecording();
        }
        System.out.println("S-A DAT DESTROY");
        super.onDestroy();
    }
    private void stopRecording()
    {
        mediaRecorder.stop();
        elapsedTimeMillis=(System.currentTimeMillis()-startingTimeMillis);
        mediaRecorder.release();
        mediaRecorder=null;
        Toast.makeText(getApplicationContext(), "Recording saved "+file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        // maybe add to database
    }


}
