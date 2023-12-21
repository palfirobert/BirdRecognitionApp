package com.example.birdrecognitionapp.services;


import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.birdrecognitionapp.activities.MainActivity;
import com.example.birdrecognitionapp.api.RetrofitAPI;
import com.example.birdrecognitionapp.dto.SoundPredictionResponse;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordingService extends Service {

    MediaRecorder mediaRecorder;
    long startingTimeMillis = 0;
    long elapsedTimeMillis = 0;
    private static final int MAX_RETRIES = 3; // Max number of retries
    private int retryCount = 0; // Current retry count
    String ts;
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
        ts = timeStampLong.toString();
        file = new File(Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/");
        file.mkdirs();
        //System.out.println(file.exists());
        //System.out.println(file.getPath());
        filename = file.getPath();
        filename += "/audio" + ts + ".mp3";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(filename);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioChannels(1);
        mediaRecorder.setAudioEncodingBitRate(256);
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
        if (mediaRecorder != null) {
            stopRecording();
        }
        System.out.println("S-A DAT DESTROY");
        String mp3FilePath = Environment.getExternalStorageDirectory().getPath() + "/soundrecordings" + "/audio" + ts + ".mp3";
        Path path = Paths.get(mp3FilePath);
        try {
            byte[] fileContent = Files.readAllBytes(path);
            System.out.println(Base64.getEncoder().encodeToString(fileContent));

            // flag the recording activity that the recording stopped and to show la loading dialog
            Intent intent=new Intent("RECORDING_STOPPED");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            postData(Base64.getEncoder().encodeToString(fileContent));
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void stopRecording() {
        mediaRecorder.stop();
        elapsedTimeMillis = (System.currentTimeMillis() - startingTimeMillis);
        mediaRecorder.release();
        mediaRecorder = null;
        Toast.makeText(getApplicationContext(), "Recording saved " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        // maybe add to database
    }


    private void postData(String soundInBase64) {
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        // Create an OkHttpClient with the desired timeout
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        // Create a Retrofit instance with the custom OkHttpClient
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // sau http://10.0.2.2:8000/  sau palfirobert.pythonanywhere.com
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)  // Set the custom OkHttpClient
                .build();

        // Create a RetrofitAPI instance
        RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

        // Create a Call object for the API request
        Call<List<SoundPredictionResponse>> call = retrofitAPI.createPost(soundInBase64);

        // Enqueue the API call
        call.enqueue(new Callback<List<SoundPredictionResponse>>() {
            @Override
            public void onResponse(Call<List<SoundPredictionResponse>> call, Response<List<SoundPredictionResponse>> response) {
                // Handle the successful response
                retryCount = 0; // Reset retry count
                Toast.makeText(getApplicationContext(), "Data added to API", Toast.LENGTH_SHORT).show();
                List<SoundPredictionResponse> responseFromAPI = response.body();
                assert responseFromAPI != null;
                Collections.sort(responseFromAPI);
                for (SoundPredictionResponse sound : responseFromAPI) {
                    System.out.println(sound.toString());
                }
                // Broadcast the list to the fragment
                sendBroadcast(responseFromAPI);
            }

            @Override
            public void onFailure(Call<List<SoundPredictionResponse>> call, Throwable t) {
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    System.out.println("Retrying... Attempt: " + retryCount);
                    postData(soundInBase64); // Retry the call
                } else {
                    // Handle the failure after exceeding retry count
                    t.printStackTrace();
                    retryCount = 0; // Reset retry count
                }
            }
        });
    }


    private void sendBroadcast(List<SoundPredictionResponse> responseList) {
        Intent intent = new Intent("send-predictions-to-record-fragment");
        ArrayList<Parcelable> parcelableList = new ArrayList(responseList);
        intent.putParcelableArrayListExtra("predictionList", parcelableList);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
